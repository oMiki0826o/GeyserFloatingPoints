#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 ]]; then echo "Usage: $0 /path/to/Geyser-Velocity.jar [GeyserFloatingPoints.jar]"; exit 2; fi
G="$1"
[[ -f "$G" ]] || { echo "Missing: $G"; exit 2; }
java -version 2>&1 | head -n 1
sha256sum "$G"
echo 'Look for this loaded method in the Geyser Core jar or its nested dependencies:'
javap -classpath "$G" org.geysermc.geyser.session.GeyserSession 2>&1 | grep -E 'RegistryCache|javaRegistries|GeyserSession|Error:' || true
echo 'Check the Java protocol class/method signatures loaded by Geyser:'
javap -classpath "$G" org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket 2>&1 | head -n 35 || true
if [[ $# -ge 2 ]]; then sha256sum "$2"; unzip -p "$2" extension.yml || true; fi
