[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder** 是一款針對 Android (minSdk 31+) 的輕量、安全且隱私優先的錄音應用程式。透過利用 Cloudflare Workers 的零成本無伺服器（Serverless）架構，它實現了由 Google Gemini 驅動的高效能語音轉文字 (STT) 和文字總結 (LLM) 功能，同時確保開發者的 API 金鑰 100% 安全且不向用戶端暴露。

* **極致輕量：** 透過 R8 混淆最佳化和 ABI 分包，Release APK 體積控制在 **10MB** 以內。
* **零成本 STT & 總結：** 透過安全的 Cloudflare Worker 反向代理 Google Gemini 的免費層多模態 API，實現音訊轉寫與總結。
* **流暢波形：** 支援 M4A、MP3、WAV 和 3GP 格式，具備與 VSYNC 同步的波形動畫，並支援動態峰值增益自動縮放。
* **分區儲存：** 原生透過儲存存取框架 (SAF) 處理，允許使用者自訂儲存目錄。
* **原生分享：** 支援透過 `FileProvider` 一鍵安全地將檔案分享至微信、QQ、WhatsApp 和 Telegram。

```text
本地錄音 ──► Cloudflare Worker (Gemini STT) ──► 純文字 ──► Cloudflare Worker (Gemini LLM) ──► 摘要總結
