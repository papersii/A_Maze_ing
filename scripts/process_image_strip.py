#!/usr/bin/env python3
"""
Image Strip Processing Pipeline
图片条处理流水线

Processes a single image containing 4 animation frames arranged horizontally.
Supports guide line detection for precise alignment.

Usage:
    # Basic usage
    python3 scripts/process_image_strip.py \
        --input raw_assets/images/boar_walk_down.png \
        --frames 4 \
        --name mob_boar_walk_down
    
    # With guide line detection
    python3 scripts/process_image_strip.py \
        --input raw_assets/images/boar_walk_down.png \
        --frames 4 \
        --guide-color "#FF00FF" \
        --name mob_boar_walk_down
    
    # Custom output size
    python3 scripts/process_image_strip.py \
        --input raw_assets/images/boar_walk_down.png \
        --frames 4 \
        --output-size 64 \
        --name mob_boar_walk_down
"""

import os
import sys
import argparse
import re
from PIL import Image
import numpy as np
from pathlib import Path

# Configuration
DEFAULT_FRAME_SIZE = 64
OUTPUT_DIR = "raw_assets/ai_ready_optimized"
FINAL_ANIM_DIR = "assets/images/animations"
FINAL_MOB_DIR = "assets/images/mobs"

# Default guide line color: Magenta
DEFAULT_GUIDE_COLOR = "#FF00FF"
GUIDE_COLOR_TOLERANCE = 60  # Increased from 30 for better detection

# Background removal settings - lower = more aggressive
BG_THRESHOLD = 180  # Was 200, lowered to catch gray backgrounds


def ensure_dirs():
    """Create necessary directories if they don't exist."""
    for d in [OUTPUT_DIR, FINAL_ANIM_DIR, FINAL_MOB_DIR, "raw_assets/images"]:
        os.makedirs(d, exist_ok=True)


def hex_to_rgb(hex_color):
    """Convert hex color to RGB tuple."""
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))


def is_guide_color(r, g, b, guide_rgb, tolerance=GUIDE_COLOR_TOLERANCE):
    """Check if a pixel matches the guide line color."""
    gr, gg, gb = guide_rgb
    return (abs(r - gr) < tolerance and 
            abs(g - gg) < tolerance and 
            abs(b - gb) < tolerance)


def is_white_or_light(r, g, b, threshold=BG_THRESHOLD):
    """Check if a pixel is considered white/light background."""
    c_max = max(r, g, b)
    c_min = min(r, g, b)
    delta = c_max - c_min
    
    saturation = 0
    if c_max > 0:
        saturation = delta / c_max
    
    is_grey_white = saturation < 0.15 and c_max > threshold
    return is_grey_white


