#!/usr/bin/env python3
"""Offline source archive smoke checks, not a Java compile or integration test."""
from pathlib import Path
import sys
root = Path(__file__).resolve().parents[1]
java = list((root / "src/main/java").rglob("*.java"))
expect = [
    "oxy/geyser/fp/GeyserFloatingPoints.java",
    "oxy/geyser/fp/network/wrapper/GeyserFPAdapterWrapper.java",
    "oxy/geyser/fp/packets/WorldPacketsRewriter.java",
    "oxy/geyser/fp/packets/PositionPacketsRewriter.java",
    "oxy/geyser/fp/packets/OffsetCollisionCorrector.java",
    "oxy/geyser/fp/packets/EntityPacketsRewriter.java",
    "oxy/geyser/fp/packets/ActionPacketsRewriter.java",
    "oxy/geyser/fp/session/cache/ChunkCache.java",
    "oxy/geyser/fp/session/GeyserFPUser.java",
]
for rel in expect:
    assert (root / "src/main/java" / rel).is_file(), rel
assert len(java) == 18, f"expected 18 Java sources, found {len(java)}"
# Build outputs are expected after Gradle runs. This verifier is about source
# archive contamination, so only inspect trees that should remain source-only.
source_only_trees = [root / "src", root / "scripts", root / "docs"]
assert not any(
    path.suffix in {".class", ".jar"}
    for tree in source_only_trees if tree.exists()
    for path in tree.rglob("*")
), "compiled binary detected in source-only tree"
alljava = "\n".join(x.read_text() for x in java)
assert "getRegistryCache(" not in alljava, "removed Geyser API still in source"
assert "sendingEvent.setCancelled(true)" in alljava, "movement cancellation fix missing"
assert "if (endPosition != null)" in alljava, "null guard missing"
assert "OffsetCollisionCorrector.correct" in alljava, "offset collision correction missing"
assert "ClientboundSetDefaultSpawnPositionPacket" in alljava, "default spawn offset rewrite missing"
assert "geyserCoreVersion=2.12.1-SNAPSHOT" in (root / "gradle.properties").read_text()
print(f"PASS: {len(java)} Java sources, offset collision/spawn patches present, source tree has no compiled binaries")
print("NOTE: Java compilation and live Geyser/Velocity/Bedrock integration remain unverified")
