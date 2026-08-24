#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repository_root=$(cd -- "$script_dir/../.." && pwd)

version_dir=${GLIDE_VERSION_DIR:-}
game_dir=${GLIDE_GAME_DIR:-}
java_bin=${GLIDE_JAVA:-}
world_name=${GLIDE_SMOKE_WORLD:-New World}
startup_timeout=${GLIDE_SMOKE_STARTUP_TIMEOUT:-240}
shutdown_timeout=${GLIDE_SMOKE_SHUTDOWN_TIMEOUT:-120}
capture_screenshot=1
require_optifine=0

usage() {
    echo "Usage: $0 --version-dir <dir> --game-dir <dir> [options]" >&2
    echo "Options: --java <java8> --world <save-folder> --no-screenshot --require-optifine" >&2
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --version-dir) version_dir=$2; shift 2 ;;
        --game-dir) game_dir=$2; shift 2 ;;
        --java) java_bin=$2; shift 2 ;;
        --world) world_name=$2; shift 2 ;;
        --no-screenshot) capture_screenshot=0; shift ;;
        --require-optifine) require_optifine=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
    esac
done

if [[ -z "$version_dir" || -z "$game_dir" ]]; then
    usage
    exit 2
fi

version_dir=$(realpath "$version_dir")
game_dir=$(realpath "$game_dir")
minecraft_root=$(realpath "$version_dir/../..")
version_json="$version_dir/GlideClient-Forge.json"
client_jar="$version_dir/GlideClient-Forge.jar"
libraries_dir="$minecraft_root/libraries"
assets_dir="$minecraft_root/assets"
natives_dir="$version_dir/natives-linux-x86_64"
log_file="$game_dir/glide-release-smoke.log"
artifact_dir="$game_dir/test-artifacts"
screenshot="$artifact_dir/glide-release-smoke.png"

if [[ -z "$java_bin" ]]; then
    bundled_java="$HOME/.cache/glideclient-forge/toolchains/jdk8/bin/java"
    if [[ -x "$bundled_java" ]]; then
        java_bin=$bundled_java
    else
        java_bin=$(command -v java || true)
    fi
fi

shopt -s nullglob
production_candidates=("$repository_root"/build/libs/GlideClient-Forge-*.jar)
smoke_candidates=("$repository_root"/build/libs/GlideClient-Forge-Smoke-*.jar)
filtered_production=()
for candidate in "${production_candidates[@]}"; do
    if [[ $(basename "$candidate") != GlideClient-Forge-Smoke-* ]]; then
        filtered_production+=("$candidate")
    fi
