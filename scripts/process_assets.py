from PIL import Image
import os

input_dir = "raw_assets/ai_generated_raw"
output_dir = "raw_assets/ai_processed_transparent"

def remove_white_background(image_path, output_path):
    img = Image.open(image_path)
    img = img.convert("RGBA")
    datas = img.getdata()
    
    new_data = []
    for item in datas:
        # Check for white (allow small variance)
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append(item)
            
    img.putdata(new_data)
    img.save(output_path, "PNG")
    print(f"Processed: {output_path}")

print("Starting background removal...")
for filename in os.listdir(input_dir):
    if filename.endswith(".png"):
        input_path = os.path.join(input_dir, filename)
        output_path = os.path.join(output_dir, filename)
        remove_white_background(input_path, output_path)
print("Done.")
