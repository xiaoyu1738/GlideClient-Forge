#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <smoke-log> [--require-optifine]" >&2
    exit 2
fi

log_file=$1
require_optifine=0
if [[ ${2:-} == "--require-optifine" ]]; then
    require_optifine=1
elif [[ -n ${2:-} ]]; then
    echo "Unknown option: $2" >&2
    exit 2
fi
if [[ ! -f "$log_file" ]]; then
    echo "Smoke log does not exist: $log_file" >&2
    exit 1
fi

required_markers=(
    "Glide platform bootstrap detected os=Linux, wayland=true"
    "Glide skipped Windows-only Mixin configuration"
    "Forge Mod Loader has successfully loaded"
    "GLIDE_SMOKE_MIXIN_AUDIT_OK"
    "GLIDE_SMOKE_SCREEN_CLEARED"
    "GLIDE_SMOKE_WORLD_READY"
    "GLIDE_SMOKE_SHUTDOWN"
)

for marker in "${required_markers[@]}"; do
    if ! grep -Fq "$marker" "$log_file"; then
        echo "Missing required smoke marker: $marker" >&2
        exit 1
    fi
done

if (( require_optifine == 1 )); then
    if ! grep -Eq 'OptiFine_1\.8\.9_[A-Za-z0-9_]+' "$log_file" \
            || ! grep -Fq 'Forge Mod Loader has detected optifine' "$log_file"; then
        echo "OptiFine was required but was not detected in $log_file" >&2
        exit 1
    fi
fi

failure_pattern='FATAL|Unreported exception|Exception caught during firing|disabled after failure|Glide startup failed|\[GC/ERROR\]|NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|MixinTransformerError|GLIDE_SMOKE_FAILED|GLIDE_SMOKE_WORLD_LAUNCH_FAILED'
if grep -E "$failure_pattern" "$log_file"; then
    echo "A fatal compatibility signature was found in $log_file" >&2
    exit 1
fi

echo "Smoke log passed: $log_file"
