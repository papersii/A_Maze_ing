import os
import sys
from PIL import Image

def generate_walls(top_path, body_path, output_dir, theme_name, visual_height_limit=None):
    """
    Generates wall assets for a given theme using a top texture and a body texture.
    Wall sizes generated: 2x2, 3x2, 2x3, 2x4, 4x2, 3x3, 4x4.
    
    Args:
        top_path (str): Path to the wall top texture (16x16 expected).
        body_path (str): Path to the wall body texture (16x16 expected).
        output_dir (str): Directory to save generated images.
        theme_name (str): Name of the theme (e.g., 'grassland').
        visual_height_limit (int, optional): If set, limits the number of vertical tiles drawn (rest transparent).
    """
    
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    try:
        top_img = Image.open(top_path).convert("RGBA")
        body_img = Image.open(body_path).convert("RGBA")
    except Exception as e:
        print(f"Error loading images: {e}")
        return

    # Standard sizes from TextureManager.java
    # { 2, 2 }, { 3, 2 }, { 2, 3 }, { 2, 4 }, { 4, 2 }, { 3, 3 }, { 4, 4 }
    sizes = [
        (2, 2), (3, 2), (2, 3), (2, 4), (4, 2), (3, 3), (4, 4)
    ]
    
    TILE_SIZE = 16
    
    # Check if we should use "Structure Mode" (Stretch/Fit) instead of tiling
    # This is useful for organic/large objects like Trees (Jungle) or Rocks
    is_structure_mode = theme_name.lower() in ["jungle", "forest"]

    for w_tiles, h_tiles in sizes:
        # Canvas size
        # Standard: (h+1)*16 to allow top cap overlap
        # Structure: Match grid size exactly? Or allow overshoot?
        # User said "Don't cut". If we fit to grid, we might distort aspect ratio.
        # But if we don't fit to grid, collision might be off visual.
        # Let's try fitting to grid (w*16, h*16) for Structure Mode to be safe for walls.
        # Actually, let's keep the (h+1) height for depth if needed, but for "Don't cut",
        # let's map the image to the full canvas.
        
        if is_structure_mode:
             # Structure Mode (Object Tiling):
             # User Request: "Larger trees, overlapping, exceed bounds"
             
             SCALE_FACTOR = 1.35  # Make tree ~35% larger
             TREE_SIZE = int(TILE_SIZE * SCALE_FACTOR) # ~21px
             
             # Increase canvas height to accommodate the "sticking out" top of the trees
             # (Tree Height - Tile Height) is the extra overlap needed at the top
             top_padding = max(0, TREE_SIZE - TILE_SIZE)
             
             img_w = w_tiles * TILE_SIZE
             img_h = (h_tiles * TILE_SIZE) + top_padding 
             
             canvas = Image.new("RGBA", (img_w, img_h))
             
             # Resize source 
             tile_img = body_img.resize((TREE_SIZE, TREE_SIZE), Image.LANCZOS)
             
             # Draw loop (Painter's Algo: Top row first, then lower rows overlap)
             for y in range(h_tiles):
                 for x in range(w_tiles):
                     # Center horizontally
                     dest_x = (x * TILE_SIZE) + (TILE_SIZE - TREE_SIZE) // 2
                     
                     # Align Bottom: The bottom of the tree should align with the bottom of the tile
                     # Tile Bottom Y relative to Grid = (y + 1) * TILE_SIZE
                     # Shifted by top_padding in canvas
                     # Canvas Tile Bottom = (y + 1) * TILE_SIZE + top_padding
                     # Tree Top Y = Canvas Tile Bottom - TREE_SIZE
                     dest_y = ((y + 1) * TILE_SIZE + top_padding) - TREE_SIZE
                     
                     # Add slight random offset to prevent perfect grid look? 
                     # User didn't strictly ask for random, but "overlapping" implies organic.
                     # Let's stick to strict grid first to ensure alignment, overlap comes from size > tile.
                     
                     canvas.paste(tile_img, (dest_x, dest_y), tile_img) # Use itself as mask for transparency
                     
        else:
            # Tiling Mode (Standard)
            img_w = w_tiles * TILE_SIZE
            img_h = (h_tiles + 1) * TILE_SIZE
            canvas = Image.new("RGBA", (img_w, img_h))

            # 1. Draw Top Row (y=0) - The "Cap" (SHIFTED DOWN by 8px)
            cap_y = TILE_SIZE // 2
            for x in range(w_tiles):
                canvas.paste(body_img, (x * TILE_SIZE, cap_y))
                canvas.paste(top_img, (x * TILE_SIZE, cap_y), top_img)

            # 2. Draw Body Rows (y from 1 to h_tiles) - SHIFTED UP by 8px
            for y in range(1, h_tiles + 1):
                dest_y = (y * TILE_SIZE) - (TILE_SIZE // 2)
                for x in range(w_tiles):
                    canvas.paste(body_img, (x * TILE_SIZE, dest_y))
                    canvas.paste(top_img, (x * TILE_SIZE, dest_y), top_img)
                    
            # 3. Fill Bottom Gap (8px)
            body_top_half = body_img.crop((0, 0, TILE_SIZE, TILE_SIZE // 2))
            filler_y = h_tiles * TILE_SIZE + (TILE_SIZE // 2)
            
            for x in range(w_tiles):
                canvas.paste(body_top_half, (x * TILE_SIZE, filler_y))

        # Save as v1
        filename_v1 = f"wall_{theme_name}_{w_tiles}x{h_tiles}_v1.png"
        save_path_v1 = os.path.join(output_dir, filename_v1)
        canvas.save(save_path_v1)
        print(f"Generated: {filename_v1}")
        
        # Save as v2 (Same as v1 for Structure Mode)
        filename_v2 = f"wall_{theme_name}_{w_tiles}x{h_tiles}_v2.png"
        save_path_v2 = os.path.join(output_dir, filename_v2)
        canvas.save(save_path_v2)
        print(f"Generated: {filename_v2}")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python generate_walls.py <top_image_path> <body_image_path> <output_dir> <theme_name> [visual_height_limit]")
        sys.exit(1)

    top_path = sys.argv[1]
    body_path = sys.argv[2]
    output_dir = sys.argv[3]
    theme_name = sys.argv[4]
    
    visual_height_limit = None
    if len(sys.argv) > 5:
        try:
            visual_height_limit = int(sys.argv[5])
        except ValueError:
            pass

    generate_walls(top_path, body_path, output_dir, theme_name, visual_height_limit)
