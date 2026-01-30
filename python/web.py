from fastapi import FastAPI, Request, Form, Depends
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
import time
from db import init_db, get_db

app = FastAPI()

# Simple in-memory rate limiter (IP: timestamp)
_rate_limit = {}

@app.on_event("startup")
async def startup():
    await init_db()

@app.get("/", response_class=HTMLResponse)
async def home():
    return """
    <!DOCTYPE html>
    <html class="bg-[#111] text-[#eee] font-mono">
    <head>
        <title>Cat Facts Node</title>
        <script src="https://cdn.tailwindcss.com"></script>
    </head>
    <body class="h-screen flex items-center justify-center">
        <div class="w-full max-w-md p-6 border border-[#333] rounded bg-[#161616]">
            <h1 class="text-xl mb-4 text-emerald-400 font-bold tracking-tighter">>> SUBMIT_FACT_PROTOCOL</h1>
            <form action="/submit" method="post" class="space-y-4">
                <div>
                    <label class="block text-xs text-gray-500 mb-1">FACT_PAYLOAD</label>
                    <textarea name="fact" required rows="3" class="w-full bg-[#0a0a0a] border border-[#333] p-2 text-sm focus:border-emerald-500 outline-none"></textarea>
                </div>
                <div>
                    <label class="block text-xs text-gray-500 mb-1">OPERATOR_ALIAS</label>
                    <input type="text" name="author" required class="w-full bg-[#0a0a0a] border border-[#333] p-2 text-sm focus:border-emerald-500 outline-none">
                </div>
                <button type="submit" class="w-full bg-[#222] hover:bg-[#333] border border-[#333] text-emerald-400 py-2 text-sm transition-colors">[ UPLOAD ]</button>
            </form>
        </div>
    </body>
    </html>
    """

@app.post("/submit")
async def submit_fact(request: Request, fact: str = Form(...), author: str = Form(...), db=Depends(get_db)):
    client_ip = request.client.host # type: ignore
    now = time.time()
    
    # 10 Minute Rate Limit (600 seconds)
    if client_ip in _rate_limit and now - _rate_limit[client_ip] < 600:
        return HTMLResponse("<body style='background:#111;color:#f55;font-family:monospace'>ERR: RATE_LIMIT_EXCEEDED. TRY_LATER.</body>", status_code=429)
    
    _rate_limit[client_ip] = now
    
    await db.execute("INSERT INTO facts (text, author, ip) VALUES (?, ?, ?)", (fact, author, client_ip))
    await db.commit()
    return HTMLResponse("<body style='background:#111;color:#5f5;font-family:monospace'>ACK: SUBMISSION_RECEIVED.</body>")