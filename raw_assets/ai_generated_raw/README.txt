# AI Generated Raw Assets Instructions
# AI生成原始素材说明

## Purpose (用途)
This folder is the entry point for all AI-generated texture assets.
此文件夹是所有AI生成纹理素材的入口点。

## Rules (规则)
1. **One Entity Per File (一物一图)**:
   - Every PNG file must contain ONLY ONE material or entity.
   - Do NOT combine multiple items into a single sprite sheet here.
   - 每个PNG文件只能包含一个素材或实体。不要在这里将多个物品拼接到一张图中。

2. **Naming Convention (命名规范)**:
   - Use snake_case: `entity_name_variant.png`
   - Example: `fire_dragon_idle.png`, `stone_wall_cracked.png`

3. **Metadata (元数据)**:
   - Ideally, keep a record of the prompt used for generation in a sidecar text file or a log if possible, to aid in reproducing the style.

## Workflow (工作流)
1. Generate image using AI.
2. Save raw output here.
3. Proceed to `../ai_processed_transparent` for background removal.
