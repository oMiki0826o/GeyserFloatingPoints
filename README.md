# GeyserFloatingPoints

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-orange)
![Geyser](https://img.shields.io/badge/Geyser-2.12.1--b1277-blue)

[中文](#中文) | [English](#english)

## 中文

### 關於

GeyserFloatingPoints 是 Minecraft 1.21.8 的 Geyser 擴充. 它為每位 Bedrock 玩家維護獨立的浮動原點, 在 Java 世界的極遠座標降低 Bedrock 客戶端浮點精度問題.

### 功能

- 依玩家與維度維護區塊對齊的座標偏移.
- 改寫部分 Java 世界, 實體與互動封包的座標.
- 快取並在原點切換後重送已載入的區塊.
- 以 ActionBar 顯示真實 Java 座標.

### 建置

在 `source/GeyserFloatingPoints-1.21.8-Source` 執行:

```bash
./gradlew shadowJar
```

建置時需提供與目標環境相容的 Geyser runtime JAR.

### 使用方式

將建置出的 extension JAR 放入 Geyser 的 `extensions/` 目錄, 不是 Fabric 的 `mods/` 目錄. 啟動 Geyser 後再依 extension 設定檔調整偏移行為.

---

## English

### About

GeyserFloatingPoints is a Geyser extension for Minecraft 1.21.8. It maintains a per-Bedrock-player floating origin to reduce Bedrock client floating-point precision issues at extreme Java world coordinates.

### Features

- Maintains chunk-aligned coordinate offsets per player and dimension.
- Rewrites coordinates in selected Java world, entity, and interaction packets.
- Caches loaded chunks and resends them after an origin shift.
- Displays real Java coordinates through the ActionBar.

### Build

Run the following in `source/GeyserFloatingPoints-1.21.8-Source`:

```bash
./gradlew shadowJar
```

The build requires a Geyser runtime JAR compatible with the target environment.

### Usage

Place the built extension JAR in Geyser's `extensions/` directory, not Fabric's `mods/` directory. Start Geyser and configure offset behavior through the extension configuration file.