def detect_guide_lines(img, guide_rgb):
    """
    Detect vertical guide lines in the image.
    Returns list of x-coordinates where vertical guide lines are found.
    """
    img_array = np.array(img.convert("RGBA"))
    height, width = img_array.shape[:2]
    
    vertical_lines = []
    
    # Scan each column
    for x in range(width):
        guide_pixel_count = 0
        for y in range(height):
            r, g, b = img_array[y, x, :3]
            if is_guide_color(r, g, b, guide_rgb):
                guide_pixel_count += 1
        
        # If most of the column is guide color, it's a vertical line
        if guide_pixel_count > height * 0.5:
            vertical_lines.append(x)
    
    # Cluster nearby x values
    if not vertical_lines:
        return []
    
    clustered = []
    current_cluster = [vertical_lines[0]]
    
    for x in vertical_lines[1:]:
        if x - current_cluster[-1] <= 3:
            current_cluster.append(x)
        else:
            clustered.append(sum(current_cluster) // len(current_cluster))
            current_cluster = [x]
    clustered.append(sum(current_cluster) // len(current_cluster))
    
    return clustered


def get_magenta_mask_hsv(img):
    """
    Get mask of magenta/pink guide lines using HSV color space.
    Returns boolean mask (True = Guide Line).
    """
    img = img.convert("RGBA")
    img_array = np.array(img)
    
    # Convert RGB to HSV (only for non-transparent pixels)
    rgb = img_array[:, :, :3].astype(np.float32) / 255.0
    
    # Calculate HSV manually for better control
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    
    # --- Aggressive Magenta Definition ---
    # Core Magenta: R and B are high, G is low
    # Anti-aliased Magenta: Can be lighter (high G) but R/B still dominate
    
    # 1. Basic Dominance check: R and B must be significantly higher than G
    #    (Anti-aliasing often blends with white, raising G, but R/B stay maxed)
    rb_avg = (r + b) / 2
    g_dist = rb_avg - g
    
    is_magenta_broad = (
        (g_dist > 0.05) &           # R/B strictly higher than G (was 0.1)
        (np.abs(r - b) < 0.25) &    # R and B are balanced (magenta hue)
        (r > 0.4) & (b > 0.4)       # Not too dark
    )
    
    # 2. Specific check for "Light Pink" blending with white
    #    (High R, High B, High G, but R/B still > G)
    is_light_pink = (
        (r > 0.7) & (b > 0.7) &     # Very bright
        (g < 0.95) &                # Not pure white
        (r > g + 0.02) & (b > g + 0.02) # Still slightly purplish
    )
    
    # 3. Specific check for "Dark Purple" blending with edges
    is_dark_purple = (
        (r > 0.3) & (b > 0.3) &
        (g < 0.4) &
        (np.abs(r - b) < 0.2)
    )

    # Combine masks
    magenta_mask = is_magenta_broad | is_light_pink | is_dark_purple
    return magenta_mask


def remove_background(img):
    """Remove white/light background from image."""
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if is_white_or_light(r, g, b):
                pixels[x, y] = (0, 0, 0, 0)
    
    return img


def smart_repair_guides(img):
    """
    Remove guide lines by extending neighbor colors (Inpainting).
    Handles 'black edges' by dilating the removal mask.
    Preserves transparency if neighbor is transparent.
    """
    from PIL import ImageFilter
    
    # 1. Get Magenta Mask (The Core Guide Line)
    magenta_mask = get_magenta_mask_hsv(img)
    
    # 2. Dilate to catch "Dark Edges"
    # Convert mask to image for filter
    mask_img = Image.fromarray((magenta_mask * 255).astype(np.uint8))
    # MaxFilter(3) is 3x3 dilation (radius 1)
    dilated_img = mask_img.filter(ImageFilter.MaxFilter(3))
    dilated_mask = np.array(dilated_img) > 0
    
    # 3. Prepare for Inpainting
    img_arr = np.array(img)
    height, width = img_arr.shape[:2]
    
    # We want to fill pixels that are in 'dilated_mask'
    # BUT we only care about pixels that are currently Opaque (or were guide lines).
    # If a pixel is ALREADY Transparent (from background removal), it stays Transparent?
    # NO. If the guide line was over the background, it is currently Opaque (Magenta).
    # We want it to become Transparent.
    # So we trust the "Valid Neighbors". 
    # If neighbor is Alpha=0, we copy Alpha=0.
    
    # 'Target' = pixels to be fixed (The dilated magenta area)
    # 'Source' = pixels to trust (Everything else)
    target_mask = dilated_mask.copy()
    
    # Iterative Propagation (Simulate Inpainting)
    # 3 passes is enough for thin lines (1-3px) + edges
    for _ in range(3):
        # Shift in 4 directions
        # For each direction, if I am Target and Neighbor is Valid, copy Neighbor
        
        # Valid pixels are those NOT in target_mask
        valid_mask = ~target_mask
        
        # We need to construct a 'contribution' from valid neighbors.
        # Simple approach: Priority to any valid neighbor.
        
        # Shifted Arrays (using slicing)
        # Up neighbor: shift array DOWN (row 0 becomes row 1's neighbor)
        # Down neighbor: shift array UP
        
        fixed_pixels = np.zeros_like(img_arr)
        fixed_counts = np.zeros((height, width), dtype=int)
        
        # Define shifts: (dy, dx)
        shifts = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        
        for dy, dx in shifts:
            # Shift the image to align neighbor to current pos
            shifted_img = np.roll(img_arr, (dy, dx), axis=(0, 1))
            shifted_valid = np.roll(valid_mask, (dy, dx), axis=(0, 1))
            
            # Handle boundary wrapping (zero out wrapped parts)
            if dy == 1: # Shifted down, top row invalid
                shifted_valid[0, :] = False
            elif dy == -1: # Shifted up, bottom row invalid
                shifted_valid[-1, :] = False
            elif dx == 1: # Shifted right, left col invalid
                shifted_valid[:, 0] = False
            elif dx == -1: # Shifted left, right col invalid
                shifted_valid[:, -1] = False
                
            # Identifying pixels where: Current is Target AND Neighbor is Valid
            can_fix = target_mask & shifted_valid
            
            # Accumulate (for averaging) or just Take First
            # Averaging is better for smooth edges
            # But simple copy is faster. Let's do simple overwrite (last wins)
            # Actually, average is safer for anti-aliasing.
            
            # Add to accumulator
            # We only touch pixels in 'can_fix'
            # Note: accumulating uint8 might overflow, cast to float
            # For simplicity in this script, let's just use 'max' (or random one)
            # Or just update inplace sequentially? No, parallel update is cleaner.
            
            # Let's take the First Valid Neighbor found in this pass
            # Update `img_arr` where `can_fix` is true
            
            # Only update pixels that haven't been updated in this pass yet?
            # Or allow multiple updates.
            
            mask_indices = np.where(can_fix)
            img_arr[mask_indices] = shifted_img[mask_indices]
            
            # Mark these as now valid for *next* pass?
            # No, strictly they become valid next pass.
            # Update the mask at end of loop.
            target_mask[mask_indices] = False # It's fixed now!
            
        # If we broke target_mask inside the loop, the next shift in same pass sees it as fixed?
        # That's fine, it accelerates propagation.
        
    return Image.fromarray(img_arr)


def crop_to_content(img, padding_pct=0.02):
    """Crop image to content with minimal padding."""
    bbox = img.getbbox()
    if not bbox:
        return img
    
    x1, y1, x2, y2 = bbox
    width, height = img.size
    pad = int(min(x2 - x1, y2 - y1) * padding_pct)
    
    x1 = max(0, x1 - pad)
    y1 = max(0, y1 - pad)
    x2 = min(width, x2 + pad)
    y2 = min(height, y2 + pad)
    
    return img.crop((x1, y1, x2, y2))


def generate_mirrored_strip(input_path, output_path, num_frames):
    """
    Generate a mirrored version of a sprite strip.
    Flips each frame horizontally but MAITAINS the frame order (1,2,3,4).
    """
    try:
        img = Image.open(input_path)
        w, h = img.size
        frame_width = w // num_frames
        
        frames = []
        for i in range(num_frames):
            # Extract frame
            frame = img.crop((i * frame_width, 0, (i + 1) * frame_width, h))
            # Flip frame
            flipped = frame.transpose(Image.FLIP_LEFT_RIGHT)
            frames.append(flipped)
            
        # Recombine
        new_w = frame_width * num_frames
        result = Image.new("RGBA", (new_w, h))
        for i, frame in enumerate(frames):
            result.paste(frame, (i * frame_width, 0))
            
        result.save(output_path)
        print(f"  ✨ Auto-generated mirror: {os.path.basename(output_path)}")
        return True
    except Exception as e:
        print(f"  ⚠️ Mirror generation failed: {e}")
        return False


def standardize_frames_unified(frames, target_size, mode='content'):
    """
    Standardize all frames using a UNIFIED bounding box.
    This ensures all frames are aligned consistently.
    """
    # Get original frame size (assuming all same size)
    orig_w, orig_h = frames[0].size
    
    if mode == 'canvas':
        # "Canvas" mode: Scale the entire input frame to fit target_size.
        scale = min(target_size / orig_w, target_size / orig_h)
    
    else:
        # "Content" mode: Find minimal bbox across all frames
        all_bboxes = []
        for frame in frames:
            bbox = frame.getbbox()
            if bbox:
                all_bboxes.append(bbox)
        
        if not all_bboxes:
            return [Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0)) for _ in frames]
        
        # Calculate unified bounding box (max of all)
        max_width = max(bbox[2] - bbox[0] for bbox in all_bboxes)
        max_height = max(bbox[3] - bbox[1] for bbox in all_bboxes)
        
        # Calculate scale to fit in target_size
        scale = min(target_size / max_width, target_size / max_height) * 0.95  # 95% to add margin
    
    # Second pass: process each frame with the SAME scale and centering
    processed = []
    
    if mode == 'canvas':
        # Simple resize of full frame (Canvas Mode)
        for frame in frames:
            w, h = frame.size
            final_w = int(w * scale)
            final_h = int(h * scale)
            
            if final_w <= 0 or final_h <= 0:
                processed.append(Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0)))
                continue
                
            resized = frame.resize((final_w, final_h), Image.LANCZOS)
            
            # Center on target canvas
            canvas = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
            off_x = (target_size - final_w) // 2
            off_y = (target_size - final_h) // 2
            canvas.paste(resized, (off_x, off_y))
            processed.append(canvas)
            
    else:
        # Content mode logic (original)
        for frame in frames:
            bbox = frame.getbbox()
            if not bbox:
                processed.append(Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0)))
                continue
            
            # Crop to content
            cropped = frame.crop(bbox)
            content_w, content_h = cropped.size
            
            # Use the unified scale factor
            scaled_w = int(content_w * scale)
            scaled_h = int(content_h * scale)
            
            if scaled_w <= 0 or scaled_h <= 0:
                processed.append(Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0)))
                continue
            
            resized = cropped.resize((scaled_w, scaled_h), Image.LANCZOS)
            
            # Center in target canvas
            canvas = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
            x_offset = (target_size - scaled_w) // 2
            y_offset = (target_size - scaled_h) // 2
            canvas.paste(resized, (x_offset, y_offset))
            processed.append(canvas)
    
    return processed


