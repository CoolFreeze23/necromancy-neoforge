#!/usr/bin/env bash
# Nether Chalice checks on a hubtest server.
#   tools/chalice_probe.sh NAME place X Z [STEP COUNT]  try /place feature along a row until one fits (needs open lava below y 36)
#   tools/chalice_probe.sh NAME count X0 Z0             generate 16 x 16 Nether chunks from X0,Z0 and count chalices
#                                                       (eight redstone torches each, on the cup's fences at lava level + 22)
set -u
H=${HUBTEST:-/c/Homework/Sessions/Japan/hubtest/hubtest}
NAME=$1
MODE=$2
N="execute in minecraft:the_nether run"
case "$MODE" in
  place)
    X=$3; Z=$4; STEP=${5:-48}; COUNT=${6:-12}
    for ((i = 0; i < COUNT; i++)); do
      x=$((X + i * STEP))
      out=$("$H" rcon "$NAME" "$N forceload add $x $Z $((x + 15)) $((Z + 15))" "$N place feature necromancy:nether_chalice $x 36 $Z" \
        "$N forceload remove $x $Z $((x + 15)) $((Z + 15))" 2>&1 | grep -E "Placed|Failed|Unknown|rror")
      echo "[$x $Z] $out"
      case "$out" in *Placed*) break ;; esac
    done
    ;;
  count)
    X0=$3; Z0=$4
    "$H" rcon "$NAME" "$N forceload add $X0 $Z0 $((X0 + 255)) $((Z0 + 255))" 2>&1 | grep -E "Marked|rror"
    sleep 60
    for y in 50 51 52 53 54 55 56; do
      for half in 0 128; do
        "$H" rcon "$NAME" "$N fill $X0 $y $((Z0 + half)) $((X0 + 255)) $y $((Z0 + half + 127)) minecraft:soul_torch replace minecraft:redstone_torch" \
          "$N fill $X0 $y $((Z0 + half)) $((X0 + 255)) $y $((Z0 + half + 127)) minecraft:redstone_torch replace minecraft:soul_torch" 2>&1 |
          grep -E "filled" | head -1 | sed "s/^/[y=$y z+$half] /"
      done
    done
    "$H" rcon "$NAME" "$N forceload remove $X0 $Z0 $((X0 + 255)) $((Z0 + 255))" > /dev/null
    ;;
esac