done
if [[ ${#filtered_production[@]} -ne 1 || ${#smoke_candidates[@]} -ne 1 ]]; then
    echo "Expected exactly one production JAR and one smoke JAR in build/libs" >&2
    exit 1
fi
production_jar=${filtered_production[0]}
smoke_jar=${smoke_candidates[0]}
target_jar="$game_dir/mods/$(basename "$production_jar")"
target_smoke_jar="$game_dir/mods/$(basename "$smoke_jar")"

for required in "$version_json" "$client_jar" "$production_jar" "$smoke_jar" \
        "$game_dir/saves/$world_name/level.dat"; do
    if [[ ! -f "$required" ]]; then
        echo "Required file is missing: $required" >&2
        exit 1
    fi
done
for required_dir in "$libraries_dir" "$assets_dir" "$natives_dir"; do
    if [[ ! -d "$required_dir" ]]; then
        echo "Required directory is missing: $required_dir" >&2
        exit 1
    fi
done
if [[ -z "$java_bin" || ! -x "$java_bin" ]]; then
    echo "A Java 8 executable is required; pass --java or GLIDE_JAVA" >&2
    exit 1
fi
if ! "$java_bin" -version 2>&1 | head -n 1 | grep -Eq 'version "1\.8\.'; then
    echo "The smoke test must run on Java 8: $java_bin" >&2
    exit 1
fi

if pgrep -af 'net\.minecraft\.launchwrapper\.Launch' | grep -F -- "--gameDir $game_dir" >/dev/null; then
    echo "A Minecraft process is already using $game_dir" >&2
    exit 1
fi

mkdir -p "$game_dir/mods" "$game_dir/backups" "$artifact_dir"
installed_glide_jars=("$game_dir"/mods/GlideClient-Forge-*.jar)
for installed in "${installed_glide_jars[@]}"; do
    if [[ "$installed" == "$target_jar" || "$installed" == "$target_smoke_jar" ]]; then
        continue
    fi
    installed_hash=$(sha256sum "$installed" | cut -c1-8)
    backup="$game_dir/backups/$(basename "$installed").before-release-$installed_hash"
    suffix=1
    while [[ -e "$backup" ]]; do
        backup="$game_dir/backups/$(basename "$installed").before-release-$installed_hash-$suffix"
        suffix=$((suffix + 1))
    done
    mv "$installed" "$backup"
done
if [[ -f "$target_jar" ]] && ! cmp -s "$production_jar" "$target_jar"; then
    existing_hash=$(sha256sum "$target_jar" | cut -c1-8)
    backup="$game_dir/backups/$(basename "$target_jar").before-release-$existing_hash"
    if [[ ! -f "$backup" ]]; then
        cp --preserve=timestamps "$target_jar" "$backup"
    fi
fi
install -m 0644 "$production_jar" "$target_jar"
install -m 0644 "$smoke_jar" "$target_smoke_jar"

extras=()
oneconfig_loader="$version_dir/OneConfig/launchwrapper/OneConfig-Loader.jar"
if [[ -f "$oneconfig_loader" ]]; then
    extras+=(--extra "$oneconfig_loader")
fi
classpath=$(python3 "$script_dir/resolve_classpath.py" \
    --version-json "$version_json" \
    --libraries "$libraries_dir" \
    --client-jar "$client_jar" \
    "${extras[@]}")

child_pid=
cleanup() {
    if [[ -n "$child_pid" ]] && kill -0 "$child_pid" 2>/dev/null; then
        kill "$child_pid" 2>/dev/null || true
        for _ in {1..20}; do
            kill -0 "$child_pid" 2>/dev/null || break
            sleep 0.25
        done
        if kill -0 "$child_pid" 2>/dev/null; then
            kill -KILL "$child_pid" 2>/dev/null || true
        fi
        wait "$child_pid" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

: > "$log_file"
(
    cd "$game_dir"
    exec "$java_bin" \
        -XX:-OmitStackTraceInFastThrow \
        -Dfml.ignoreInvalidMinecraftCertificates=true \
        -Dfml.ignorePatchDiscrepancies=true \
        -Dlog4j2.formatMsgNoLookups=true \
        -Djava.library.path="$natives_dir" \
        -Dorg.lwjgl.librarypath="$natives_dir" \
        -Dglide.smoke.world="$world_name" \
        -Dglide.smoke.readyTicks=160 \
        -Dglide.smoke.exitTicks=600 \
        -Xmx4096m \
        -cp "$classpath" \
        net.minecraft.launchwrapper.Launch \
        --username GlideSmoke \
        --version GlideClient-Forge \
        --gameDir "$game_dir" \
        --assetsDir "$assets_dir" \
        --assetIndex 1.8 \
        --uuid 2a8212d75d3642059a3a036a0c41e654 \
        --accessToken 0 \
        --userProperties '{}' \
        --userType legacy \
        --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker \
        --width 1280 \
        --height 720
) > "$log_file" 2>&1 &
child_pid=$!

ready=0
deadline=$((SECONDS + startup_timeout))
while (( SECONDS < deadline )); do
    if grep -Fq 'GLIDE_SMOKE_WORLD_READY' "$log_file"; then
        ready=1
        break
    fi
    if ! kill -0 "$child_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done
if (( ready == 0 )); then
    echo "Minecraft did not reach the in-world smoke marker" >&2
    tail -n 80 "$log_file" >&2
    exit 1
fi

if (( capture_screenshot == 1 )); then
    if ! command -v xdotool >/dev/null || ! command -v import >/dev/null; then
        echo "xdotool and ImageMagick import are required for screenshot verification" >&2
        exit 1
    fi
    window_id=$(xdotool search --onlyvisible --pid "$child_pid" 2>/dev/null | tail -n 1 || true)
    if [[ -z "$window_id" ]]; then
        window_id=$(xdotool search --onlyvisible --name 'Minecraft.*1\.8\.9' 2>/dev/null | tail -n 1 || true)
    fi
    if [[ -z "$window_id" ]]; then
        echo "Could not find the Minecraft XWayland window" >&2
        exit 1
    fi
    xdotool windowactivate --sync "$window_id" 2>/dev/null || true
    sleep 1
    import -window "$window_id" "$screenshot"
    if [[ ! -s "$screenshot" ]]; then
        echo "Minecraft screenshot capture failed: $screenshot" >&2
        exit 1
    fi
fi

deadline=$((SECONDS + shutdown_timeout))
while kill -0 "$child_pid" 2>/dev/null && (( SECONDS < deadline )); do
    sleep 1
done
if kill -0 "$child_pid" 2>/dev/null; then
    echo "Minecraft did not close after the smoke controller timeout" >&2
    exit 1
fi

set +e
wait "$child_pid"
exit_code=$?
set -e
child_pid=
if (( exit_code != 0 )); then
    echo "Minecraft exited with status $exit_code" >&2
    tail -n 80 "$log_file" >&2
    exit 1
fi

if (( require_optifine == 1 )); then
    "$script_dir/verify-smoke-log.sh" "$log_file" --require-optifine
else
    "$script_dir/verify-smoke-log.sh" "$log_file"
fi
sha256sum "$production_jar"
if (( capture_screenshot == 1 )); then
    sha256sum "$screenshot"
fi