def split_strip_by_guides(img, guide_lines, num_frames):
    """
    Split image strip using detected guide lines.
    Falls back to equal division if guides not detected.
    """
    width, height = img.size
    
    # Filter out edge guide lines (within 5% of edges)
    edge_margin = int(width * 0.05)
    interior_guides = [x for x in guide_lines if edge_margin < x < width - edge_margin]
    
    print(f"  Interior guides (excluding edges): {interior_guides}")
    
    # We need num_frames-1 interior guide lines to split into num_frames
    if len(interior_guides) >= num_frames - 1:
        # Use the first num_frames-1 interior guides as dividers
        dividers = sorted(interior_guides[:num_frames - 1])
        boundaries = [0] + dividers + [width]
        print(f"  Using guide lines for splitting: {boundaries}")
    else:
        # Fall back to equal division
        frame_width = width // num_frames
        boundaries = [i * frame_width for i in range(num_frames + 1)]
        print(f"  Using equal division (no guides): {boundaries}")
    
    frames = []
    for i in range(num_frames):
        x1 = boundaries[i]
        x2 = boundaries[i + 1]
        frame = img.crop((x1, 0, x2, height))
        frames.append(frame)
    
    return frames


def split_strip_equal(img, num_frames):
    """Split image strip into equal-sized frames."""
    width, height = img.size
    frame_width = width // num_frames
    
    frames = []
    for i in range(num_frames):
        x1 = i * frame_width
        x2 = x1 + frame_width
        frame = img.crop((x1, 0, x2, height))
        frames.append(frame)
    
    return frames


