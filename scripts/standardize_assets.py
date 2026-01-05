from PIL import Image
import os

input_dir = "raw_assets/ai_processed_transparent"
output_dir = "raw_assets/ai_ready_optimized"
# Increased resolution to preserve detail (was 16x16, now 64x64)
TARGET_SIZE = (64, 64) 

def standardize_file(filename):
    input_path = os.path.join(input_dir, filename)
    output_path = os.path.join(output_dir, filename)

    try:
        img = Image.open(input_path).convert("RGBA")
        
        # 1. Trim transparent borders
        bbox = img.getbbox()
        if bbox:
            img_cropped = img.crop(bbox)
        else:
            print(f"Skipping empty image: {filename}")
            return

        # 2. Determine Scale Logic
        # Items should maintain aspect ratio and fit inside (e.g. 14x14)
        # Tiles should likely fill the space (16x16)
        
        new_img = Image.new("RGBA", TARGET_SIZE, (0, 0, 0, 0))
        
        if filename.startswith("tile_") or filename.startswith("wall_"):
            # Tiles & Walls: Resize to fill exactly
            img_resized = img_cropped.resize(TARGET_SIZE, Image.LANCZOS)
            new_img.paste(img_resized, (0, 0))
            
        else:
            # Items/Traps/UI: Fit maintain aspect ratio
            width, height = img_cropped.size
            if width > height:
                ratio = TARGET_SIZE[0] / width
            else:
                ratio = TARGET_SIZE[1] / height
            
            new_size = (int(width * ratio), int(height * ratio))
            # Use LANCZOS for high quality downsampling
            img_resized = img_cropped.resize(new_size, Image.LANCZOS)
            
            # Center it
            paste_x = (TARGET_SIZE[0] - new_size[0]) // 2
            paste_y = (TARGET_SIZE[1] - new_size[1]) // 2
            new_img.paste(img_resized, (paste_x, paste_y))

        new_img.save(output_path)
        print(f"Standardized: {filename} -> {TARGET_SIZE}")
        
    except Exception as e:
        print(f"Error processing {filename}: {e}")

print("Starting asset standardization...")
if not os.path.exists(output_dir):
    os.makedirs(output_dir)

for filename in os.listdir(input_dir):
    if filename.endswith(".png"):
        standardize_file(filename)
print("Done.")
