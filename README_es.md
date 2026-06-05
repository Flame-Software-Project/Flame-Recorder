[English](./README.md) | [简体中文](./README_zh_hans.md) | [繁體中文](./README_zh_hant.md) | [日本語](./README_jp.md) | [한국어](./README_ko.md) | [Русский](./README_ru.md) | [Español](./README_es.md)

---

# Flame Recorder

**Flame Recorder** es una aplicación de grabación de voz ligera, segura y enfocada en la privacidad para Android (minSdk 31+). Al aprovechar una arquitectura serverless sin costo con Cloudflare Workers, ofrece transcripción de voz a texto (STT) y resúmenes (LLM) de alto rendimiento basados en Google Gemini, manteniendo las claves API del desarrollador 100% seguras y ocultas del lado del cliente.

* **Ultra ligera:** Los APK de producción ocupan menos de **10 MB** gracias a la optimización R8 y la división por ABI.
* **STT y Resúmenes sin costo:** Transcribe y resume audio de forma segura a través de un proxy en Cloudflare Worker que conecta con la capa gratuita de la API multimodal de Google Gemini.
* **Forma de onda fluida:** Animación de onda sincronizada con VSYNC y escalado automático de ganancia de pico para formatos M4A, MP3, WAV y 3GP.
* **Almacenamiento seguro (Scoped Storage):** Gestionado de forma nativa mediante Storage Access Framework (SAF), lo que permite elegir directorios de almacenamiento personalizados.
* **Compartición nativa:** Comparta archivos de forma segura con un solo toque en WeChat, QQ, WhatsApp y Telegram a través de `FileProvider`.

```text
Grabación local ──► Cloudflare Worker (Gemini STT) ──► Texto plano ──► Cloudflare Worker (Gemini LLM) ──► Resumen
