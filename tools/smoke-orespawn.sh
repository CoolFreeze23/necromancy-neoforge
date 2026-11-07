#!/usr/bin/env bash
# The hidden dev client with OreSpawn (and GeckoLib) installed in run-smoke/mods, for checking OreSpawn creature parts.
# usage: tools/smoke-orespawn.sh <script>      ORESPAWN_JAR / GECKOLIB_JAR override the jars; the mods are removed again after.
ROOT=$(cd "$(dirname "$0")/.." && pwd)
PACK=/c/Homework/Projects/CrazyCraft5/build/server-pack/mods
ORESPAWN_JAR=${ORESPAWN_JAR:-$PACK/orespawn-1.21.1-2.0.0-beta.15.jar}
GECKOLIB_JAR=${GECKOLIB_JAR:-$PACK/geckolib-neoforge-1.21.1-4.9.3.jar}
mkdir -p "$ROOT/run-smoke/mods"
cp "$ORESPAWN_JAR" "$GECKOLIB_JAR" "$ROOT/run-smoke/mods/"
bash "$ROOT/tools/smoke.sh" "$@"
rc=$?
rm -f "$ROOT/run-smoke/mods/$(basename "$ORESPAWN_JAR")" "$ROOT/run-smoke/mods/$(basename "$GECKOLIB_JAR")"
exit $rc
