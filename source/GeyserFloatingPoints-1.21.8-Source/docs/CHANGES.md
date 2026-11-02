# Source compatibility branch changes

- Java 1.21.8 BACKEND explicitly documented; Geyser Build #1276 corresponds to Geyser Core `2.12.1-SNAPSHOT` (source commit `e963aec`). Current Geyser Core API version is configurable through `-PgeyserCoreVersion`.
- Remove Modrinth upload automation to prevent accidental publishing and remove the stale `1.21.11`-only release metadata.
- Fix possible null end position in `ClientboundEntityPositionSyncPacket` (upstream issue #40).
- Correct `PacketSendingEvent` cancellation to ensure an offset-triggered cancelled movement does not leak to the Java server.
- Avoid mutating the original Java position packet's `relatives` collection.
- Clear local chunk cache when Java login or respawn packets arrive (retained from upstream 2.0).
- Include a full local source project and build script; no compiled extension JAR.
- Preserve GPL-3.0 licensing and attribution to oryxel1.

## Known limits

- The screenshots show at least two distinct `NoSuchMethodError`s. One refers to `GeyserSession.getRegistryCache`, the other to the spawn-position packet. Their COMPLETE stack traces and exact live Geyser version are needed to identify all callers.
- Changing release metadata from 1.21.11 to 1.21.8 is not a binary API fix.
- `compileOnly` of Geyser Core is tightly coupled to a runtime version; do not mix versions or expect universal binary compatibility.
- No remote Maven dependencies, game runtime, live Velocity, or Bedrock client were accessible in the build environment. Build success and gameplay compatibility have NOT been verified.

- Build #1276 runtime and backend 1.21.8 are separate version axes; first requires Core API alignment, second requires protocol bridge.
- `1.5` is known to reference the now-removed `getRegistryCache()`; this fork is based on 2.0 instead.
