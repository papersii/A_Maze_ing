#!/usr/bin/env python3
"""
Color Transform Pipeline for Animation Sprites
动画纹理颜色变换流水线

This script transforms animation sprite sheets between different game themes
using color space mapping techniques.

Supported modes:
- hue_shift: Rotate hue in HSV color space
- palette_map: Map source colors to target theme colors
- temperature: Adjust color temperature (warm/cool)

Usage:
    # Single theme transformation
    python3 scripts/color_transform.py --input anim_fire_4f.png --target ice
    
    # Generate all theme variants
    python3 scripts/color_transform.py --input anim_fire_4f.png --all-themes
    
    # Specific transformation mode
    python3 scripts/color_transform.py --input anim_fire_4f.png --target space --mode hue_shift
"""

import os
import sys
import argparse
import numpy as np
from PIL import Image
from pathlib import Path
import colorsys

# Theme color palettes (Hue values in 0-360 range)
THEME_PALETTES = {
    'grassland': {
        'primary_hue': 120,      # Green
        'secondary_hue': 90,     # Yellow-Green
        'accent_hue': 180,       # Cyan
        'temperature': 0.1,       # Slightly warm
        'saturation_mod': 1.0,
        'value_mod': 1.05,
        'description': 'Lush green, natural tones'
    },
    'desert': {
        'primary_hue': 30,       # Orange
        'secondary_hue': 45,     # Yellow-Orange
        'accent_hue': 15,        # Red-Orange
        'temperature': 0.3,       # Warm
        'saturation_mod': 1.1,
        'value_mod': 1.0,
        'description': 'Warm sand and ember tones'
    },
    'ice': {
        'primary_hue': 200,      # Cyan-Blue
        'secondary_hue': 220,    # Blue
        'accent_hue': 180,       # Cyan
        'temperature': -0.3,      # Cold
        'saturation_mod': 0.9,
        'value_mod': 1.1,
        'description': 'Cold blue and crystal tones'
    },
    'jungle': {
        'primary_hue': 90,       # Yellow-Green
        'secondary_hue': 150,    # Teal-Green
        'accent_hue': 300,       # Purple (bioluminescent)
        'temperature': 0.05,      # Slightly warm
        'saturation_mod': 1.2,
        'value_mod': 0.95,
        'description': 'Deep green with bioluminescent accents'
    },
    'space': {
        'primary_hue': 270,      # Purple
        'secondary_hue': 190,    # Cyan
        'accent_hue': 330,       # Magenta/Pink
        'temperature': -0.1,      # Slightly cold
        'saturation_mod': 1.3,
        'value_mod': 1.0,
        'description': 'Neon cyberpunk with electric accents'
    }
}

# Default output directory
OUTPUT_DIR = "raw_assets/ai_ready_optimized"
FINAL_DIR = "assets/images/animations"


def rgb_to_hsv(r, g, b):
    """Convert RGB (0-255) to HSV (0-360, 0-1, 0-1)."""
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    return h * 360, s, v


def hsv_to_rgb(h, s, v):
    """Convert HSV (0-360, 0-1, 0-1) to RGB (0-255)."""
    h = h / 360.0
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, min(1.0, max(0.0, s)), min(1.0, max(0.0, v)))
    return int(r * 255), int(g * 255), int(b * 255)


