"""NEXIA prototype backend. Run only behind HTTPS. Never expose provider keys to Android."""
import os, time, secrets
from collections import defaultdict, deque
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
import httpx

SECRET = os.environ.get("SESSION_SECRET", "")
CLIENT_ID = os.environ.get("GOOGLE_WEB_CLIENT_ID", "")
if len(SECRET) < 32 or not CLIENT_ID:
    raise RuntimeError("Configure SESSION_SECRET (>=32 chars) and GOOGLE_WEB_CLIENT_ID")
serializer = URLSafeTimedSerializer(SECRET, salt="nexia-session-v1")
app = FastAPI(title="NEXIA AI API", docs_url=None, redoc_url=None)
requests_by_user = defaultdict(deque)

class Login(BaseModel):
    id_token: str = Field(min_length=50, max_length=10000)

class Chat(BaseModel):
    message: str = Field(min_length=1, max_length=12000)
    provider: str = Field(pattern="^(openai|gemini)$")

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/auth/google")
def login(payload: Login):
    try:
        claims = id_token.verify_oauth2_token(payload.id_token, google_requests.Request(), CLIENT_ID)
    except (ValueError, Exception):
        raise HTTPException(401, "Token de Google inválido")
    if not claims.get("email_verified") or not claims.get("sub"):
        raise HTTPException(401, "Cuenta no verificada")
    # Use Google's stable sub, never the email as account identifier.
    token = serializer.dumps({"sub": claims["sub"], "nonce": secrets.token_hex(8)})
    return {"access_token": token, "token_type": "bearer", "expires_in": 3600}

def authenticate(authorization: str | None):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Inicia sesión")
    try:
        data = serializer.loads(authorization[7:], max_age=3600)
        return data["sub"]
    except (BadSignature, SignatureExpired, KeyError):
        raise HTTPException(401, "Sesión vencida o inválida")

def limit(sub):
    now = time.monotonic()
    q = requests_by_user[sub]
    while q and now-q[0] > 60: q.popleft()
    if len(q) >= 10: raise HTTPException(429, "Límite: 10 solicitudes por minuto")
    q.append(now)

async def openai_reply(message):
    key = os.environ.get("OPENAI_API_KEY")
    if not key: raise HTTPException(503, "OpenAI sin configurar")
    async with httpx.AsyncClient(timeout=65) as client:
        r = await client.post("https://api.openai.com/v1/responses", headers={"Authorization": f"Bearer {key}"}, json={"model": os.getenv("OPENAI_MODEL", "gpt-4.1-mini"), "input": message, "max_output_tokens": 1200})
        r.raise_for_status()
        data=r.json()
        parts=[part.get("text", "") for item in data.get("output", []) for part in item.get("content", []) if part.get("type")=="output_text"]
        return "\n".join(parts)

async def gemini_reply(message):
    key=os.environ.get("GEMINI_API_KEY")
    if not key: raise HTTPException(503, "Gemini sin configurar")
    # generateContent is supported but legacy; migrate to Interactions API later.
    model=os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    async with httpx.AsyncClient(timeout=65) as client:
        r=await client.post(f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", headers={"x-goog-api-key": key}, json={"contents": [{"parts": [{"text": message}]}], "generationConfig": {"maxOutputTokens": 1200}})
        r.raise_for_status()
        return "\n".join(p.get("text", "") for c in r.json().get("candidates", []) for p in c.get("content", {}).get("parts", []))

@app.post("/chat")
async def chat(payload: Chat, authorization: str | None = Header(default=None)):
    sub=authenticate(authorization)
    limit(sub)
    try:
        reply=await (openai_reply(payload.message) if payload.provider=="openai" else gemini_reply(payload.message))
    except httpx.HTTPStatusError as exc:
        raise HTTPException(502, f"Proveedor no disponible (HTTP {exc.response.status_code})")
    except httpx.RequestError:
        raise HTTPException(504, "Tiempo de espera agotado")
    if not reply: raise HTTPException(502, "El modelo no devolvió texto")
    return {"reply": reply, "provider": payload.provider}
