#!/usr/bin/env bash
# Places several villages of one type on a hubtest server and counts the necromancers (one per cemetery piece).
# usage: tools/worldgen_probe.sh TEST_NAME VILLAGE_TYPE [COUNT] [START_X] [Z]
#   e.g. tools/worldgen_probe.sh necro-wg plains 8 3000 3000
set -u
H=${HUBTEST:-/c/Homework/Sessions/Japan/hubtest/hubtest}
NAME=$1
TYPE=$2
COUNT=${3:-8}
X0=${4:-3000}
Z=${5:-3000}
NECRO='nbt={VillagerData:{profession:"necromancy:necromancer"}}'
for ((i = 0; i < COUNT; i++)); do
  x=$((X0 + i * 400))
  "$H" rcon "$NAME" \
    "forceload add $((x - 112)) $((Z - 112)) $((x + 112)) $((Z + 112))" \
    "place structure minecraft:village_$TYPE $x 0 $Z" \
    "execute if entity @e[type=villager,x=$x,y=64,z=$Z,distance=..160,$NECRO]" \
    "execute if entity @e[type=villager,x=$x,y=64,z=$Z,distance=..160]" \
    "execute as @e[type=villager,x=$x,y=64,z=$Z,distance=..160,$NECRO] at @s run tp @s ~ ~ ~" \
    "forceload remove $((x - 112)) $((Z - 112)) $((x + 112)) $((Z + 112))" 2>&1 |
    grep -E "^> place|Test|Teleported|Generated|failed|Unknown|Error|error" | sed "s/^/[$TYPE $x] /"
done
