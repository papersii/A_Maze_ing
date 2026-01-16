#!/usr/bin/env python3
"""
Mob Sprite Sheet Processing Pipeline
移动实体 Sprite Sheet 处理流水线

This script processes Grid-layout sprite sheets for mobile entities (enemies, NPCs).
It handles the 8-row × 4-column layout with frame counter labels.

Layout expected:
- Row 0: Walk Down (4 frames)
- Row 1: Walk Right (4 frames)
- Row 2: Walk Up (4 frames)
- Row 3: Walk Left (4 frames)
- Row 4: Attack Down (4 frames)
- Row 5: Attack Right (4 frames)
- Row 6: Attack Up (4 frames)
- Row 7: Attack Left (4 frames)

Usage:
    # From Grid image:
    python3 scripts/process_mob_spritesheet.py --input raw_assets/animations/boar_grid.png --rows 8 --cols 4
    
    # With custom output name:
    python3 scripts/process_mob_spritesheet.py --input boar_grid.png --rows 8 --cols 4 --name mob_boar_grassland
    
    # With label cropping (remove top 12px per cell):
    python3 scripts/process_mob_spritesheet.py --input boar_grid.png --rows 8 --cols 4 --crop-label 12
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
FINAL_DIR = "assets/images/mobs"
LABEL_CROP_HEIGHT = 12  # Pixels to crop from top of each cell (frame label area)

# Row definitions for mobile entities
ROW_DEFINITIONS = [
    ("walk_down", "Walk Down"),
    ("walk_right", "Walk Right"),
    ("walk_up", "Walk Up"),
    ("walk_left", "Walk Left"),
    ("attack_down", "Attack Down"),
    ("attack_right", "Attack Right"),
    ("attack_up", "Attack Up"),
    ("attack_left", "Attack Left"),
]

# Background removal settings
BG_THRESHOLD = 200


def ensure_dirs():
    """Create necessary directories if they don't exist."""
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(FINAL_DIR, exist_ok=True)


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


def crop_to_content(img, padding_pct=0.02):
    """Crop image to content with minimal padding."""
    bbox = img.getbbox()
    if not bbox:
        return img
    
    # Add minimal padding
    x1, y1, x2, y2 = bbox
    width, height = img.size
    pad = int(min(x2 - x1, y2 - y1) * padding_pct)
    
    x1 = max(0, x1 - pad)
    y1 = max(0, y1 - pad)
    x2 = min(width, x2 + pad)
    y2 = min(height, y2 + pad)
    
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


def detect_grid_layout(img, expected_rows, expected_cols):
    """
    Detect grid cell boundaries in the image.
    Returns cell width and height.
    """
    width, height = img.size
    
    # Calculate expected cell size
    cell_width = width // expected_cols
    cell_height = height // expected_rows
    
    print(f"  Detected cell size: {cell_width}×{cell_height} px")
    
    return cell_width, cell_height


def extract_cells(img, rows, cols, label_crop=LABEL_CROP_HEIGHT):
    """
    Extract individual cells from a grid image.
    
    Args:
        img: PIL Image
        rows: Number of rows
        cols: Number of columns
        label_crop: Pixels to crop from top of each cell (label area)
    
    Returns:
        2D list of cell images [row][col]
    """
    width, height = img.size
    cell_width = width // cols
    cell_height = height // rows
    
    cells = []
    
    for row in range(rows):
        row_cells = []
        for col in range(cols):
            # Calculate cell boundaries
            x1 = col * cell_width
            y1 = row * cell_height
            x2 = x1 + cell_width
            y2 = y1 + cell_height
            
            # Extract cell
            cell = img.crop((x1, y1, x2, y2))
            
            # Crop label area from top if specified
            if label_crop > 0 and cell.size[1] > label_crop:
                cell = cell.crop((0, label_crop, cell.size[0], cell.size[1]))
            
            row_cells.append(cell)
        
        cells.append(row_cells)
    
    return cells, cell_width, cell_height - label_crop


