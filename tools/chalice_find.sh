#!/usr/bin/env bash
# Finds the chunks holding a Nether Chalice (its redstone torches at Y) in a square of chunks, by swapping torches twice.
# usage: tools/chalice_find.sh NAME X0 Z0 CHUNKS [Y]      X0/Z0 on a chunk corner; the area must be loaded
set -u
H=${HUBTEST:-/c/Homework/Sessions/Japan/hubtest/hubtest}
NAME=$1; X0=$2; Z0=$3; C=$4; Y=${5:-53}
for ((i = 0; i < C; i++)); do
  for ((j = 0; j < C; j++)); do
    x=$((X0 + i * 16)); z=$((Z0 + j * 16))
    r=$("$H" rcon "$NAME" "execute in minecraft:the_nether run fill $x $Y $z $((x + 15)) $Y $((z + 15)) minecraft:soul_torch replace minecraft:redstone_torch" \
      "execute in minecraft:the_nether run fill $x $Y $z $((x + 15)) $Y $((z + 15)) minecraft:redstone_torch replace minecraft:soul_torch" 2>&1 | grep -m1 "Successfully")
    [ -n "$r" ] && echo "chunk at $x $z: $r"
  done
done
