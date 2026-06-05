[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder** 是一款针对 Android (minSdk 31+) 的轻量、安全且隐私优先的录音应用。通过利用 Cloudflare Workers 的零成本无服务器（Serverless）架构，它实现了由 Google Gemini 驱动的高性能语音转文字 (STT) 和文本总结 (LLM) 功能，同时确保开发者的 API 密钥 100% 安全且不向客户端暴露。

* **极致轻量：** 通过 R8 混淆优化和 ABI 分包，Release APK 体积控制在 **10MB** 以内。
* **零成本 STT & 总结：** 通过安全的 Cloudflare Worker 反向代理 Google Gemini 的免费层多模态 API，实现音频转写与总结。
* **流畅波形：** 支持 M4A、MP3、WAV 和 3GP 格式，具备与 VSYNC 同步的波形动画，并支持动态峰值增益自动缩放。
* **分区存储：** 原生通过存储访问框架 (SAF) 处理，允许用户自定义存储目录。
* **原生分享：** 支持通过 `FileProvider` 一键安全地将文件分享至微信、QQ、WhatsApp 和 Telegram。

```text
本地录音 ──► Cloudflare Worker (Gemini STT) ──► 纯文本 ──► Cloudflare Worker (Gemini LLM) ──► 摘要总结
