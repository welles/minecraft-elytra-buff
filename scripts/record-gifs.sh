#!/usr/bin/env bash
# Plays the scenes of src/gametest in a headless Minecraft and turns them into GIFs in media/.
# Run it in the Nix shell, which provides xvfb-run and ffmpeg: nix-shell --run scripts/record-gifs.sh
set -euo pipefail
cd "$(dirname "$0")/.."

node=1.21.11-fabric
frames=versions/$node/build/run/clientGameTest/recordings
# CurseForge does not accept larger GIFs
max_bytes=2000000

# A virtual display with Mesa's software renderer, so no window opens; the game test clears its run directory first
LIBGL_ALWAYS_SOFTWARE=1 __GLX_VENDOR_LIBRARY_NAME=mesa xvfb-run -a -s '-screen 0 1280x720x24' \
    ./gradlew ":$node:runClientGameTest"

# gif FRAMES OUT FPS COLORS: 480 pixels wide, a palette of its own and no dithering keep the GIFs small
gif() {
    ffmpeg -loglevel error -y -framerate 20 -i "$1/%04d.png" \
        -vf "fps=$3,scale=480:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=$4:stats_mode=full[p];[b][p]paletteuse=dither=none:diff_mode=rectangle" \
        -loop 0 "$2"
}

mkdir -p media
for scene in "$frames"/*/; do
    name=$(basename "$scene")
    out=media/$name.gif
    # the best quality that fits: one frame per tick first, then fewer frames and colors
    for setting in "20 128" "15 128" "15 96" "12 96" "12 64" "10 64"; do
        read -r fps colors <<<"$setting"
        gif "$scene" "$out" "$fps" "$colors"
        size=$(stat -c %s "$out")
        [ "$size" -le "$max_bytes" ] && break
    done
    if [ "$size" -gt "$max_bytes" ]; then
        echo "$out: $size bytes, more than $max_bytes even at $fps fps and $colors colors; shorten the scene" >&2
        exit 1
    fi
    echo "$out: $size bytes at $fps fps and $colors colors"
done
