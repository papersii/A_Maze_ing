# Desert Wall Generation Guide (荒漠墙体生成指南)

Please use the following prompts to generate the assets.
**Note**: `v1` is the "Standard Sandstone" (Clean/Layered), and `v2` is the "Ancient Brick" (Constructed/Ruins) style. Both share the same color palette.

## Color Palette Reference
**Colors**: Scorched Sand (Orange-Yellow), Reddish-Brown clay shadows, Bleached Bone highlights.

## Asset Generation Table

| Filename | Size & Variant | Visual Description | Exact AI Prompt (Copy Paste) |
| :--- | :--- | :--- | :--- |
| **`wall_desert_2x2_v1.png`** | 2x2 Standard | Natural eroded sandstone block, wind-swept horizontal layers. | `Sandstone Wall Element, 2x2 block. Top-Down RPG. Style: Hand-painted stylized, natural wind-eroded rock, horizontal sedimentary layers. Warm orange/yellow. Isolated on White Background. --no bricks, construction, geometry` |
| **`wall_desert_2x2_v2.png`** | 2x2 Ancient | Constructed sandstone bricks, slightly crumbling, ancient ruins feel. | `Ancient Sandstone Brick Wall, 2x2 block. Top-Down RPG. Style: Hand-painted stylized, square cut stones, crumbling ruins, heavy cracks. Warm orange/yellow. Isolated on White Background. --no natural rock, smooth surface` |
| **`wall_desert_3x2_v1.png`** | 3x2 Standard | Wide natural sandstone formation, smooth top. | `Wide Sandstone Wall Element, 3x2 block. Top-Down RPG. Style: Hand-painted stylized, natural wind-eroded rock, smooth weathered top face. Warm orange/yellow. Isolated on White Background. --no bricks` |
| **`wall_desert_3x2_v2.png`** | 3x2 Ancient | Wide brick wall section, some missing bricks, ancient look. | `Wide Ancient Brick Wall, 3x2 block. Top-Down RPG. Style: Hand-painted stylized, heavy masonry work, some missing bricks, shadow depth. Warm orange/yellow. Isolated on White Background. --no natural rock` |
| **`wall_desert_4x2_v1.png`** | 4x2 Standard | Long natural ridge, dune-like coloring. | `Long Sandstone Ridge Wall, 4x2 block. Top-Down RPG. Style: Hand-painted stylized, wavy wind patterns, organic shape. Warm orange/yellow. Isolated on White Background.` |
| **`wall_desert_4x2_v2.png`** | 4x2 Ancient | Long fortress wall, reinforced base, heavy stone. | `Long Ancient Fortress Wall, 4x2 block. Top-Down RPG. Style: Hand-painted stylized, large heavy megalithic blocks, sturdy construction. Warm orange/yellow. Isolated on White Background.` |
| **`wall_desert_3x3_v1.png`** | 3x3 Standard | Large cubic rock formation, solid and heavy. | `Large Cubic Sandstone finish, 3x3 block. Top-Down RPG. Style: Hand-painted stylized, heavy boulder look, solid mass. Warm orange/yellow. Isolated on White Background.` |
| **`wall_desert_3x3_v2.png`** | 3x3 Ancient | Ruined temple corner block, hieroglyphic hints (subtle). | `Ancient Temple Block Wall, 3x3 block. Top-Down RPG. Style: Hand-painted stylized, carved stone texture, subtle geometric patterns, worn edges. Warm orange/yellow. Isolated on White Background.` |
| **`wall_desert_4x4_v1.png`** | 4x4 Standard | Massive plateau chunk, flat top. | `Massive Sandstone Plateau Wall, 4x4 block. Top-Down RPG. Style: Hand-painted stylized, flat top mesa look, steep sides. Warm orange/yellow. Isolated on White Background.` |
| **`wall_desert_4x4_v2.png`** | 4x4 Ancient | Massive pyramid/monument base, stepped structure. | `Massive Ancient Monument Wall, 4x4 block. Top-Down RPG. Style: Hand-painted stylized, stepped stone structure, weathered pyramid base. Warm orange/yellow. Isolated on White Background.` |

### Global Settings (Universal)
Add these to ALL prompts if your tool supports it, or ensure the AI knows this context:
> **Aspect Ratio**: Square (1:1) is fine, or slightly vertical for taller walls.
> **Negative Prompts**: `--no cropped, cut off, partial, touching edges, 3d render, photorealistic, noise, dark, night, water, green vegetation`
