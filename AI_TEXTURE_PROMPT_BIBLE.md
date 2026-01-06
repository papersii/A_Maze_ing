# AI Art Direction & Meta-Prompt Bible
# AI 美术指导与元提示词圣经

> **Role**: World-Class Game Art Director (世界级游戏美术总监)
> **Goal**: Create unified, premium, high-readability assets for a top-down 2D game.
> **Style Target**: "Stylized Hand-Painted Fantasy" (风格化手绘奇幻) - precise, vibrant, and timeless.

---

## 1. The "Meta-Prompt" Core Logic (元提示词核心逻辑)

To ensure consistency, every prompt must be constructed using this **Modular Formula (模块化公式)**:

```text
[Subject Definition] + [Viewpoint & Layout] + [Composition & Padding] + [Art Style Pillars] + [Material & Lighting] + [Technical Enforcers] + [--no Negative Prompts]
```

### 1.1 Art Style Pillars (美术风格支柱 - Must Include in Every Prompt)
Use these keywords to lock the visual identity:
> **"Game Asset, Sprite Sheet Style, Hand-painted texture, Stylized Realism, Vibrant Color Palette, Clean Vector-like Lines, Thick Brushstrokes, Digital 2D Painting, High Fidelity, Blizzard/Riot Art Style, Defined Silhouette."**

### 1.2 Viewpoint & Layout (视角与布局)
- **General**: "Top-down Orthographic 90 degree view" (strictly flat for 2D grid games).
- **Walls**: "Top-down 3/4 perspective, showing top face and front face, modular design."
- **Floors**: "Direct top-down flat view, seamless texture pattern."

### 1.3 Composition & Padding (构图与留白 - CRITICAL)
**Prevent "Cut-off" is the #1 priority.**
- **Keywords**: "Isolated on White Background, Centered, Wide shot, Surrounded by empty space, Entire object visible, No cropping."
- **Logic**: Use a wider field of view (e.g. "Zoomed out view") to ensure the edges of the wall/prop never touch the canvas border.

---

## 2. Category-Specific Master Prompts (分类大师级提示词)

### 🧱 Type A: Walls & Obstacles (墙体与障碍)
**Design Goal**: Solid, heavy looking, clearly defined "Top" (walkable visual) and "Front" (blocking visual).
**Meta-Prompt Template**:
```
[Material] Wall Element for Top-Down RPG Game, Orthographic Projection. 
Single modular block, clearly defined Top Face (lighter) and Front Face (darker, shadowed). 
Style: Hand-painted stylized texture, chunky stone/wood/metal details, ambient occlusion in crevices. 
Composition: Isolated on Solid White Background, Centered, 20% transparent padding around object.
Sharp edges, no cast shadow on ground, game ready asset, fully visible.
```
**Example (Dungeon Wall)**:
> "Ancient Grey Stone Wall block, heavy granite texture with moss details. Top-down RPG game asset. Hand-painted stylized style, thick outlines. Clearly defined top surface and vertical front face. High contrast between top and side. Solid White Background. --no perspective slant, 3d render, photorealism"

### 🟦 Type B: Floor Tiles (地面贴图)
**Design Goal**: Low noise, high seamlessness, does not distract from player/enemies.
**Meta-Prompt Template**:
```
Seamless Tiling Texture of [Material], [Theme] Surface. flat top-down view.
Pattern: Repetitive but natural, avoiding obvious grid lines.
Style: Hand-painted blizzard style, soft diffuse lighting, low contrast details (to let charas pop).
Color Palette: [Theme Colors].
Full frame texture, no borders.
```
**Example (Lava Floor)**:
> "Seamless Molten Magma floor texture, crusted cooling black rock with glowing orange cracks. Hand-painted game texture. Flat top-down view. Low contrast, soft glow. High fidelity. --no artifacts, blurry, grid lines, watermarks"

### ⚔️ Type C: Props & Items (道具与物品)
**Design Goal**: Instantly readable silhouette, "Pop" functionality.
**Meta-Prompt Template**:
```
Single Game Sprite of [Item Name], [Theme] style. 
View: Top-down orthographic 45-degree angle (to show volume).
Style: Vibrant colors, magical glow (if applicable), thick bold outline, stylized digital painting.
Composition: On Solid White Background, Centered, Wide margin, Entire object within frame.
```
**Example (Treasure Chest)**:
> "Golden Royal Treasure Chest with jewels, closed. Top-down game sprite. Hand-painted stylized art. Shiny gold metal, rich wood texture. Magical aura. Solid White Background. --no cut off, multiple items, complex background"

### ✨ Type D: VFX & Particles (特效与粒子)
**Design Goal**: Dynamic, fluid, additive blending ready.
**Meta-Prompt Template**:
```
Game Visual Effect Sprite, [Effect Name], [Element] type.
Style: Cell-shaded anime FX, clean shapes, fluid motion blur stylistic.
On Solid Black Background (for screen blend) OR Solid Transparent Background.
High luminosity, saturation.
```
**Example (Explosion)**:
> "2D Cartoon Explosion effect, single frame keyart. Fire and Smoke clouds. Anime style blast. Vibrant Orange and Grey. Solid Black Background. --no dithering, noise"

---

## 3. The "Secret Sauce" Parameters (独家参数调优)

When using Midjourney/DALL-E/Stable Diffusion, apply these modifier weights:

- **Stylize**: High (e.g., `--s 250` in MJ) to push the "Artistic" feel over "Realism".
- **Quality**: Max.
- **Negative Prompts (Use these to filter garbage)**:
  > **--no** cropped, cut off, out of frame, partial, touching edges, close up, macro shot, 3d render, blender, photorealistic, noise, dusty, dirty, grainy, low res, jpeg artifacts, watermark, text, signature, perspective distortion, vanishing point, isometric grid lines, blurry edges.

