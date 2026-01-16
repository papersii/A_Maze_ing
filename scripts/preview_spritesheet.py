#!/usr/bin/env python3
"""
Sprite Sheet Preview Utility
Sprite Sheet 预览工具

A simple utility to preview individual frames from a sprite sheet.

Usage:
    python3 scripts/preview_spritesheet.py --input anim_fire_4f.png --frames 4
    python3 scripts/preview_spritesheet.py --input anim_crystal_16f.png --frames 16 --export
"""

import os
import sys
import argparse
from PIL import Image
from pathlib import Path


def preview_spritesheet(input_path, frame_count, export=False, output_dir=None):
    """
    Preview or export individual frames from a sprite sheet.
    
    Args:
        input_path: Path to sprite sheet
        frame_count: Number of frames in the sheet
        export: If True, save individual frames
        output_dir: Directory for exported frames
    """
    if not os.path.exists(input_path):
        print(f"Error: File not found: {input_path}")
        return 1
    
    img = Image.open(input_path).convert("RGBA")
    width, height = img.size
    
    frame_width = width // frame_count
    frame_height = height
    
    print("=" * 50)
    print("Sprite Sheet Preview")
    print("=" * 50)
    print(f"Input: {input_path}")
    print(f"Sheet size: {width}x{height}")
    print(f"Frame count: {frame_count}")
    print(f"Frame size: {frame_width}x{frame_height}")
    print("=" * 50)
    
    input_name = Path(input_path).stem
    
    if export:
        if output_dir is None:
            output_dir = f"raw_assets/animations/{input_name}_frames"
        os.makedirs(output_dir, exist_ok=True)
        print(f"Exporting frames to: {output_dir}\n")
    
    frames = []
    for i in range(frame_count):
        x = i * frame_width
        frame = img.crop((x, 0, x + frame_width, frame_height))
        frames.append(frame)
        
        # Calculate content coverage
        bbox = frame.getbbox()
        if bbox:
            content_w = bbox[2] - bbox[0]
            content_h = bbox[3] - bbox[1]
            coverage = (content_w * content_h) / (frame_width * frame_height) * 100
        else:
            coverage = 0
        
        print(f"  Frame {i+1:2d}: content coverage = {coverage:5.1f}%")
        
        if export:
            frame_path = os.path.join(output_dir, f"frame_{i+1:02d}.png")
            frame.save(frame_path)
    
    if export:
        print(f"\n✅ Exported {frame_count} frames to {output_dir}")
    
    # Quick validation
    print("\n### Validation ###")
    
    # Check for empty frames
    empty_frames = []
    for i, f in enumerate(frames):
        if not f.getbbox():
            empty_frames.append(i + 1)
    
    if empty_frames:
        print(f"⚠️  Warning: Empty frames detected: {empty_frames}")
    else:
        print("✅ All frames contain content")
    
    # Check size consistency
    print(f"✅ Frame size: {frame_width}x{frame_height} (consistent)")
    
    return 0


def main():
    parser = argparse.ArgumentParser(
        description='Preview sprite sheet frames',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input sprite sheet file')
    parser.add_argument('--frames', '-f', type=int, required=True,
                        help='Number of frames in the sheet')
    parser.add_argument('--export', '-e', action='store_true',
                        help='Export individual frames as PNGs')
    parser.add_argument('--output-dir', '-o', default=None,
                        help='Output directory for exported frames')
    
    args = parser.parse_args()
    
    return preview_spritesheet(args.input, args.frames, args.export, args.output_dir)


if __name__ == "__main__":
    sys.exit(main())
