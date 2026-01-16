#!/usr/bin/env python3
"""
Animation Frame Processing Pipeline
动画帧处理流水线

This script handles:
1. Extracting frames from video files (mp4, gif, mov, avi)
2. Processing PNG frame sequences from folders
3. Background removal
4. Uniform sizing
5. Sprite sheet assembly

Usage:
    # From video:
    python3 scripts/process_animation_frames.py --input raw_assets/videos/fire.mp4 --frames 4
    
    # From PNG folder:
    python3 scripts/process_animation_frames.py --input raw_assets/animations/fire/ --frames 4
    
    # With custom output name:
    python3 scripts/process_animation_frames.py --input raw_assets/videos/fire.mp4 --frames 16 --output anim_trap_fire_16f.png
    
    # With custom frame size:
    python3 scripts/process_animation_frames.py --input raw_assets/videos/fire.mp4 --frames 4 --size 128
"""

import cv2
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
TEMP_DIR = "raw_assets/ai_processed_transparent"

# Background removal settings (reusing from process_assets.py)
BG_THRESHOLD = 200

def ensure_dirs():
    """Create necessary directories if they don't exist."""
    for d in [OUTPUT_DIR, TEMP_DIR]:
        os.makedirs(d, exist_ok=True)

def is_white_or_light(r, g, b, threshold=BG_THRESHOLD):
    """Check if a pixel is considered white/light background."""
    c_max = max(r, g, b)
    c_min = min(r, g, b)
    delta = c_max - c_min
    
    saturation = 0
    if c_max > 0:
        saturation = delta / c_max
    
    is_grey_white = saturation < 0.15 and c_max > threshold
    is_checkerboard_grey = saturation < 0.1 and 180 < c_max < 230 and abs(r - g) < 15 and abs(g - b) < 15
    
    return is_grey_white or is_checkerboard_grey

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

def crop_to_content(img, padding_pct=0.05):
    """Crop image to content with optional padding."""
    bbox = img.getbbox()
    if not bbox:
        return img
    
    # Add padding
    x1, y1, x2, y2 = bbox
    width, height = img.size
    pad_x = int((x2 - x1) * padding_pct)
    pad_y = int((y2 - y1) * padding_pct)
    
    x1 = max(0, x1 - pad_x)
    y1 = max(0, y1 - pad_y)
    x2 = min(width, x2 + pad_x)
    y2 = min(height, y2 + pad_y)
    
    return img.crop((x1, y1, x2, y2))

def standardize_frame(img, target_size):
    """Resize and center frame to target size."""
    # Crop to content first
    img = crop_to_content(img)
    
    if img.size[0] == 0 or img.size[1] == 0:
        return Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    
    # Calculate scale to fit within target while preserving aspect ratio
    scale = min(target_size / img.size[0], target_size / img.size[1])
    new_w = int(img.size[0] * scale)
    new_h = int(img.size[1] * scale)
    
    if new_w <= 0 or new_h <= 0:
        return Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    
    # Resize
    resized = img.resize((new_w, new_h), Image.LANCZOS)
    
    # Center on target canvas
    final = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    paste_x = (target_size - new_w) // 2
    paste_y = (target_size - new_h) // 2
    final.paste(resized, (paste_x, paste_y))
    
    return final

def extract_frames_from_video(video_path, frame_count):
    """Extract evenly spaced frames from a video file."""
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"Error: Cannot open video file: {video_path}")
        return []
    
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    print(f"Video has {total_frames} frames, extracting {frame_count} evenly spaced frames...")
    
    if total_frames < frame_count:
        print(f"Warning: Video has fewer frames ({total_frames}) than requested ({frame_count})")
        frame_count = total_frames
    
    if frame_count == 0:
        print("Error: Cannot extract 0 frames")
        cap.release()
        return []
    
    step = total_frames / frame_count
    extracted = []
    
    for i in range(frame_count):
        frame_idx = int(i * step)
        cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = cap.read()
        
        if ret:
            # Convert BGR (OpenCV) to RGBA (PIL)
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGBA)
            img = Image.fromarray(frame_rgb)
            extracted.append(img)
            print(f"  Extracted frame {i+1}/{frame_count} (video frame {frame_idx})")
        else:
            print(f"  Warning: Failed to read frame at index {frame_idx}")
    
    cap.release()
    return extracted

def load_frames_from_folder(folder_path):
    """Load PNG frames from a folder, sorted by filename."""
    folder = Path(folder_path)
    png_files = sorted(folder.glob("*.png"), key=lambda x: natural_sort_key(x.name))
    
    if not png_files:
        print(f"Error: No PNG files found in {folder_path}")
        return []
    
    print(f"Found {len(png_files)} PNG files in folder")
    
    frames = []
    for i, png_file in enumerate(png_files):
        img = Image.open(png_file).convert("RGBA")
        frames.append(img)
        print(f"  Loaded frame {i+1}/{len(png_files)}: {png_file.name}")
    
    return frames

def natural_sort_key(s):
    """Key function for natural sorting of filenames."""
    return [int(text) if text.isdigit() else text.lower() 
            for text in re.split(r'(\d+)', s)]

