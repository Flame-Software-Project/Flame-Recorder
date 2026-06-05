[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder**는 Android (minSdk 31+)를 위한 가볍고 안전하며 개인정보를 최우선으로 하는 음성 녹음 앱입니다. Cloudflare Workers의 비용 제로 서버리스(Serverless) 아키텍처를 활용하여, 개발자의 API 키를 클라이언트 측에 노출하지 않고 100% 안전하게 보호하면서 Google Gemini 기반의 고성능 음성 인식(STT) 및 요약(LLM) 기능을 제공합니다.

* **초경량 설계:** R8 최적화 및 ABI 분할을 통해 릴리스 APK 크기를 **10MB** 미만으로 유지합니다.
* **비용 제로 STT & 요약:** 안전한 Cloudflare Worker 프록시를 통해 Google Gemini의 무료 티어 멀티모달 API를 호출하여 오디오 받아쓰기 및 요약을 수행합니다.
* **부드러운 파형 애니메이션:** M4A, MP3, WAV, 3GP 포맷을 지원하며, VSYNC와 동기화된 파형 애니메이션 및 동적 피크 게인 자동 스케일링 기능을 갖추고 있습니다.
* **Scoped Storage 지원:** Storage Access Framework (SAF)를 네이티브로 처리하여 사용자가 저장 디렉토리를 자유롭게 지정할 수 있습니다.
* **네이티브 공유:** `FileProvider`를 통해 WeChat, QQ, WhatsApp, Telegram 등으로 원클릭 안전 파일 공유가 가능합니다.

```text
로컬 녹음 ──► Cloudflare Worker (Gemini STT) ──► 텍스트 ──► Cloudflare Worker (Gemini LLM) ──► 요약
