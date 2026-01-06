from PIL import Image
import os
import re

input_dir = "raw_assets/ai_processed_transparent"
output_dir = "raw_assets/ai_ready_optimized"
UNIT_SIZE = 64

# Aspect ratio preservation settings
# For walls: allow slight distortion to fill, but not extreme
MAX_ASPECT_DISTORTION = 1.3  # Max 30% stretch in either direction

def get_asset_config(filename):
    """
    Parses filename to determine target grid size.
    Returns: (width_units, height_units, is_wall, is_tile)
    """
    w_units = 2  # Minimum 2x2 now
    h_units = 2
    is_wall = filename.startswith("wall_")
    is_tile = filename.startswith("tile_") or filename.startswith("floor_")

    # Parse Dimensions from Filename (e.g., "_2x2", "_3x3")
    dim_match = re.search(r'[_\-](\d+)x(\d+)', filename)
    if dim_match:
        w_units = int(dim_match.group(1))
        h_units = int(dim_match.group(2))

    return w_units, h_units, is_wall, is_tile

def process_asset_preserve_ratio(img, target_w, target_h, max_distortion=MAX_ASPECT_DISTORTION):
    """
    Process asset to target size while preserving natural aspect ratio.
    Allows slight distortion but prevents extreme stretching that looks unnatural.
    
    Args:
        img: PIL Image
        target_w: Target width in pixels
        target_h: Target height in pixels
        max_distortion: Maximum allowed aspect ratio change (e.g., 1.3 = 30%)
    
    Returns:
        Processed PIL Image at target dimensions
    """
    bbox = img.getbbox()
    if not bbox:
        return Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    
    content = img.crop(bbox)
    content_w, content_h = content.size
    
    # Calculate the aspect ratios
    content_ratio = content_w / max(content_h, 1)
    target_ratio = target_w / max(target_h, 1)
    
    # Calculate how much distortion would occur with direct resize
    distortion_factor = max(content_ratio / target_ratio, target_ratio / content_ratio)
    
    if distortion_factor <= max_distortion:
        # Distortion is acceptable, do direct resize (for seamless tiles, etc.)
        return content.resize((target_w, target_h), Image.LANCZOS)
    else:
        # Distortion too high - scale to fit while preserving ratio, then extend
        # This prevents the "cheap texture look" from extreme stretching
        
        # Calculate the scale to fit content within target bounds
        scale = min(target_w / content_w, target_h / content_h)
        new_w = int(content_w * scale)
        new_h = int(content_h * scale)
        
        if new_w <= 0 or new_h <= 0:
            return Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
        
        # Resize content preserving ratio
        resized = content.resize((new_w, new_h), Image.LANCZOS)
        
        # Create final image and extend edges to fill target size
        final = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
        
        # Center the resized content
        paste_x = (target_w - new_w) // 2
        paste_y = (target_h - new_h) // 2
        final.paste(resized, (paste_x, paste_y))
        
        # Edge extension: fill gaps by stretching edge pixels
        # This avoids transparent gaps while maintaining visual quality
        final = extend_edges(final, resized, paste_x, paste_y, new_w, new_h, target_w, target_h)
        
        return final

def extend_edges(final, content, paste_x, paste_y, content_w, content_h, target_w, target_h):
    """
    Extends edges of the content to fill the target dimensions.
    Uses edge pixel stretching for natural look.
    """
    import numpy as np
    
    final_array = np.array(final)
    content_array = np.array(content)
    
    # Fill left gap
    if paste_x > 0:
        left_edge = content_array[:, 0:1, :]  # First column
        left_fill = np.tile(left_edge, (1, paste_x, 1))
        final_array[paste_y:paste_y+content_h, 0:paste_x] = left_fill
    
    # Fill right gap
    right_start = paste_x + content_w
    if right_start < target_w:
        right_edge = content_array[:, -1:, :]  # Last column
        right_fill = np.tile(right_edge, (1, target_w - right_start, 1))
        final_array[paste_y:paste_y+content_h, right_start:target_w] = right_fill
    
    # Fill top gap (including corners)
    if paste_y > 0:
        top_edge = final_array[paste_y:paste_y+1, :, :]  # First row of full width
        top_fill = np.tile(top_edge, (paste_y, 1, 1))
        final_array[0:paste_y, :] = top_fill
    
    # Fill bottom gap (including corners)
    bottom_start = paste_y + content_h
    if bottom_start < target_h:
        bottom_edge = final_array[bottom_start-1:bottom_start, :, :]  # Last row of full width
        bottom_fill = np.tile(bottom_edge, (target_h - bottom_start, 1, 1))
        final_array[bottom_start:target_h, :] = bottom_fill
    
    return Image.fromarray(final_array)

