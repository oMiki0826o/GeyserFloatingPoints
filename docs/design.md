# GeyserFloatingPoints 1.21.8 修復設計

## 意圖

修復目前 GeyserFloatingPoints compat JAR 在遠座標下的活塞碰撞校正、重生點與世界絕對座標封包缺口，同時建立不接觸既有服務的 Java_C 副本服。

## 目標

- 保留 Geyser 2.12.1-b1277 與 Java 1.21.8 後端的部署方向。
- 修復遠座標且已啟用 offset 時，活塞推動玩家可能被校正到本地原點的問題。
- 恢復 2.0 重構移除、但 1.5 已處理的世界重生點、爆炸、粒子、音效、世界事件、look-at 與告示牌絕對座標轉換。
- 對不相容的私有反射提供可診斷日誌，而非靜默忽略。
- 將 Java_C 複製為完全獨立的副本服，且不影響現行 Java_C。

## 非目標

- 不停止、重啟、重新設定或部署到 `/home/allen/Velocity`、`/home/allen/Java`、`/home/allen/Java_C`。
- 不進行 Bedrock 實機登入、珍珠砲、活塞或高座標實機測試；只允許啟動隔離後端副本作煙霧檢查。
- 不宣稱未知 Geyser 私有 API 在所有未來版本相容。

## 已選方案

以提供的 compat source 為修改基底；只移植 1.5 中仍可對應現行 MCProtocolLib/Geyser API 的行為，不回退 1.5 的 `getRegistryCache()` API。

活塞修正採用「offset 後的 Java serverbound 移動封包，透過 Geyser 世界管理器與碰撞方塊重新校正」的舊版語義。若目前 API 沒有等價簽章，保留安全 pass-through 並以明確警告記錄，不能用猜測的反射替代。

## 驗證決策

使用者明確要求不做紅燈測試，因此不建立或執行預期失敗測試。只執行靜態來源檢查、JDK 21 編譯、JAR metadata/雜湊、以及副本服的離線設定與埠隔離檢查。所有遊戲行為一律標示未驗證。
