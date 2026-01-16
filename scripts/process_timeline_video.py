#!/usr/bin/env python3
"""
Timeline Video Processing Pipeline
时间线视频处理流水线

This script processes animation videos that contain multiple animation states
arranged sequentially in time. It extracts frames and audio based on timeline
configuration.

Unlike grid-based processing, this script cuts video segments based on time
timestamps, not spatial grid layout.

Usage:
    # Basic usage with timeline config
    python3 scripts/process_timeline_video.py \
        --input raw_assets/videos/boar.mp4 \
        --config "walk_down:0-1,walk_right:1-2,attack:2-3" \
        --frames-per-segment 4 \
        --name mob_boar_grassland
    
    # With audio extraction
    python3 scripts/process_timeline_video.py \
        --input raw_assets/videos/boar.mp4 \
        --config "walk_down:0-1,walk_right:1-2" \
        --frames-per-segment 4 \
        --name mob_boar_grassland \
        --extract-audio
    
    # Static entity (single segment)
    python3 scripts/process_timeline_video.py \
        --input raw_assets/videos/fire_trap.mp4 \
        --config "idle:0-1" \
        --frames-per-segment 4 \
        --name trap_fire_desert
"""

import cv2
import os
import sys
import argparse
import subprocess
from PIL import Image
import numpy as np
from pathlib import Path

# Configuration
DEFAULT_FRAME_SIZE = 64
OUTPUT_DIR = "raw_assets/ai_ready_optimized"
FINAL_ANIM_DIR = "assets/images/animations"
FINAL_MOB_DIR = "assets/images/mobs"
AUDIO_OUTPUT_DIR = "assets/audio/sfx/entities"

# Background removal settings
BG_THRESHOLD = 200


def ensure_dirs():
    """Create necessary directories if they don't exist."""
    for d in [OUTPUT_DIR, FINAL_ANIM_DIR, FINAL_MOB_DIR, AUDIO_OUTPUT_DIR]:
        os.makedirs(d, exist_ok=True)


def parse_timeline_config(config_str):
    """
    Parse timeline configuration string.
    
    Format: "segment_name:start-end,segment_name:start-end,..."
    Example: "walk_down:0-1,walk_right:1-2,attack:2-3"
    
    Returns: List of (segment_name, start_time, end_time) tuples
    """
    segments = []
    for item in config_str.split(','):
        item = item.strip()
        if ':' not in item or '-' not in item:
            print(f"Warning: Invalid segment format: {item}")
            continue
        
        name, time_range = item.split(':')
        start, end = time_range.split('-')
        
        try:
            start_time = float(start)
            end_time = float(end)
            segments.append((name.strip(), start_time, end_time))
        except ValueError:
            print(f"Warning: Invalid time values in: {item}")
            continue
    
    return segments


def is_white_or_light(r, g, b, threshold=BG_THRESHOLD):
    """Check if a pixel is considered white/light background."""
    c_max = max(r, g, b)
    c_min = min(r, g, b)
    delta = c_max - c_min
    
    saturation = 0
    if c_max > 0:
        saturation = delta / c_max
    
    is_grey_white = saturation < 0.15 and c_max > threshold
    is_checkerboard_grey = saturation < 0.1 and 180 < c_max < 230 and abs(r - g) < 15 and abs(g - b) < 15
    
    return is_grey_white or is_checkerboard_grey


def remove_background(img):
    """Remove white/light background from image."""
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if is_white_or_light(r, g, b):
                pixels[x, y] = (0, 0, 0, 0)
    
    return img


def crop_to_content(img, padding_pct=0.02):
    """Crop image to content with minimal padding."""
    bbox = img.getbbox()
    if not bbox:
        return img
    
    x1, y1, x2, y2 = bbox
    width, height = img.size
    pad = int(min(x2 - x1, y2 - y1) * padding_pct)
    
    x1 = max(0, x1 - pad)
    y1 = max(0, y1 - pad)
    x2 = min(width, x2 + pad)
    y2 = min(height, y2 + pad)
    
    return img.crop((x1, y1, x2, y2))


def standardize_frame(img, target_size):
    """Resize and center frame to target size."""
    img = crop_to_content(img)
    
    if img.size[0] == 0 or img.size[1] == 0:
        return Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    
    scale = min(target_size / img.size[0], target_size / img.size[1])
    new_w = int(img.size[0] * scale)
    new_h = int(img.size[1] * scale)
    
    if new_w <= 0 or new_h <= 0:
        return Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    
    resized = img.resize((new_w, new_h), Image.LANCZOS)
    
    final = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
    paste_x = (target_size - new_w) // 2
    paste_y = (target_size - new_h) // 2
    final.paste(resized, (paste_x, paste_y))
    
    return final


