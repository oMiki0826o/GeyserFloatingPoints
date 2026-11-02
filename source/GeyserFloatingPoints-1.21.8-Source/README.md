# GeyserFloatingPoints — 1.21.8 Compatibility Source Branch

**An editable, uncompiled Geyser extension source project** based on [oryxel1/GeyserFloatingPoints](https://github.com/oryxel1/GeyserFloatingPoints). Original author: oryxel; original license: GPL-3.0-or-later. This adaptation retains the original GPL license rights/conditions and credits. See `LICENSE`.

## Scope: what '1.21.8' means

- **Target backend:** Minecraft Java **1.21.8** Survival behind a Velocity proxy, with Geyser-Velocity **Build #1276** (28 Oct 2026).
- **Runtime:** Geyser-Velocity that can connect to the 1.21.8 backend through ViaVersion or another explicitly tested protocol bridge.
- **Geyser Core API:** Compile-time version defaults to `2.12.1-SNAPSHOT` and can be overridden. This is **NOT** the Minecraft backend version, and it is **NOT** a promise of compatibility with every old or new Geyser build.
- **Not a Fabric mod:** Install the *built* JAR under Geyser's `extensions` directory, not Fabric's `mods` directory.

## Build (Ubuntu)

Prerequisites: JDK 21, Gradle 9.x (or generate a wrapper), Maven network access, and Geyser API dependency matching the **Geyser-Velocity JAR currently deployed**.

```bash
cd GeyserFloatingPoints-1.21.8-Source
java -version
gradle clean shadowJar
# OR supply an explicit matching version, e.g.:
gradle clean shadowJar -PgeyserCoreVersion=2.12.1-SNAPSHOT
```

Produces `build/libs/GeyserFloatingPoints-1.21.8-compat.jar` if compilation succeeds. Archive contains NO compiled JAR, NO Gradle wrapper binary, and NO third-party binaries. If you have no Gradle installation, install Gradle 9.x and run `gradle wrapper` first.

**Do not replace the live extension without staging tests.** Stop Velocity; move the old GFP extension out of `extensions`; restart Velocity and confirm Bedrock login succeeds without any GFP; next install this fork's build in the staging proxy; never run both at once.

## Included source and fixes

- Every upstream 2.0 source component is included as editable Java source (17 source files).
- End-position null guard in `EntityPacketsRewriter` to prevent one possible NPE.
- Actual `PacketSendingEvent#setCancelled(true)` when an offset relocation cancels an outgoing movement packet (otherwise the original packet could still be transmitted).
- Explicit separation of original Minecraft Java protocol 1.21.8 backend version from the Geyser Core compile API.
- Cleared chunk cache at login/respawn, retained from the current upstream 2.0.
- Non-mutating copy of the client position packet's `PositionElement` relatives.
- Correctly released a locally allocated Netty chunk parsing buffer.
- Runtime API diagnostics script, config, build script and license included.

## IMPORTANT: known unresolved compatibility risks

The two screenshots previously shared contained TWO different `NoSuchMethodError` messages, including `GeyserSession.getRegistryCache()` and a method of `ClientboundSetDefaultSpawnPositionPacket`. The *current* upstream 2.0 source doesn't directly call either method; changing Minecraft version metadata alone cannot fix a method that is absent at runtime. The first screenshot truncates the method name, and the complete Velocity/Geyser stack traces were not available.

This source has not been compiled or tested against your deployed Geyser core, Velocity build or Bedrock client. If a compile error or `NoSuchMethodError` remains, compare the exact Geyser runtime build and library class signatures. See `docs/CHANGES.md`.

### Collect diagnostics

```bash
# In Velocity console:
geyser version
# On Ubuntu, optionally:
./scripts/check-runtime.sh /path/to/Geyser-Velocity.jar
```

Also collect the **full stack trace**, especially method names after `ClientboundSetDefaultSpawnPositionPacket` and all `at ...` stack frames.

## Tests to run before any release

1. Baseline Bedrock login without any GFP extension.
2. Login with this extension on a staging Velocity proxy.
3. Move, teleport, respawn, disconnect/reconnect, switch servers and dimensions.
4. X/Z=0, 3,000, 100,000, 1,000,000 and 10,000,000 with block placement and entity spawning.
5. Check console exceptions, `moved too quickly` and local coordinate drift.
6. Verify Java clients and other installed extensions still behave normally.

`docs/CHANGES.md` contains the known limitations and modification notes.

## Target build provenance (checked from official source)

The GeyserMC download page lists **Build #1276 (2026-10-28)** as current for the referenced environment. Its latest change is commit `e963aec`. Geyser source `gradle.properties` **at `e963aec`** declares `version=2.12.1-SNAPSHOT`. That is why this project uses `geyserCoreVersion=2.12.1-SNAPSHOT` by default.

- https://geysermc.org/download
- https://github.com/GeyserMC/Geyser/blob/e963aec/gradle.properties
- https://github.com/oryxel1/GeyserFloatingPoints (original 2.0 source revision `617f00b`)

A mutable Maven SNAPSHOT **does not pin the exact Build #1276 binary API**. For strict compatibility, arrange the exact matching Geyser Core artifacts in a local Maven repository and compile against those or compare method signatures with the installed plugin JAR. This branch does **not** falsely claim full verification.

### Build-specific fault analysis

- Old `1.5`: `getRegistryCache()` is absent from GeyserSession in Build #1276; the 2.0 source uses `javaRegistries()` instead, and should not invoke that removed method.
- Reported `ClientboundSetDefaultSpawnPositionPacket` error: the screenshot truncates the method name and cannot yet establish if Geyser, another extension, or plugin code is the caller. Collect full proxy stack traces before assigning cause.
- Backend 1.21.8: requires appropriate ViaVersion protocol translation on the Java backend or a proven alternative; Geyser Build #1276 targets a newer Java protocol.

### Static source check

```bash
python3 scripts/verify-source.py
```

This checks the presence of complete source and important repair markers; it is **not** equivalent to compiling or running a Bedrock client.
