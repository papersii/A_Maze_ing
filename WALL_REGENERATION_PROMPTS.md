# Wall Texture Regeneration Prompts
# 墙体纹理重新生成提示词表

> **问题诊断**: 之前的纹理失败原因是它们看起来像"贴纸"——一个完整的独立物体被放置在方格内。正确的做法是生成**建筑结构的截面/模块**，而非独立物品。

---

## 待生成素材清单

| Filename | Size (W×H) | Visual Description | Exact AI Prompt (Copy-Paste Ready) |
|----------|------------|--------------------|------------------------------------|
| `wall_desert_2x4_v1.png` | 2×4 (128×288px) | 沙漠盐柱塔 (Salt Pillar Tower) | Desert Salt Crystal Pillar for Top-Down RPG Game, Orthographic 3/4 Projection. Tall vertical modular block (narrow width, tall height). Style: Sun-bleached white salt crystals with sandstone patches, subtle amber mineral veins. Clearly defined Top Surface (lighter, flat crystal formation) and Front Vertical Face (darker, layered salt deposits with wind erosion cracks). Hand-painted stylized texture, chunky geometric crystal facets. Composition: Isolated on Solid White Background, Centered, 20% transparent padding around object, entire object visible. No cast shadow, game ready asset. Color Palette: Ivory White, Dusty Amber, Warm Sand Yellow. --no floating, cut off, perspective slant, 3d render, photorealism, isometric grid |
| `wall_desert_2x4_v2.png` | 2×4 (128×288px) | 沙漠风化古柱 (Weathered Ancient Obelisk) | Weathered Desert Obelisk for Top-Down RPG Game, Orthographic 3/4 Projection. Tall vertical sandstone pillar with hieroglyphic carvings partially buried in sand. Style: Orange-tan sandstone with deep wind erosion grooves, faded carved symbols on front face, sand drift at base. Clearly defined Top Surface (flat, cracked stone cap) and Front Face (vertical, textured carvings). Hand-painted stylized texture, thick strokes. Composition: Isolated on Solid White Background, Centered, 20% padding, not touching canvas edges. No ground shadow. Color Palette: Burnt Orange, Dusty Tan, Sun-bleached Bone. --no modern elements, 3d render, cut off, floating |
| `wall_desert_4x4_v1.png` | 4×4 (256×288px) | 巨兽遗骨 (Giant Beast Ribcage) | Massive Bleached Beast Ribcage Half-Buried in Sand, Top-Down RPG Game Asset, Orthographic 3/4 View. Wide rectangular formation of curved ancient bones forming a natural barrier wall. Style: Sun-bleached ivory bones connected by dried sinew, sand accumulation at base, scattered smaller bones. Clearly defined Top (bone ridge visible from above) and Front (curved rib arches blocking path). Hand-painted stylized texture, organic curves, desert palette. Composition: Isolated on Solid White Background, Centered, 20% padding, fully visible, no cropping. Color Palette: Ivory White, Pale Gold Sand, Warm Tan. --no flesh, modern, cut off, 3d render, isolated single bone |
| `wall_desert_4x4_v2.png` | 4×4 (256×288px) | 干裂泥塔废墟群 (Cracked Mud Tower Ruins) | Cluster of Collapsed Adobe Mud Brick Towers, Desert Ruins, Top-Down RPG Game Asset, Orthographic 3/4 View. Wide rectangular wall formation with multiple broken cylindrical towers, interconnected by crumbling walls. Style: Deep crack patterns on dried clay, visible brick layers, orange-brown earth tones. Clearly defined Top (broken rooftop platforms) and Front (damaged facade with dark window holes). Hand-painted stylized, thick outlines. Composition: Isolated on Solid White Background, Centered, 20% padding, entire structure visible. Color Palette: Terracotta Orange, Burnt Sienna, Dusty Gold. --no modern, intact structure, 3d render, cut off |
| `wall_grassland_3x2_v1.png` | 3×2 (192×160px) | 苔藓巨石堆 (Mossy Boulder Cluster) | Cluster of Stacked Natural Boulders with Moss, Grassland RPG Wall Element, Orthographic 3/4 View. Wide horizontal barrier made of irregular grey stones stacked naturally, green moss patches on top and in crevices. Style: Hand-painted stylized rock texture, visible stone grain, lush grass tufts sprouting from cracks. Clearly defined Top (mossy flat surfaces catching light) and Front (shadowed rough stone face). Composition: Isolated on Solid White Background, Centered, 20% padding, no cropping. Modular, blends with outdoor environment. Color Palette: Slate Grey, Forest Green, Earthy Brown. --no cut off, 3d render, uniform perfect stones, floating |
| `wall_grassland_3x2_v2.png` | 3×2 (192×160px) | 藤蔓缠绕木栅栏 (Vine-Covered Wooden Fence Section) | Rustic Wooden Fence Section Overgrown with Vines, Grassland RPG Game Asset, Orthographic 3/4 View. Wide horizontal wooden barrier, three weathered log posts connected by horizontal planks, covered in green leafy vines and wildflowers. Style: Hand-painted stylized wood grain, natural imperfection, lush vegetation. Clearly defined Top (ivy covering post tops) and Front (planks with climbing vines). Composition: Isolated on Solid White Background, Centered, 20% padding, full structure visible. Modular, connects naturally. Color Palette: Warm Wood Brown, Leaf Green, Soft Yellow Flowers. --no modern, cut off, 3d render, perfect symmetry |
| `wall_grassland_4x2_v1.png` | 4×2 (256×160px) | 倒塌圆柱神殿遗迹 (Fallen Temple Column Ruins) | Fallen Ancient Stone Columns with Overgrown Grass, Grassland Ruins RPG Game Asset, Orthographic 3/4 View. Wide horizontal wall formation of two toppled Doric columns lying across, moss and grass growing around. Style: Hand-painted weathered white marble with grey age stains, cracked stone, lush grass tufts at base. Clearly defined Top (column surfaces with moss patches, lit) and Front (rounded column body with shadows in crevices). Composition: Isolated on Solid White Background, Centered, 20% padding, not touching edges. Modular design. Color Palette: Off-white Marble, Stone Grey, Grass Green. --no standing upright, modern, cut off, 3d render, single column |
| `wall_grassland_4x2_v2.png` | 4×2 (256×160px) | 长满苔藓的矮石墙 (Mossy Low Stone Wall) | Ancient Low Stone Wall Covered in Moss, Grassland RPG Game Asset, Orthographic 3/4 View. Wide horizontal wall made of irregular stacked stones, thick moss carpet on top, grass growing at base. Style: Hand-painted stylized stacked field stones (grey-brown), lush green moss blanket on roof, weathered and aged. Clearly defined Top (flat mossy surface, well-lit) and Front (darker stone face with gaps between rocks). Natural, blends with meadow. Composition: Isolated on Solid White Background, Centered, 20% padding, modular and tileable. Color Palette: Stone Grey-Brown, Vibrant Moss Green, Earthy Base. --no modern, uniform bricks, cut off, 3d render |

---

## 使用说明

1. **复制对应的 Prompt**，粘贴到外部 AI 工具 (Midjourney/DALL-E/Stable Diffusion)
2. **文件命名**：生成后按表格中的 `Filename` 命名
3. **保存位置**：放入 `raw_assets/ai_generated_raw/`
4. **告知我**: 说 **"素材已放入"**

---

## 关键改进点

| 之前的问题 | 现在的改进 |
|-----------|-----------|
| 独立物体贴在方格上 | 模块化建筑结构截面 |
| 像"贴纸" | 明确的顶面+立面分层 |
| 风格不协调 | 使用主题配色板 |
| 缺乏层次感 | 强调明暗对比 (Top lighter, Front darker) |