def process_mob_spritesheet(input_path, rows, cols, name, target_size, label_crop):
    """
    Process a mob spritesheet from grid layout.
    
    Args:
        input_path: Path to input grid image
        rows: Number of rows in grid
        cols: Number of columns in grid
        name: Base name for output files
        target_size: Target frame size in pixels
        label_crop: Pixels to crop from top of each cell
    
    Returns:
        List of generated files
    """
    print(f"\nLoading: {input_path}")
    img = Image.open(input_path).convert("RGBA")
    print(f"  Image size: {img.size}")
    
    # Extract cells
    print(f"\nExtracting {rows}×{cols} grid...")
    cells, cell_w, cell_h = extract_cells(img, rows, cols, label_crop)
    print(f"  Extracted {len(cells)} rows × {len(cells[0])} cols")
    print(f"  Cell content size: {cell_w}×{cell_h} px")
    
    generated_files = []
    
    # Process each row as a separate animation strip
    for row_idx, row_cells in enumerate(cells):
        if row_idx < len(ROW_DEFINITIONS):
            anim_id, anim_name = ROW_DEFINITIONS[row_idx]
        else:
            anim_id = f"row{row_idx}"
            anim_name = f"Row {row_idx}"
        
        print(f"\n  Processing Row {row_idx}: {anim_name}")
        
        # Process each frame
        processed_frames = []
        for col_idx, cell in enumerate(row_cells):
            # Remove background
            cell = remove_background(cell)
            # Standardize size
            frame = standardize_frame(cell, target_size)
            processed_frames.append(frame)
        
        # Create horizontal strip for this animation
        strip_width = target_size * len(processed_frames)
        strip_height = target_size
        strip = Image.new("RGBA", (strip_width, strip_height), (0, 0, 0, 0))
        
        for i, frame in enumerate(processed_frames):
            strip.paste(frame, (i * target_size, 0))
        
        # Save strip
        output_name = f"{name}_{anim_id}_{len(processed_frames)}f.png"
        output_path = os.path.join(OUTPUT_DIR, output_name)
        strip.save(output_path)
        print(f"    ✅ Saved: {output_path}")
        
        generated_files.append((anim_id, anim_name, output_name))
    
    # Also create a combined sprite sheet (all rows stacked)
    print(f"\nCreating combined sprite sheet...")
    combined_height = target_size * rows
    combined_width = target_size * cols
    combined = Image.new("RGBA", (combined_width, combined_height), (0, 0, 0, 0))
    
    for row_idx, row_cells in enumerate(cells):
        for col_idx, cell in enumerate(row_cells):
            # Remove background
            cell = remove_background(cell)
            # Standardize size
            frame = standardize_frame(cell, target_size)
            # Paste to combined sheet
            combined.paste(frame, (col_idx * target_size, row_idx * target_size))
    
    combined_name = f"{name}_spritesheet.png"
    combined_path = os.path.join(OUTPUT_DIR, combined_name)
    combined.save(combined_path)
    print(f"  ✅ Saved combined: {combined_path}")
    
    return generated_files, combined_name