def apply_hue_shift(img, target_theme, source_hue=None):
    """
    Apply hue shift to transform colors to target theme.
    
    Args:
        img: PIL Image in RGBA mode
        target_theme: Target theme name
        source_hue: Optional source hue to shift from (auto-detect if None)
    
    Returns:
        Transformed PIL Image
    """
    if target_theme not in THEME_PALETTES:
        print(f"Warning: Unknown theme '{target_theme}', using grassland")
        target_theme = 'grassland'
    
    theme = THEME_PALETTES[target_theme]
    target_hue = theme['primary_hue']
    sat_mod = theme['saturation_mod']
    val_mod = theme['value_mod']
    
    img_array = np.array(img)
    height, width, channels = img_array.shape
    
    # Process pixels
    result = img_array.copy()
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = img_array[y, x]
            
            # Skip transparent pixels
            if a < 10:
                continue
            
            # Convert to HSV
            h, s, v = rgb_to_hsv(r, g, b)
            
            # Auto-detect source hue from first significant pixel
            if source_hue is None:
                if s > 0.2 and v > 0.2:  # Has significant color
                    source_hue = h
            
            # Apply hue shift
            if source_hue is not None:
                hue_diff = target_hue - source_hue
                new_h = (h + hue_diff) % 360
            else:
                new_h = h
            
            # Apply saturation and value modifiers
            new_s = min(1.0, s * sat_mod)
            new_v = min(1.0, v * val_mod)
            
            # Preserve contrast - don't shift grey tones much
            if s < 0.15:  # Near grey
                new_h = h
                new_s = s
            
            # Convert back to RGB
            new_r, new_g, new_b = hsv_to_rgb(new_h, new_s, new_v)
            
            result[y, x] = [new_r, new_g, new_b, a]
    
    return Image.fromarray(result.astype(np.uint8))


def apply_temperature_shift(img, target_theme):
    """
    Apply color temperature adjustment.
    Positive = warmer (more red/yellow), Negative = cooler (more blue)
    """
    if target_theme not in THEME_PALETTES:
        print(f"Warning: Unknown theme '{target_theme}', using grassland")
        target_theme = 'grassland'
    
    temp = THEME_PALETTES[target_theme]['temperature']
    
    img_array = np.array(img).astype(np.float32)
    
    # Temperature adjustment: shift red/blue balance
    if temp > 0:  # Warmer
        img_array[:, :, 0] = np.clip(img_array[:, :, 0] * (1 + temp * 0.3), 0, 255)  # More red
        img_array[:, :, 2] = np.clip(img_array[:, :, 2] * (1 - temp * 0.2), 0, 255)  # Less blue
    else:  # Cooler
        img_array[:, :, 0] = np.clip(img_array[:, :, 0] * (1 + temp * 0.2), 0, 255)  # Less red
        img_array[:, :, 2] = np.clip(img_array[:, :, 2] * (1 - temp * 0.3), 0, 255)  # More blue
    
    return Image.fromarray(img_array.astype(np.uint8))


def apply_palette_map(img, target_theme):
    """
    Apply palette-based color mapping.
    Maps detected dominant colors to theme palette.
    """
    # First apply hue shift as base
    result = apply_hue_shift(img, target_theme)
    
    # Then apply temperature adjustment
    result = apply_temperature_shift(result, target_theme)
    
    return result


def detect_source_theme(img):
    """Attempt to detect the source theme based on dominant colors."""
    img_array = np.array(img)
    
    # Sample non-transparent pixels
    samples = []
    height, width = img_array.shape[:2]
    
    for y in range(0, height, 4):
        for x in range(0, width, 4):
            if img_array[y, x, 3] > 128:  # Non-transparent
                r, g, b = img_array[y, x, :3]
                if r > 10 or g > 10 or b > 10:  # Not black
                    h, s, v = rgb_to_hsv(r, g, b)
                    if s > 0.2:  # Has color
                        samples.append(h)
    
    if not samples:
        return None, None
    
    # Find dominant hue
    avg_hue = sum(samples) / len(samples)
    
    # Try to match to a theme
    best_theme = None
    best_diff = 360
    
    for theme_name, theme_data in THEME_PALETTES.items():
        diff = min(abs(avg_hue - theme_data['primary_hue']), 
                   360 - abs(avg_hue - theme_data['primary_hue']))
        if diff < best_diff:
            best_diff = diff
            best_theme = theme_name
    
    return best_theme, avg_hue


def generate_output_name(input_name, target_theme, current_theme=None):
    """Generate output filename with theme replaced."""
    name = Path(input_name).stem
    
    # Try to replace existing theme in name
    for theme in THEME_PALETTES.keys():
        if theme in name:
            name = name.replace(theme, target_theme)
            return name + '.png'
    
    # No theme found in name, insert before frame count
    import re
    match = re.search(r'_(\d+f)$', name)
    if match:
        base = name[:match.start()]
        frames = match.group(1)
        return f"{base}_{target_theme}_{frames}.png"
    
    # Fallback: append theme
    return f"{name}_{target_theme}.png"


