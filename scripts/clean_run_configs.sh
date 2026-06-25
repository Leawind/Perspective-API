#!/usr/bin/env bash
set -euo pipefail

RUN_CONFIGS_DIR=".idea/runConfigurations"

if [ ! -d "$RUN_CONFIGS_DIR" ]; then
  echo "Directory $RUN_CONFIGS_DIR not found, skipping."
  exit 0
fi

# ./gradlew --no-build-cache --rerun-tasks --offline stonecutterIdea
# ./gradlew --no-build-cache --rerun-tasks --offline configureClientLaunch

echo "==> Removing configurations that are not Minecraft_Client or Stonecutter"
for f in "$RUN_CONFIGS_DIR"/*.xml; do
  [ -f "$f" ] || continue
  basename="$(basename "$f")"
  if [[ "$basename" != Minecraft_Client* && "$basename" != Stonecutter* ]]; then
    echo "    Removing: $basename"
    rm -f "$f"
  fi
done

echo "==> Done. Remaining configurations:"
ls "$RUN_CONFIGS_DIR"/*.xml 2>/dev/null | xargs -I{} basename {} || echo "    (none)"