def main():
    parser = argparse.ArgumentParser(
        description='Process mob sprite sheet from grid layout',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input grid image file')
    parser.add_argument('--rows', '-r', type=int, default=8,
                        help='Number of rows in grid (default: 8)')
    parser.add_argument('--cols', '-c', type=int, default=4,
                        help='Number of columns in grid (default: 4)')
    parser.add_argument('--name', '-n', default=None,
                        help='Base name for output files (default: from input filename)')
    parser.add_argument('--size', '-s', type=int, default=DEFAULT_FRAME_SIZE,
                        help=f'Target frame size in pixels (default: {DEFAULT_FRAME_SIZE})')
    parser.add_argument('--crop-label', '-l', type=int, default=LABEL_CROP_HEIGHT,
                        help=f'Pixels to crop from top of each cell (default: {LABEL_CROP_HEIGHT})')
    parser.add_argument('--no-copy', action='store_true',
                        help='Do not copy to assets/images/mobs/')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Mob Sprite Sheet Processing Pipeline v1.0")
    print("=" * 60)
    print(f"Input: {args.input}")
    print(f"Grid: {args.rows} rows × {args.cols} cols")
    print(f"Frame size: {args.size}×{args.size}")
    print(f"Label crop: {args.crop_label}px from top")
    print("=" * 60)
    
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}")
        return 1
    
    ensure_dirs()
    
    # Generate base name from input if not provided
    if args.name is None:
        args.name = Path(args.input).stem
        # Clean up name
        args.name = re.sub(r'_?grid', '', args.name, flags=re.IGNORECASE)
        args.name = re.sub(r'_?spritesheet', '', args.name, flags=re.IGNORECASE)
        args.name = args.name.strip('_')
        if not args.name.startswith('mob_'):
            args.name = f'mob_{args.name}'
    
    print(f"Output name prefix: {args.name}")
    
    # Process the sprite sheet
    generated, combined = process_mob_spritesheet(
        args.input, args.rows, args.cols,
        args.name, args.size, args.crop_label
    )
    
    # Copy to assets directory
    if not args.no_copy:
        import shutil
        print(f"\nCopying to {FINAL_DIR}...")
        
        # Copy individual strips
        for anim_id, anim_name, filename in generated:
            src = os.path.join(OUTPUT_DIR, filename)
            dst = os.path.join(FINAL_DIR, filename)
            shutil.copy(src, dst)
            print(f"  ✅ {filename}")
        
        # Copy combined sheet
        src = os.path.join(OUTPUT_DIR, combined)
        dst = os.path.join(FINAL_DIR, combined)
        shutil.copy(src, dst)
        print(f"  ✅ {combined}")
    
    print("\n" + "=" * 60)
    print("Processing complete!")
    print("=" * 60)
    
    # Print summary
    print("\n### Generated Animation Strips ###")
    print(f"| Row | Animation | Filename |")
    print(f"|-----|-----------|----------|")
    for i, (anim_id, anim_name, filename) in enumerate(generated):
        print(f"| {i} | {anim_name} | `{filename}` |")
    
    print(f"\n### Combined Sprite Sheet ###")
    print(f"`{combined}` ({args.cols}×{args.rows} grid, {args.size}×{args.size}px per frame)")
    
    # Print Java integration code
    print("\n### Java Integration Code ###")
    print(f"""
// Load mob animations from individual strips
public void loadMobAnimations(String baseName) {{
    String[] directions = {{"down", "right", "up", "left"}};
    String[] actions = {{"walk", "attack"}};
    
    for (String action : actions) {{
        for (String dir : directions) {{
            String path = "images/mobs/" + baseName + "_" + action + "_" + dir + "_4f.png";
            Animation<TextureRegion> anim = loadAnimatedSprite(path, 4, 0.15f);
            // Store in animation map...
        }}
    }}
}}

// Or load from combined sprite sheet
public void loadMobFromSheet(String sheetPath) {{
    Texture sheet = new Texture(Gdx.files.internal(sheetPath));
    int frameSize = {args.size};
    int cols = {args.cols};
    int rows = {args.rows};
    
    Animation<TextureRegion>[] animations = new Animation[rows];
    for (int row = 0; row < rows; row++) {{
        TextureRegion[] frames = new TextureRegion[cols];
        for (int col = 0; col < cols; col++) {{
            frames[col] = new TextureRegion(sheet, col * frameSize, row * frameSize, frameSize, frameSize);
        }}
        animations[row] = new Animation<>(0.15f, frames);
        animations[row].setPlayMode(Animation.PlayMode.LOOP);
    }}
}}
""")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
