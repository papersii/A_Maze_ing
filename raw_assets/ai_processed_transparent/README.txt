# AI Processed Transparent Assets Instructions
# AI处理后透明素材说明

## Purpose (用途)
This folder contains assets that have been processed from `../ai_generated_raw` to remove backgrounds.
此文件夹存放从 `../ai_generated_raw` 处理过、已去除背景的素材。

## Rules (规则)
1. **One Entity Per File (一物一图)**:
   - Maintain the "one entity per PNG" rule.
   - Do NOT pack into atlases yet.
   - 保持“一物一图”的规则。还不要打包成图集。

2. **Transparency (透明化)**:
   - All images here must have a transparent background.
   - Ensure edges are clean (no artifacts from the original background).
   - 所有图片必须是透明背景。确保边缘干净（没有原背景的残留）。

## Setup Guide for Context-less AI (给无上下文AI的指引)
If you are an AI Agent reading this without prior context:
1. **Input**: Take files from `../ai_generated_raw`.
2. **Process**: Use a script or tool to remove the background.
3. **Output**: Save the clean PNG here.
4. **Next Step**: These files will eventually be stitched into a texture atlas in the final `assets/` folder by the game engine or a texture packer tool.
