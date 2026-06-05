# Flame-Recorder

**Flame Recorder** is a lightweight, secure, and privacy-first voice recorder for Android (minSdk 31+). By leveraging a zero-cost serverless architecture with Cloudflare Workers, it delivers high-performance voice-to-text (STT) and summarization (LLM) powered by Google Gemini, while keeping developer API keys 100% secure and hidden.

### Key Highlights

* **Ultra-Lightweight**: Release APKs are under **10MB** using R8 minification and ABI splits.
* **Zero-Cost STT**: Transcribes audio via a secure Cloudflare Worker proxying Google Gemini's free-tier multimodal API.
* **Fluid Waveform**: VSYNC-synced waveform animation with dynamic peak-gain auto-scaling for M4A, MP3, WAV, and 3GP formats.
* **Scoped Storage**: Handled natively via Storage Access Framework (SAF), allowing custom storage directories.
* **Native Sharing**: One-click secure file sharing to WeChat, QQ, WhatsApp, and Telegram via `FileProvider`.

### How It Works
Local Recording ──► Cloudflare Worker (Gemini STT) ──► Plain Text ──► Cloudflare Worker (Gemini LLM) ──► Summary
