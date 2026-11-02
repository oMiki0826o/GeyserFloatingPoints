# GeyserFloatingPoints 1.21.8 修復 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立隔離的 GFP 修正來源與 Java_C 副本服，修復已知遠座標 offset 封包缺口而不觸碰運行中服務。

**Architecture:** GFP source 保持 Geyser Extension 形式，在 Java protocol 攔截層統一重寫絕對座標；活塞使用與 1.5 相同意圖的碰撞校正，但只呼叫已被實際 b1277 API 證實的介面。副本服是 Java_C 的檔案副本，採用不同 port、query port、RCON port 與 level name。

**Tech Stack:** Java 21、Gradle、Geyser 2.12.1-b1277、MCProtocolLib、Fabric 1.21.8。

**Spec:** `GeyserFloatingPoints-1.21.8-fix/docs/design.md`

## Global Constraints

- 所有工作檔案僅置於 `GeyserFloatingPoints-1.21.8-fix/`。
- 不操作 `/home/allen/Velocity`、`/home/allen/Java`、`/home/allen/Java_C` 的既有服務。
- 不做紅燈測試；僅做靜態、編譯、隔離設定與隔離後端啟動／停止驗證。
- 不採用 1.5 的 `GeyserSession.getRegistryCache()`。
- 不把產生的 JAR 部署至任何 extensions 目錄。

## Review Focus

- 活塞推動時，修正後 Java 移動座標必須保持真實 offset，不能回到本地原點。
- 世界重生點、爆炸與 look-at 的 X/Z 必須採用同一 session offset。
- 不可變 relatives 集合及 null entity sync position 不得造成 NPE 或 UnsupportedOperationException。
- 私有 API 簽章不相符時必須有可辨識日誌，不得靜默吞掉。
- 副本服設定不可與 51215、51216、51217 或既有 query/RCON port 衝突。

### Task 1: 建立隔離基線與副本服

**Files:** `baseline/`, `source/`, `test-server/`, `build-evidence/baseline.md`, `test-server/server.properties`

- [ ] 複製附件 source/JAR 到基線並記錄 SHA-256。
- [ ] 解壓 source 至 `source/`。
- [ ] 複製 `/home/allen/Java_C/` 至 `test-server/`，排除 logs、crash-reports、processedMods 與 runtime lock files。
- [ ] 將副本 server/query/RCON port 與 level name 改為未使用的獨立值；不啟動副本服。

### Task 2: 鎖定 b1277 API 與恢復碰撞校正

**Files:** `source/.../PositionPacketsRewriter.java`, `source/.../OffsetCollisionCorrector.java`, `source/.../GeyserUtil.java`, `build-evidence/api-compatibility.md`

- [ ] 以實際 Geyser-Velocity JAR 類別簽章確認碰撞 API。
- [ ] 新增集中式 offset collision corrector，僅在 own chunk cache 可用時校正 serverbound 玩家位置。
- [ ] 對無法安全套用的相容性條件輸出 rate-limited warning。

### Task 3: 恢復世界與實體絕對座標封包

**Files:** `source/.../WorldPacketsRewriter.java`, `source/.../EntityPacketsRewriter.java`, `source/.../GeyserFPUser.java`

- [ ] 恢復 default spawn、explosion、sound、particles、level event、open sign editor、player look-at 的 offset 轉換。
- [ ] 加入現行 MCProtocolLib 的絕對座標實體 teleport 路徑。
- [ ] 將 offset 反射失敗改為含上下文的 warning。

### Task 4: 區塊重送安全與建置證據

**Files:** `source/.../ChunkCache.java`, `source/.../GeyserFPUser.java`, `build-evidence/build-results.md`, `build-evidence/known-limitations.md`

- [ ] 避免重入時重複重送區塊，記錄 offset 重送的快取數量。
- [ ] 以 JDK 21 執行靜態檢查與乾淨 shadowJar。
- [ ] 記錄 JAR metadata、SHA-256 與未進行的實機測試。

## Self-review

- Task 1 覆蓋隔離副本與不碰既有服務。
- Task 2 覆蓋活塞 offset 碰撞校正。
- Task 3 覆蓋重生點與珍珠砲相關絕對座標封包。
- Task 4 覆蓋遠距離區塊快取重送、可重現建置與未驗證範圍。