def process_image_strip(input_path, num_frames, guide_color, target_size, name, scale_mode='content'):
    """
    Process a 4-frame image strip into a game-ready sprite sheet.
    
    Args:
        input_path: Path to input image strip
        num_frames: Number of frames in the strip
        guide_color: Hex color of guide lines (or None to skip)
        target_size: Target frame size in pixels
        name: Base name for output files
        scale_mode: 'content' or 'canvas'
    
    Returns:
        Output file path
    """
    print(f"\nLoading: {input_path}")
    img = Image.open(input_path).convert("RGBA")
    print(f"  Image size: {img.size}")
    
    guide_rgb = None
    guide_lines = []
    
    # Detect and remove guide lines using HSV color space
    if guide_color:
        try:
            guide_rgb = hex_to_rgb(guide_color)
            print(f"  Guide color: {guide_color} → RGB{guide_rgb}")
            
            # Detect vertical guide lines (for splitting reference)
            guide_lines = detect_guide_lines(img, guide_rgb)
            print(f"  Detected {len(guide_lines)} vertical guide lines at x={guide_lines}")
            
            # NOTE: We DO NOT remove lines here anymore. 
            # We defer removal to 'smart_repair_guides' after background removal
            # so we can fix the black edges using neighbor colors.
            
        except Exception as e:
            print(f"  Warning: Guide detection failed: {e}")
            pass
    
    # Split into frames
    if guide_lines:
        frames = split_strip_by_guides(img, guide_lines, num_frames)
    else:
        frames = split_strip_equal(img, num_frames)
    
    print(f"  Split into {len(frames)} frames")
    
    # Process each frame - remove background first
    bg_removed_frames = []
    for frame in frames:
        frame = remove_background(frame)
        
        # Smart repair guide lines (fill with neighbors)
        if guide_color:
            frame = smart_repair_guides(frame)
            
        bg_removed_frames.append(frame)
    
    # Standardize ALL frames with UNIFIED bounding box or canvas scaling
    processed_frames = standardize_frames_unified(bg_removed_frames, target_size, scale_mode)
    
    # Report content coverage
    for i, frame in enumerate(processed_frames):
        bbox = frame.getbbox()
        if bbox:
            content_w = bbox[2] - bbox[0]
            content_h = bbox[3] - bbox[1]
            coverage = (content_w * content_h) / (target_size * target_size) * 100
        else:
            coverage = 0
        print(f"    Frame {i+1}: content coverage = {coverage:.1f}%")
    
    # Create output sprite sheet
    strip_width = target_size * num_frames
    strip_height = target_size
    output = Image.new("RGBA", (strip_width, strip_height), (0, 0, 0, 0))
    
    for i, frame in enumerate(processed_frames):
        output.paste(frame, (i * target_size, 0))
    
    # Save output
    output_name = f"{name}_{num_frames}f.png"
    output_path = os.path.join(OUTPUT_DIR, output_name)
    output.save(output_path)
    print(f"  ✅ Saved: {output_path}")
    
    return output_path, output_name


