[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder** is a lightweight, secure, and privacy-first voice recorder for Android (minSdk 31+). By leveraging a zero-cost serverless architecture with Cloudflare Workers, it delivers high-performance voice-to-text (STT) and summarization (LLM) powered by Google Gemini, while keeping developer API keys 100% secure and hidden.

* **Ultra-Lightweight:** Release APKs are under **10MB** using R8 minification and ABI splits.
* **Zero-Cost STT & Summarization:** Transcribes and summarizes audio via a secure Cloudflare Worker proxying Google Gemini's free-tier multimodal API.
* **Fluid Waveform:** VSYNC-synced waveform animation with dynamic peak-gain auto-scaling for M4A, MP3, WAV, and 3GP formats.
* **Scoped Storage:** Handled natively via Storage Access Framework (SAF) for custom storage directories.
* **Native Sharing:** Secure file sharing to WeChat, QQ, WhatsApp, and Telegram via `FileProvider`.

```text
Local Recording ──► Cloudflare Worker (Gemini STT) ──► Plain Text ──► Cloudflare Worker (Gemini LLM) ──► Summary