def extract_frames_from_segment(video_path, start_time, end_time, frame_count):
    """
    Extract evenly spaced frames from a video segment.
    
    Args:
        video_path: Path to video file
        start_time: Start time in seconds
        end_time: End time in seconds
        frame_count: Number of frames to extract
    
    Returns:
        List of PIL Images
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"Error: Cannot open video: {video_path}")
        return []
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps <= 0:
        fps = 30  # Default fallback
    
    start_frame = int(start_time * fps)
    end_frame = int(end_time * fps)
    segment_frames = max(1, end_frame - start_frame)
    
    if segment_frames < frame_count:
        print(f"Warning: Segment has {segment_frames} frames but need {frame_count}")
        frame_count = segment_frames
    
    step = segment_frames / frame_count
    extracted = []
    
    for i in range(frame_count):
        frame_idx = start_frame + int(i * step)
        cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = cap.read()
        
        if ret:
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGBA)
            img = Image.fromarray(frame_rgb)
            extracted.append(img)
        else:
            print(f"Warning: Could not read frame at index {frame_idx}")
    
    cap.release()
    return extracted


def extract_audio_segment(video_path, start_time, end_time, output_path):
    """
    Extract audio segment from video using ffmpeg.
    
    Args:
        video_path: Path to video file
        start_time: Start time in seconds
        end_time: End time in seconds
        output_path: Output audio file path (.ogg)
    """
    duration = end_time - start_time
    
    cmd = [
        'ffmpeg', '-y',
        '-i', video_path,
        '-ss', str(start_time),
        '-t', str(duration),
        '-vn',  # No video
        '-acodec', 'libvorbis',
        '-q:a', '4',  # Quality
        output_path
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode == 0:
            return True
        else:
            print(f"Warning: ffmpeg error: {result.stderr}")
            return False
    except FileNotFoundError:
        print("Warning: ffmpeg not found, cannot extract audio")
        return False


def process_timeline_video(video_path, segments, frames_per_segment, name, target_size, extract_audio):
    """
    Process a timeline-based video into sprite sheets and audio clips.
    
    Args:
        video_path: Path to video file
        segments: List of (segment_name, start_time, end_time) tuples
        frames_per_segment: Number of frames per segment
        name: Base name for output files
        target_size: Target frame size in pixels
        extract_audio: Whether to extract audio for each segment
    
    Returns:
        List of generated files
    """
    print(f"\nProcessing video: {video_path}")
    print(f"Segments: {len(segments)}")
    print(f"Frames per segment: {frames_per_segment}")
    
    generated_files = []
    all_frames_by_segment = {}
    
    for seg_name, start_time, end_time in segments:
        print(f"\n  [{seg_name}] {start_time}s - {end_time}s")
        
        # Extract frames
        frames = extract_frames_from_segment(video_path, start_time, end_time, frames_per_segment)
        
        if not frames:
            print(f"    Warning: No frames extracted for {seg_name}")
            continue
        
        print(f"    Extracted {len(frames)} frames")
        
        # Process each frame
        processed_frames = []
        for i, frame in enumerate(frames):
            frame = remove_background(frame)
            frame = standardize_frame(frame, target_size)
            processed_frames.append(frame)
        
        all_frames_by_segment[seg_name] = processed_frames
        
        # Create horizontal strip for this segment
        strip_width = target_size * len(processed_frames)
        strip_height = target_size
        strip = Image.new("RGBA", (strip_width, strip_height), (0, 0, 0, 0))
        
        for i, frame in enumerate(processed_frames):
            strip.paste(frame, (i * target_size, 0))
        
        # Save strip
        output_name = f"{name}_{seg_name}_{len(processed_frames)}f.png"
        output_path = os.path.join(OUTPUT_DIR, output_name)
        strip.save(output_path)
        print(f"    ✅ Saved: {output_path}")
        generated_files.append(('anim', seg_name, output_name))
        
        # Extract audio if requested
        if extract_audio:
            audio_name = f"{name}_{seg_name}.ogg"
            audio_path = os.path.join(AUDIO_OUTPUT_DIR, audio_name)
            if extract_audio_segment(video_path, start_time, end_time, audio_path):
                print(f"    ✅ Audio: {audio_path}")
                generated_files.append(('audio', seg_name, audio_name))
    
    # Create combined sprite sheet if multiple segments
    if len(all_frames_by_segment) > 1:
        print(f"\nCreating combined sprite sheet...")
        
        # Calculate dimensions
        num_rows = len(all_frames_by_segment)
        num_cols = frames_per_segment
        combined = Image.new("RGBA", (num_cols * target_size, num_rows * target_size), (0, 0, 0, 0))
        
        for row_idx, (seg_name, frames) in enumerate(all_frames_by_segment.items()):
            for col_idx, frame in enumerate(frames):
                combined.paste(frame, (col_idx * target_size, row_idx * target_size))
        
        combined_name = f"{name}_spritesheet.png"
        combined_path = os.path.join(OUTPUT_DIR, combined_name)
        combined.save(combined_path)
        print(f"  ✅ Combined: {combined_path}")
        generated_files.append(('combined', 'all', combined_name))
    
    return generated_files


def main():
    parser = argparse.ArgumentParser(
        description='Process timeline-based animation videos',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('--input', '-i', required=True,
                        help='Input video file')
    parser.add_argument('--config', '-c', required=True,
                        help='Timeline config: "name:start-end,name:start-end,..."')
    parser.add_argument('--frames-per-segment', '-f', type=int, default=4,
                        help='Number of frames per segment (default: 4)')
    parser.add_argument('--name', '-n', required=True,
                        help='Base name for output files')
    parser.add_argument('--size', '-s', type=int, default=DEFAULT_FRAME_SIZE,
                        help=f'Target frame size (default: {DEFAULT_FRAME_SIZE})')
    parser.add_argument('--extract-audio', '-a', action='store_true',
                        help='Extract audio for each segment')
    parser.add_argument('--no-copy', action='store_true',
                        help='Do not copy to final asset directories')
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Timeline Video Processing Pipeline v1.0")
    print("=" * 60)
    print(f"Input: {args.input}")
    print(f"Config: {args.config}")
    print(f"Frames per segment: {args.frames_per_segment}")
    print(f"Output name: {args.name}")
    print(f"Frame size: {args.size}×{args.size}")
    print(f"Extract audio: {args.extract_audio}")
    print("=" * 60)
    
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}")
        return 1
    
    ensure_dirs()
    
    # Parse timeline config
    segments = parse_timeline_config(args.config)
    
    if not segments:
        print("Error: No valid segments in config")
        return 1
    
    print(f"\nParsed {len(segments)} segments:")
    for name, start, end in segments:
        print(f"  - {name}: {start}s - {end}s")
    
    # Process video
    generated = process_timeline_video(
        args.input, segments, args.frames_per_segment,
        args.name, args.size, args.extract_audio
    )
    
    # Copy to asset directories
    if not args.no_copy:
        import shutil
        print(f"\nCopying to asset directories...")
        
        # Determine if this is a mob or static entity
        is_mob = any(seg[0] in ['walk_down', 'walk_right', 'walk_up', 'walk_left', 'move'] 
                     for seg in segments)
        target_dir = FINAL_MOB_DIR if is_mob else FINAL_ANIM_DIR
        
        for file_type, seg_name, filename in generated:
            if file_type == 'anim' or file_type == 'combined':
                src = os.path.join(OUTPUT_DIR, filename)
                dst = os.path.join(target_dir, filename)
                shutil.copy(src, dst)
                print(f"  ✅ {filename} → {target_dir}/")
    
    print("\n" + "=" * 60)
    print("Processing complete!")
    print("=" * 60)
    
    # Print summary
    print("\n### Generated Files ###")
    
    anim_files = [(t, n, f) for t, n, f in generated if t in ['anim', 'combined']]
    audio_files = [(t, n, f) for t, n, f in generated if t == 'audio']
    
    if anim_files:
        print("\n**Animation Strips:**")
        for _, seg_name, filename in anim_files:
            print(f"  - `{filename}` ({seg_name})")
    
    if audio_files:
        print("\n**Audio Files:**")
        for _, seg_name, filename in audio_files:
            print(f"  - `{filename}` ({seg_name})")
    
    # Print Java integration code
    print("\n### Java Integration Code ###")
    print(f"""
// Load animations from individual strips
Map<String, Animation<TextureRegion>> animations = new HashMap<>();
String[] segments = {{{', '.join(f'"{s[0]}"' for s in segments)}}};

for (String seg : segments) {{
    String path = "images/mobs/{args.name}_" + seg + "_{args.frames_per_segment}f.png";
    Animation<TextureRegion> anim = loadAnimatedSprite(path, {args.frames_per_segment}, 0.15f);
    animations.put(seg, anim);
}}

// Load sound effects
Map<String, Sound> sounds = new HashMap<>();
for (String seg : segments) {{
    String path = "audio/sfx/entities/{args.name}_" + seg + ".ogg";
    if (Gdx.files.internal(path).exists()) {{
        sounds.put(seg, Gdx.audio.newSound(Gdx.files.internal(path)));
    }}
}}
""")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