def assemble_sprite_sheet(frames, target_size, separate_death_frame=False):
    """Assemble frames into a horizontal sprite sheet."""
    if not frames:
        print("Error: No frames to assemble")
        return None, None
    
    main_frames = frames
    death_frame = None
    
    # Handle 17-frame case (16 loop + 1 death)
    if len(frames) == 17 and separate_death_frame:
        main_frames = frames[:16]
        death_frame = frames[16]
        print("Separating death frame (frame 17) from loop animation")
    
    # Process and standardize each frame
    processed_frames = []
    for i, frame in enumerate(main_frames):
        # Remove background
        frame = remove_background(frame)
        # Standardize size
        frame = standardize_frame(frame, target_size)
        processed_frames.append(frame)
        print(f"  Processed frame {i+1}/{len(main_frames)}")
    
    # Create horizontal strip
    sheet_width = target_size * len(processed_frames)
    sheet_height = target_size
    
    sheet = Image.new("RGBA", (sheet_width, sheet_height), (0, 0, 0, 0))
    
    for i, frame in enumerate(processed_frames):
        sheet.paste(frame, (i * target_size, 0))
    
    # Process death frame if present
    death_sheet = None
    if death_frame is not None:
        death_frame = remove_background(death_frame)
        death_sheet = standardize_frame(death_frame, target_size)
        print("  Processed death frame")
    
    return sheet, death_sheet

def generate_output_name(input_path, frame_count):
    """Generate default output filename based on input."""
    input_name = Path(input_path).stem
    
    # Clean up name, remove frame indicators
    clean_name = re.sub(r'_?\d+frames?', '', input_name, flags=re.IGNORECASE)
    clean_name = re.sub(r'_?frame_?\d+', '', clean_name, flags=re.IGNORECASE)
    clean_name = clean_name.strip('_')
    
    # Add anim prefix if not present
    if not clean_name.startswith('anim_'):
        clean_name = f'anim_{clean_name}'
    
    return f"{clean_name}_{frame_count}f.png"

def main():
    parser = argparse.ArgumentParser(
        description='Process animation frames into sprite sheets',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input video file or folder containing PNG frames')
    parser.add_argument('--frames', '-f', type=int, required=True,
                        help='Number of frames to extract/use (4, 16, or 17)')
    parser.add_argument('--output', '-o', default=None,
                        help='Output filename (default: auto-generated)')
    parser.add_argument('--size', '-s', type=int, default=DEFAULT_FRAME_SIZE,
                        help=f'Frame size in pixels (default: {DEFAULT_FRAME_SIZE})')
    parser.add_argument('--separate-death', action='store_true',
                        help='For 17-frame input, separate the death frame')
    parser.add_argument('--no-copy', action='store_true',
                        help='Do not copy to assets/images/animations/')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Animation Frame Processing Pipeline v1.0")
    print("=" * 60)
    print(f"Input: {args.input}")
    print(f"Target frames: {args.frames}")
    print(f"Frame size: {args.size}x{args.size}")
    print("=" * 60 + "\n")
    
    ensure_dirs()
    
    input_path = Path(args.input)
    
    # Determine input type and load frames
    if input_path.is_dir():
        print("Processing PNG folder...")
        frames = load_frames_from_folder(input_path)
        
        if len(frames) != args.frames:
            print(f"Warning: Found {len(frames)} frames but requested {args.frames}")
            if len(frames) < args.frames:
                print("Error: Not enough frames in folder")
                return 1
            # Use only the requested number
            frames = frames[:args.frames]
            
    elif input_path.is_file():
        ext = input_path.suffix.lower()
        if ext in ['.mp4', '.gif', '.mov', '.avi', '.webm']:
            print(f"Processing video file ({ext})...")
            frames = extract_frames_from_video(str(input_path), args.frames)
        elif ext == '.png':
            print("Single PNG provided, treating as a single-frame animation")
            frames = [Image.open(input_path).convert("RGBA")]
        else:
            print(f"Error: Unsupported file type: {ext}")
            return 1
    else:
        print(f"Error: Input path does not exist: {args.input}")
        return 1
    
    if not frames:
        print("Error: No frames extracted/loaded")
        return 1
    
    print(f"\nLoaded {len(frames)} frames")
    
    # Assemble sprite sheet
    print("\nAssembling sprite sheet...")
    separate_death = args.separate_death or len(frames) == 17
    sheet, death_sheet = assemble_sprite_sheet(frames, args.size, separate_death)
    
    if sheet is None:
        print("Error: Failed to assemble sprite sheet")
        return 1
    
    # Generate output filename
    actual_frame_count = 16 if len(frames) == 17 and separate_death else len(frames)
    output_name = args.output or generate_output_name(args.input, actual_frame_count)
    
    # Ensure output has .png extension
    if not output_name.endswith('.png'):
        output_name += '.png'
    
    # Save to ai_ready_optimized
    output_path = os.path.join(OUTPUT_DIR, output_name)
    sheet.save(output_path)
    print(f"\n✅ Saved sprite sheet: {output_path}")
    print(f"   Dimensions: {sheet.size[0]}x{sheet.size[1]} ({actual_frame_count} frames)")
    
    # Save death frame if present
    if death_sheet is not None:
        death_name = output_name.replace(f'_{actual_frame_count}f.png', '_death.png')
        death_path = os.path.join(OUTPUT_DIR, death_name)
        death_sheet.save(death_path)
        print(f"✅ Saved death frame: {death_path}")
    
    # Copy to assets directory
    if not args.no_copy:
        final_dir = "assets/images/animations"
        os.makedirs(final_dir, exist_ok=True)
        
        import shutil
        final_path = os.path.join(final_dir, output_name)
        shutil.copy(output_path, final_path)
        print(f"✅ Copied to: {final_path}")
        
        if death_sheet is not None:
            death_final_path = os.path.join(final_dir, death_name)
            shutil.copy(death_path, death_final_path)
            print(f"✅ Copied death frame to: {death_final_path}")
    
    print("\n" + "=" * 60)
    print("Pipeline complete!")
    print("=" * 60)
    
    # Print Java integration code
    print("\n### Java Integration Code ###")
    print(f"""
Animation<TextureRegion> anim = textureManager.loadAnimatedSprite(
    "images/animations/{output_name}", {actual_frame_count}, 0.15f);
""")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
