# NEXIA AI v0.4 — Backend autenticado + dos proveedores

## Contenido
- `backend/main.py`: FastAPI; verifica ID tokens de Google, emite sesión de 1 hora, limita peticiones y llama a OpenAI Responses o Gemini generateContent (API heredada aún soportada).
- `app/.../auth/GoogleBackendLogin.kt`: Credential Manager para iniciar sesión y canjear token de Google.
- `app/.../ai/SecureChatClient.kt`: cliente de chat autenticado para seleccionar `openai` o `gemini`.
- Se conserva la UI v0.3; **todavía NO se ha cableado la pantalla del chat al nuevo inicio de sesión y cliente**, ni se ha compilado el APK.

## Configuración backend (Python 3.11+)
1. `cd backend && python -m venv .venv` y activa el entorno.
2. `pip install -r requirements.txt`
3. Configura variables de `.env.example` en el entorno; NO subas las claves al repositorio. `SESSION_SECRET` debe ser aleatoria y >=32 caracteres.
4. `uvicorn main:app --host 127.0.0.1 --port 8000` detrás de proxy HTTPS con TLS.
5. Crea OAuth Client ID tipo **Web** para el backend y cliente Android en Google Cloud (package `com.nexia.ai` + SHA-1 de la firma). Habilita Sign-in with Google.
6. En Android pasa la URL HTTPS y el Web Client ID a `GoogleBackendLogin`; tras `signIn()` usa el token en `SecureChatClient.chat()` desde `Dispatchers.IO`.

## Seguridad y limitaciones
- Nunca coloques OPENAI_API_KEY o GEMINI_API_KEY dentro del APK. No uses HTTP en producción.
- El inicio de sesión solo identifica al usuario; NO otorga permisos de Gmail/Drive/Calendar. OAuth de datos de Google requiere consentimiento por scopes y tokens separados.
- El rate limit es por proceso en memoria: sustituir por Redis y cuotas de gasto por usuario antes de producción.
- No hay persistencia ni revocación de sesiones; configurar DB, revocación, auditoría mínima, protección contra abuso, límites monetarios, CORS restringido, política de privacidad y tests.
- La versión local/offline sigue siendo un stub; no se implementan imágenes, documentos ni voz en este backend.
- El backend usa llamadas de texto de una sola interacción: para chat multi-turno agrega historial por usuario con límites y consentimiento.
- El modelo Gemini y OpenAI son configurables por entorno; verifica disponibilidad y precio en tu cuenta.
- No se han ejecutado llamadas reales por falta de credenciales.
