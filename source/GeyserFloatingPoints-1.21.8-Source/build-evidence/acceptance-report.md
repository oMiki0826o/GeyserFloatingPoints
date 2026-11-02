# GFP compat.2 驗收紀錄

日期：2026-10-31（Asia/Taipei）

## 產物

- 檔案：`deliverables/GeyserFloatingPoints-1.21.8-compat.2-b1277-verified.jar`
- SHA-256：`dccb6dc4db7279dc14754cd20de10f09606f45ddc272c06dfe0f04500e05fe0d`
- extension 版本：`2.0.0-1.21.8-compat.2`

## 實際執行的驗收

1. Java 21：`gradle clean shadowJar test --no-daemon -PgeyserRuntimeJar=/home/allen/Velocity/plugins/Geyser-Velocity.jar`
   - 結果：成功；`test NO-SOURCE`，專案未提供 JUnit 測試。
2. `scripts/verify-source.py`
   - 結果：18 個 Java 來源、碰撞校正與出生點封包重寫存在、來源樹沒有編譯二進位。
3. 實體 JAR 檢查
   - 結果：含 `extension.yml`、`OffsetCollisionCorrector`、位置／世界／實體封包重寫類別。
4. 隔離 Fabric 副本服
   - 路徑：`test-server/Java_C`；port：`51226`。
   - 結果：Minecraft 1.21.8 以 Java 21 啟動、輸出 `Done`，再正常停止並保存三個維度；測試後未殘留 51226 監聽。
5. 隔離 Velocity/Geyser 實載
   - 路徑：`test-proxy/Velocity`；Java proxy：`127.0.0.1:25586`；Bedrock：`127.0.0.1:19146`。
   - 結果：Velocity 4.3.1 以 Java 25 載入 Geyser 2.12.1-b1277；日誌確認 `Enabled extension GeyserFloatingPoints`、`Loaded 1 extension(s)`、`Started Geyser on 127.0.0.1:19146`。正常結束時確認 `Disabled extension GeyserFloatingPoints` 及 `Geyser shutdown successfully`，兩個專用 port 都已釋放。

## 已修復範圍

- serverbound 玩家移動以真正的 offset 座標進行 Geyser 方塊碰撞校正，修復高座標活塞可能回到本地原點的缺口。
- 補回 default spawn、爆炸、音效、粒子、世界事件、告示牌編輯及 player look-at 的絕對 X/Z offset 重寫。
- 補回絕對位置 entity teleport 重寫。
- Java 21 編譯時移除不可能拋出的反射例外 catch。
- chunk 重送加入重入保護；私有反射失敗會只記錄一次可識別警告。

## Production timeout forensic boundary

- The live Velocity log recorded downstream read timeouts exactly 30 seconds after survival connections at 12:13:11 and 12:15:47.
- At those same times the live survival log had already stopped servicing the server thread: Spark could not obtain world statistics every minute, and at 12:20 it reported 739611 ms (14792 ticks) behind.
- The survival server did not process the later login until 12:22:24. This identifies the immediate timeout source as backend main-thread starvation (consistent with the pearl-cannon/entity-collision workload), rather than Geyser authentication or extension load failure.
- Raising Velocity read-timeout would only delay the disconnect. It is not recorded as a fix.

## Packet coverage audit

- GFP 1.5 packets were compared with compat.2. The absent ClientboundMoveEntityPosPacket and ClientboundMoveEntityPosRotPacket are relative motion packets; ClientboundRemoveEntitiesPacket and ServerboundLoadingScreenPacket do not carry world positions, so none needs an origin rewrite.
- Current protocol absolute entity relocation is handled by ClientboundTeleportEntityPacket in compat.2.

## 相容性基準與限制

- 使用者起初提供 #1276 GFP 來源，但附件沒有 Geyser #1276 執行 JAR。機器目前實際 Velocity Geyser JAR／log 顯示 2.12.1-b1277，因此編譯 API 驗收採 b1277。
- 已驗證 extension 能被真實 Geyser/Velocity 載入；沒有 Bedrock 用戶端，故尚未驗證 Bedrock 登入、超遠座標重連、活塞或珍珠砲行為矩陣。
- A local RakNet Bedrock protocol client was also attempted against the isolated Geyser endpoint. It reached Geyser but was rejected before Java-session creation with "Please log into Xbox to join this server." Geyser requires a valid Xbox identity even when its Java auth-type is offline. Therefore a real authenticated Bedrock client is still required to execute the requested high-coordinate teleport, piston, and pearl-cannon matrix.
- JAR 尚未部署；應放入隔離 Geyser/Velocity 的 `extensions/`，不可放入 Fabric `mods/`。