def process_asset_stretch(img, target_w, target_h):
    """
    Process asset with full stretch to target size.
    Used for tiles that need seamless tiling.
    """
    bbox = img.getbbox()
    if not bbox:
        return Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    
    content = img.crop(bbox)
    return content.resize((target_w, target_h), Image.LANCZOS)

def process_asset_centered(img, target_w, target_h):
    """
    Process asset centered without stretching.
    Used for items and UI elements.
    """
    bbox = img.getbbox()
    if not bbox:
        return Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    
    content = img.crop(bbox)
    content_w, content_h = content.size
    
    # Scale to fit
    scale = min(target_w / content_w, target_h / content_h)
    if scale > 10:
        scale = 1.0
    
    new_w = int(content_w * scale)
    new_h = int(content_h * scale)
    
    if new_w <= 0 or new_h <= 0:
        return Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    
    resized = content.resize((new_w, new_h), Image.LANCZOS)
    final = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    paste_x = (target_w - new_w) // 2
    paste_y = (target_h - new_h) // 2
    final.paste(resized, (paste_x, paste_y))
    
    return final

def standardize_file(filename):
    input_path = os.path.join(input_dir, filename)
    output_path = os.path.join(output_dir, filename)

    try:
        img = Image.open(input_path).convert("RGBA")
        w_units, h_units, is_wall, is_tile = get_asset_config(filename)
        
        if is_wall:
            # WALL: Visual height = logical height + 0.5
            # Use direct high-quality resize (same as tiles) - walls are AI-generated complete textures
            target_w = w_units * UNIT_SIZE
            target_h = int((h_units + 0.5) * UNIT_SIZE)
            std_img = process_asset_stretch(img, target_w, target_h)
            mode = f"Wall ({w_units}x{h_units} -> {target_w}x{target_h}px, direct resize)"
            
        elif is_tile:
            # TILES: Full stretch for seamless tiling (this is expected for tiles)
            target_w = w_units * UNIT_SIZE
            target_h = h_units * UNIT_SIZE
            std_img = process_asset_stretch(img, target_w, target_h)
            mode = "Tile (100% fill, seamless)"
            
        elif filename.startswith("item_") or filename.startswith("ui_"):
            # ITEMS/UI: Centered, no stretch
            target_w = UNIT_SIZE
            target_h = UNIT_SIZE
            std_img = process_asset_centered(img, target_w, target_h)
            mode = "Item (centered, no stretch)"
            
        else:
            # DEFAULT: Use ratio-preserving for unknown assets
            target_w = w_units * UNIT_SIZE
            target_h = h_units * UNIT_SIZE
            std_img = process_asset_preserve_ratio(img, target_w, target_h)
            mode = f"Default ({target_w}x{target_h}px, ratio-aware)"
            
        std_img.save(output_path)
        print(f"[{mode}] {filename}")

    except Exception as e:
        print(f"Error: {filename} - {e}")

print("=" * 60)
print("Asset Standardization v5 - Ratio-Aware Processing")
print("=" * 60)
print(f"Input:  {input_dir}")
print(f"Output: {output_dir}")
print(f"Unit Size: {UNIT_SIZE}px")
print(f"Max Distortion: {MAX_ASPECT_DISTORTION:.0%}")
print("=" * 60 + "\n")

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

if not os.path.exists(input_dir):
    print(f"Warning: Input directory '{input_dir}' does not exist.")
else:
    files = [f for f in os.listdir(input_dir) if f.endswith(".png")]
    print(f"Found {len(files)} PNG files to process.\n")
    
    for filename in files:
        standardize_file(filename)

print("\n" + "=" * 60)
print("Standardization complete!")
print("=" * 60)
