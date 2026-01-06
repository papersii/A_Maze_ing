from PIL import Image, ImageFilter, ImageDraw
import os
import re
import numpy as np

input_dir = "raw_assets/ai_generated_raw"
output_dir = "raw_assets/ai_processed_transparent"

# Watermark detection settings
WATERMARK_DETECTION_HEIGHT_PCT = 0.08  # Bottom 8% height to check
WATERMARK_DETECTION_WIDTH_PCT = 0.25   # Right 25% width to check
INPAINT_RADIUS = 5  # Radius for inpainting blur

def is_white_or_light(r, g, b, threshold=200):
    """
    Checks if a pixel is considered white/light background.
    """
    c_max = max(r, g, b)
    c_min = min(r, g, b)
    delta = c_max - c_min
    
    saturation = 0
    if c_max > 0:
        saturation = delta / c_max
    
    is_grey_white = saturation < 0.15 and c_max > threshold
    is_checkerboard_grey = saturation < 0.1 and 180 < c_max < 230 and abs(r - g) < 15 and abs(g - b) < 15
    
    return is_grey_white or is_checkerboard_grey

def detect_watermark_region(img):
    """
    Detects if there's a watermark-like pattern in the bottom-right corner.
    Returns the bounding box of the watermark region if detected, None otherwise.
    """
    width, height = img.size
    pixels = img.load()
    
    # Define detection area (Bottom-Right corner)
    check_h = int(height * WATERMARK_DETECTION_HEIGHT_PCT)
    check_w = int(width * WATERMARK_DETECTION_WIDTH_PCT)
    
    start_x = width - check_w
    start_y = height - check_h
    
    # Get the background color (from top-left, assumed to be background)
    bg_r, bg_g, bg_b, bg_a = pixels[0, 0]
    
    # Count pixels that differ significantly from background (potential watermark)
    non_bg_count = 0
    total_pixels = check_w * check_h
    
    for y in range(start_y, height):
        for x in range(start_x, width):
            r, g, b, a = pixels[x, y]
            # Check if this pixel is different from background
            color_diff = abs(r - bg_r) + abs(g - bg_g) + abs(b - bg_b)
            if color_diff > 30 and a > 128:  # Significant color difference and visible
                non_bg_count += 1
    
    # If more than 5% of the corner area has non-background pixels, likely watermark
    watermark_ratio = non_bg_count / max(total_pixels, 1)
    if watermark_ratio > 0.05:
        return (start_x, start_y, width, height)
    
    return None

def inpaint_region(img, region_box):
    """
    Inpaints a region using surrounding pixels (texture-aware filling).
    Uses a simple but effective method: blur surrounding pixels into the region.
    """
    x1, y1, x2, y2 = region_box
    width, height = img.size
    
    # Create a copy of the image as numpy array for easier manipulation
    img_array = np.array(img)
    
    # Get the region above and to the left of the watermark area
    sample_height = min(y2 - y1, 20)  # Sample from above
    sample_width = min(x2 - x1, 20)   # Sample from left
    
    # Sample pixels from just above the watermark region
    if y1 > sample_height:
        top_sample = img_array[y1 - sample_height:y1, x1:x2]
    else:
        top_sample = img_array[0:y1, x1:x2] if y1 > 0 else None
    
    # Sample pixels from just left of the watermark region  
    if x1 > sample_width:
        left_sample = img_array[y1:y2, x1 - sample_width:x1]
    else:
        left_sample = img_array[y1:y2, 0:x1] if x1 > 0 else None
    
    # Fill the watermark region by extending the pattern from above
    if top_sample is not None and top_sample.size > 0:
        # Tile the sample vertically to fill the region
        sample_h = top_sample.shape[0]
        region_h = y2 - y1
        region_w = x2 - x1
        
        if sample_h > 0:
            # Create fill pattern by tiling
            repeats = (region_h // sample_h) + 1
            tiled = np.tile(top_sample, (repeats, 1, 1))[:region_h, :region_w]
            
            # Apply to the region
            img_array[y1:y2, x1:x2] = tiled
    elif left_sample is not None and left_sample.size > 0:
        # Fall back to using left sample
        sample_w = left_sample.shape[1]
        region_h = y2 - y1
        region_w = x2 - x1
        
        if sample_w > 0:
            repeats = (region_w // sample_w) + 1
            tiled = np.tile(left_sample, (1, repeats, 1))[:region_h, :region_w]
            img_array[y1:y2, x1:x2] = tiled
    
    # Convert back to PIL and apply slight blur to blend edges
    result = Image.fromarray(img_array)
    
    # Create a mask for just the repaired region edges
    mask = Image.new('L', (width, height), 0)
    draw = ImageDraw.Draw(mask)
    # Draw feathered edge around the inpainted region
    edge_width = 3
    draw.rectangle([x1, y1, x2 - 1, y1 + edge_width], fill=128)  # Top edge
    draw.rectangle([x1, y1, x1 + edge_width, y2 - 1], fill=128)  # Left edge
    
    # Apply selective blur to blend
    blurred = result.filter(ImageFilter.GaussianBlur(radius=2))
    
    # Composite the blurred edges with the sharp result
    result = Image.composite(blurred, result, mask)
    
    return result

def smart_watermark_removal(img):
    """
    Intelligently detects and removes watermark using inpainting.
    Only acts if a watermark is detected.
    """
    watermark_box = detect_watermark_region(img)
    
    if watermark_box:
        print(f"  -> Watermark detected, applying smart inpaint...")
        return inpaint_region(img, watermark_box)
    else:
        print(f"  -> No watermark detected, skipping inpaint")
        return img

def remove_all_white_pixels(image_path, output_path):
    """
    1. Smart watermark removal (Inpainting if detected)
    2. Removes all white/light background pixels globally
    """
    img = Image.open(image_path)
    img = img.convert("RGBA")
    
    print(f"Processing: {os.path.basename(image_path)}")
    
    # Step 1: Smart watermark removal (Only if watermark detected)
    img = smart_watermark_removal(img)
    
    width, height = img.size
    pixels = img.load()
    
    removed_count = 0
    
    # Step 2: Remove white pixels
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if is_white_or_light(r, g, b):
                pixels[x, y] = (0, 0, 0, 0)
                removed_count += 1
    
    img.save(output_path, "PNG")
    print(f"  -> Done: removed {removed_count} bg pixels\n")

print("=" * 50)
print("Smart Asset Pipeline v4 - Intelligent Inpainting")
print("=" * 50)
print(f"Input:  {input_dir}")
print(f"Output: {output_dir}\n")

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

if not os.path.exists(input_dir):
    print(f"Warning: Input directory '{input_dir}' does not exist.")
else:
    files = [f for f in os.listdir(input_dir) if f.endswith(".png")]
    print(f"Found {len(files)} PNG files to process.\n")
    
    for filename in files:
        input_path = os.path.join(input_dir, filename)
        output_path = os.path.join(output_dir, filename)
        remove_all_white_pixels(input_path, output_path)

print("=" * 50)
print("Pipeline complete!")
print("=" * 50)
