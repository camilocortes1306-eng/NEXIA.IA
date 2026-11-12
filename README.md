# NEXIA AI v0.3 — prototipo de desarrollo

## Incluye
- UI Android con Compose, chat y Google Hub heredados de v0.2.
- Orquestador híbrido: petición HTTPS al backend configurado, con fallback explícito a un mensaje de que NO hay modelo local instalado.
- Cliente JSON POST `{ "message": "..." }` y respuesta `{ "reply": "..." }`.
- OAuth Google heredado de v0.2; requiere configuración Google Cloud, consentimiento y permisos. No se ha verificado su ejecución.

## Configuración de IA
1. Crear un backend HTTPS propio con autenticación de usuarios, cuotas y límites de uso. El backend puede llamar a Gemini u OpenAI con una clave guardada SOLO en el servidor.
2. En `app/build.gradle.kts`, reemplazar el valor vacío de `AI_BACKEND_URL` por la URL HTTPS del endpoint propio, por ejemplo `https://tu-dominio.example/api/chat`.
3. Sin backend configurado, el chat explica que aún no hay modelo local. No simula respuestas inteligentes.
4. Nunca poner claves de API en BuildConfig, APK o repositorio. Para producción agregar autenticación de app, rate limiting y validación de mensajes en servidor.

## Google
Los permisos se solicitan individualmente mediante Google Identity Services. Gmail/Calendar/Tasks/Contacts solo muestran autorización: no se implementan operaciones completas. Drive tiene una prueba básica de lectura de metadatos. El acceso a Google Photos, Maps, YouTube, Docs y Sheets NO está implementado todavía. Para datos sensibles, revisar políticas de Google y requerimientos de verificación OAuth.

## Limitaciones
No incluye APK ni modelo LLM local, reconocimiento de voz, generación de imágenes o análisis real de documentos. No compilado ni probado en Android Studio. Requiere JDK 17, Android SDK 35 y Gradle/Android Studio con acceso a dependencias. El proyecto no contiene Gradle Wrapper.