---

## 4. Workflow for Consistency (一致性工作流)

1.  **Seed Locking**: If you generate a perfect "Grass Floor", keep its Seed/Style Reference ID. Use it to generate the "Grass Wall" to ensure color matching.
2.  **Palette Control**: Always append "Color Palette: [Hex Codes or Descriptive names]" if the AI drifts. e.g., "Color Palette: Emerald Green, Dark Slate Grey".
3.  **Downscaling Logic**: Generate at 1024x1024 or 512x512. Downscale to 64x64 using "Nearest Neighbor" (if pixel perfect) or "Bicubic Sharper" (if hand-painted) then apply a slight "Unsharp Mask" to pop details at low res.

---

## 5. Theme Specific Keywords (主题关键词库)

- **Grassland**: "Lush green, soft blades, small flowers, sunlit, gentle earth tones."
- **Desert**: "Scorched sand, sandstone, dry cracks, warm orange and yellow, sun-bleached bone."
- **Ice**: "Glacial blue, frost overlay, crystalline, hard edges, cold white and cyan, reflective."
- **Jungle**: "Deep overgrown green, vines, mossy ruins, humidity, tropical, dark shadows."
- **Space**: "Metallic panels, sci-fi vents, neon lights, dark chrome, industrial grey, blueprint blue."

---

---

## 6. Variant Strategy (变体生成策略)

When generating `v1`, `v2`, etc., **CHANGE THE ADJECTIVES**:
- **v1 (Standard)**: "Clean, regular, standard look."
- **v2 (Alternate)**: "Damaged, ancient, overgrown, or different brick pattern."
- **Constraint**: Keep the **Color Palette** and **Art Style** identical. Only change the internal structure/texture.

贴图的prompting，不仅要保证一张纹理贴图能加载套用到1*1的方格，还要确保能够套用到有5种类型的墙体（宽*高：2*2 大小的，2*3的，3*2的，2*4的，4*2的，3*3的，4*4的），注意非正方形的规格，需要对同一贴图提供横竖两种版本，从让AI生成设计之初就要考虑到这点，对于每一种规格的墙体，都要有两种不同的物体作为贴图，就比如说，3*2的横向墙体，在草原里，是倒塌的圆柱，另外一种3*2的横向墙体，在草原里则是其他的东西。墙体的贴图，要确保从游戏视角来看，拼接起来观感是正常的，不能不协调，比图说原始贴图的图案是斜着的墙。墙体部分贴图，并不是说一定得是墙，需要时5类不同适应主题风格（草原，荒漠，冰原，丛林，太空船）的物体，要有创意，有想象力。
---

---

## 7. Creative Material Diversity (不再只是石头!)

**CRITICAL**: Avoid repetitive materials. For every theme, explore **Sub-Materials** to tell a story.

| Theme | Common Material | **Creative Alternatives (Use these!)** |
|-------|-----------------|----------------------------------------|
| **Desert** | Sandstone | Giant Bones, Petrified Wood, Dried Giant Cactus, Crystal Outcrops, Buried Ancient Statues, Salt Pillars. |
| **Ice** | Ice Block | Frozen Machinery, Mammoth Fur Tents, Blue Fire Crystals, Obsidian Rock (User contrast). |
| **Jungle**| Trees/Vines | Aztec Gold Idols, Giant Flowers, Carnivorous Plants, Crashed Plane Parts. |
| **Dungeon**| Stone Brick | Iron Cages, Piles of Gold, Torture Devices, Magical Runes on Walls. |
| **Space** | Metal | Alien Eggs, Holographic Barriers, Broken Robots, Glowing Server Racks. |

**Rule**: When generating `v1` vs `v2`, try to change the **MATERIAL** entirely if possible, not just the shape.

---

## 8. 🚫 Avoiding "Sticker Effect" (避免贴纸感 - CRITICAL LESSON)

**Problem Diagnosis (问题诊断)**:
When walls look like independent objects **pasted onto a grid tile**, they appear fake and break immersion. This happens when:
- The object is **too "complete"** (e.g., a full gear mechanism, a standalone pillar).
- The object **fills the entire tile** edge-to-edge with no breathing room.
- There is no visual **integration with the ground** (no shadows, no base, no transition).

**Solution - The "Modular Structure" Approach (解决方案 - 模块化结构)**:

| ❌ BAD (Sticker Feel) | ✅ GOOD (Integrated Feel) |
|-----------------------|---------------------------|
| Complete standalone object | **Section/fragment** of a larger structure |
| Object fills entire tile | **20% padding** around object |
| No ground connection | **Base/debris/sand accumulation** at bottom |
| Uniform lighting | **Clear Top (lighter) + Front (darker)** |
| Perfect/intact condition | **Weathered, aged, partially damaged** |

**Prompt Keywords to Enforce (强制使用的关键词)**:
```
✅ INCLUDE: "Modular block", "Section of", "Fragment of", "Interconnected", 
            "Blends with environment", "Sand/grass/debris at base", 
            "Clearly defined Top Face and Front Face", "20% padding"

❌ AVOID:  "Isolated single object", "Complete [item]", "Full [structure]",
           "Centered perfectly", "Floating"
```

**Example Fix (修正案例)**:
- ❌ **Before**: "Ancient gear mechanism" → Looks like a steampunk sticker
- ✅ **After**: "Cluster of collapsed adobe mud brick towers, interconnected by crumbling walls, sand drift at base"

---

*This document serves as the single source of truth for all AI generation tasks. Adhere to these prompts to maintain World-Class quality.*