def main():
    parser = argparse.ArgumentParser(
        description='Process image strips containing animation frames',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input image strip file')
    parser.add_argument('--frames', '-f', type=int, default=4,
                        help='Number of frames per row (default: 4)')
    parser.add_argument('--rows', '-r', type=int, default=1,
                        help='Number of rows (default: 1, for grid layouts use 2+)')
    parser.add_argument('--row-names', 
                        help='Comma-separated names for each row (e.g., "walk_down,walk_up")')
    parser.add_argument('--guide-color', '-g', default=None,
                        help=f'Hex color of guide lines (default: None, e.g., "#FF00FF")')
    parser.add_argument('--scale-mode', default='content', choices=['content', 'canvas'],
                        help='Scaling strategy: "content" (crop & max fit) or "canvas" (scale entire frame, preserves relative size)')
    parser.add_argument('--auto-mirror', action='store_true',
                        help='Automatically generate missing Left/Right view by mirroring the existing one')
    parser.add_argument('--output-size', '-s', type=int, default=DEFAULT_FRAME_SIZE,
                        help=f'Target frame size (default: {DEFAULT_FRAME_SIZE})')
    parser.add_argument('--name', '-n', required=True,
                        help='Base name for output files')
    parser.add_argument('--no-copy', action='store_true',
                        help='Do not copy to final asset directories')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Image Strip Processing Pipeline v1.1 (Grid Support)")
    print("=" * 60)
    print(f"Input: {args.input}")
    print(f"Frames per row: {args.frames}")
    print(f"Rows: {args.rows}")
    print(f"Guide color: {args.guide_color or 'None (equal division)'}")
    print(f"Scale mode: {args.scale_mode}")
    print(f"Output size: {args.output_size}×{args.output_size}")
    print(f"Name: {args.name}")
    print("=" * 60)
    
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}")
        return 1
    
    ensure_dirs()
    
    # Parse row names
    row_names = None
    if args.row_names:
        row_names = [n.strip() for n in args.row_names.split(',')]
    
    # Load image
    img = Image.open(args.input).convert("RGBA")
    width, height = img.size
    print(f"\nImage size: {width}×{height}")
    
    generated_files = []
    
    # Handle multi-row grid
    if args.rows > 1:
        row_height = height // args.rows
        print(f"Splitting into {args.rows} rows, each {row_height}px tall")
        
        for row_idx in range(args.rows):
            # Extract this row
            y1 = row_idx * row_height
            y2 = y1 + row_height
            row_img = img.crop((0, y1, width, y2))
            
            # Determine row name
            if row_names and row_idx < len(row_names):
                row_suffix = row_names[row_idx]
            else:
                row_suffix = f"row{row_idx}"
            
            row_name = f"{args.name}_{row_suffix}"
            print(f"\n--- Processing Row {row_idx}: {row_suffix} ---")
            
            # Save temp row image
            temp_path = f"/tmp/row_{row_idx}.png"
            row_img.save(temp_path)
            
            # Process this row
            output_path, output_name = process_image_strip(
                temp_path, args.frames, args.guide_color,
                args.output_size, row_name, args.scale_mode
            )
            # Store full path for copying
            generated_files.append((row_suffix, output_path, output_name))
    else:
        print(f"\n--- Processing Single Row ---")
        # Single row - original behavior
        output_path, output_name = process_image_strip(
            args.input, args.frames, args.guide_color,
            args.output_size, args.name, args.scale_mode
        )
        # Infer suffix
        row_suffix = "single"
        if "_left" in output_name: row_suffix = "left"
        elif "_right" in output_name: row_suffix = "right"
        
        generated_files.append((row_suffix, output_path, output_name))
    
    # --- AUTO MIRRORING LOGIC ---
    if args.auto_mirror:
        print(f"\n--- Auto-Mirroring Checks ---")
        extra_files = []
        
        for suffix, src_path, src_name in generated_files:
            # Check for Left -> Right
            if "left" in src_name.lower() and "right" not in src_name.lower():
                target_name = src_name.replace("left", "right").replace("Left", "Right")
                target_path = src_path.replace("left", "right").replace("Left", "Right")
                
                # Only if not already generated
                if not any(f[2] == target_name for f in generated_files) and not any(f[2] == target_name for f in extra_files):
                    if generate_mirrored_strip(src_path, target_path, args.frames):
                        extra_files.append(("mirror", target_path, target_name))
            
            # Check for Right -> Left
            elif "right" in src_name.lower() and "left" not in src_name.lower():
                target_name = src_name.replace("right", "left").replace("Right", "Left")
                target_path = src_path.replace("right", "left").replace("Right", "Left")
                
                if not any(f[2] == target_name for f in generated_files) and not any(f[2] == target_name for f in extra_files):
                    if generate_mirrored_strip(src_path, target_path, args.frames):
                        extra_files.append(("mirror", target_path, target_name))

        # Add mirrors to list for copying
        generated_files.extend(extra_files)

    # Copy to final directory
    if not args.no_copy:
        print("\n--- Copying to Assets ---")
        import shutil
        is_mob = any(x in args.name.lower() for x in ['mob_', 'enemy_', 'walk_', 'attack_'])
        target_dir = FINAL_MOB_DIR if is_mob else FINAL_ANIM_DIR
        
        for suffix, src, name in generated_files:
            dst = os.path.join(target_dir, name)
            shutil.copy(src, dst)
            print(f"✅ {suffix}: {dst}")

    print("\n" + "="*60)
    print("Processing complete!")
    print("="*60)
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