def transform_animation(input_path, target_theme, mode='palette_map'):
    """
    Transform an animation sprite sheet to a target theme.
    
    Args:
        input_path: Path to input sprite sheet
        target_theme: Target theme name
        mode: Transformation mode ('hue_shift', 'palette_map', 'temperature')
    
    Returns:
        Transformed PIL Image
    """
    img = Image.open(input_path).convert("RGBA")
    
    print(f"  Input size: {img.size}")
    
    # Detect source theme
    source_theme, source_hue = detect_source_theme(img)
    if source_theme:
        print(f"  Detected source theme: {source_theme} (hue: {source_hue:.0f}°)")
    
    if source_theme == target_theme:
        print(f"  Warning: Source and target theme appear to be the same")
    
    # Apply transformation based on mode
    if mode == 'hue_shift':
        result = apply_hue_shift(img, target_theme, source_hue)
    elif mode == 'temperature':
        result = apply_temperature_shift(img, target_theme)
    else:  # palette_map (default)
        result = apply_palette_map(img, target_theme)
    
    return result


def main():
    parser = argparse.ArgumentParser(
        description='Transform animation sprites between game themes',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input sprite sheet file')
    parser.add_argument('--target', '-t', default=None,
                        help='Target theme (grassland, desert, ice, jungle, space)')
    parser.add_argument('--mode', '-m', default='palette_map',
                        choices=['hue_shift', 'palette_map', 'temperature'],
                        help='Transformation mode (default: palette_map)')
    parser.add_argument('--all-themes', action='store_true',
                        help='Generate variants for all themes')
    parser.add_argument('--output', '-o', default=None,
                        help='Output filename (default: auto-generated)')
    parser.add_argument('--output-dir', '-d', default=OUTPUT_DIR,
                        help=f'Output directory (default: {OUTPUT_DIR})')
    parser.add_argument('--preserve-contrast', action='store_true',
                        help='Preserve original contrast more strictly')
    parser.add_argument('--no-copy', action='store_true',
                        help='Do not copy to assets/images/animations/')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Color Transform Pipeline v1.0")
    print("=" * 60)
    print(f"Input: {args.input}")
    print(f"Mode: {args.mode}")
    
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}")
        return 1
    
    # Ensure output directories exist
    os.makedirs(args.output_dir, exist_ok=True)
    os.makedirs(FINAL_DIR, exist_ok=True)
    
    # Determine themes to process
    if args.all_themes:
        themes = list(THEME_PALETTES.keys())
        print(f"Generating all {len(themes)} theme variants")
    elif args.target:
        themes = [args.target]
    else:
        print("Error: Specify --target THEME or use --all-themes")
        return 1
    
    print("=" * 60 + "\n")
    
    input_name = Path(args.input).name
    results = []
    
    for theme in themes:
        print(f"\n[{theme.upper()}] {THEME_PALETTES[theme]['description']}")
        print("-" * 40)
        
        # Transform
        transformed = transform_animation(args.input, theme, args.mode)
        
        # Generate output name
        if args.output and not args.all_themes:
            output_name = args.output
        else:
            output_name = generate_output_name(input_name, theme)
        
        # Ensure .png extension
        if not output_name.endswith('.png'):
            output_name += '.png'
        
        # Save to output directory
        output_path = os.path.join(args.output_dir, output_name)
        transformed.save(output_path)
        print(f"  ✅ Saved: {output_path}")
        
        # Copy to assets directory
        if not args.no_copy:
            final_path = os.path.join(FINAL_DIR, output_name)
            transformed.save(final_path)
            print(f"  ✅ Copied to: {final_path}")
        
        results.append((theme, output_name))
    
    print("\n" + "=" * 60)
    print("Transformation complete!")
    print("=" * 60)
    
    print("\n### Generated Files ###")
    for theme, name in results:
        print(f"  [{theme:10}] {name}")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
