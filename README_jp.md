[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder** は、Android (minSdk 31+) 向けの軽量、安全、かつプライバシーを最優先したボイスレコーダーアプリです。Cloudflare Workers を利用したコストゼロのサーバーレスアーキテクチャにより、開発者の API キーをクライアント側に一切露出させることなく完全に保護しながら、Google Gemini を活用した高性能な音声文字起こし (STT) および要約 (LLM) ツールを提供します。

* **超軽量設計:** R8 の最適化と ABI 分割により、リリース APK のサイズを **10MB** 未満に抑えています。
* **コストゼロの STT & 要約:** 安全な Cloudflare Worker をプロキシとして経由し、Google Gemini の無料枠マルチモーダル API を利用して音声の文字起こしと要約を行います。
* **滑らかな波形表示:** M4A、MP3、WAV、3GP フォーマットに対応し、VSYNC に同期した波形アニメーションと動的なピークゲイン自動スケーリングを搭載しています。
* **Scoped Storage 対応:** Storage Access Framework (SAF) をネイティブに処理し、カスタム保存ディレクトリの選択が可能です。
* **ネイティブ共有:** `FileProvider` を介して、WeChat、QQ、WhatsApp、Telegram へワンタップで安全にファイルを共有できます。

```text
ローカル録音 ──► Cloudflare Worker (Gemini STT) ──► テキスト ──► Cloudflare Worker (Gemini LLM) ──► 要約
