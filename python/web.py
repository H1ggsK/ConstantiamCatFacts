from fastapi import FastAPI, Request, Form, Depends
from fastapi.responses import HTMLResponse, RedirectResponse
import time
import os
from db import init_db, get_db

app = FastAPI()

_rate_limit = {}
DISCORD_INVITE = os.getenv("DISCORD_INVITE_URL", "https://discord.gg/")

@app.on_event("startup")
async def startup():
    await init_db()

@app.get("/discord")
async def discord_redirect():
    return RedirectResponse(DISCORD_INVITE)

@app.get("/", response_class=HTMLResponse)
async def home():
    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Cat Facts | Submit</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet">
        <style>body {{ font-family: 'Inter', sans-serif; }}</style>
    </head>
    <body class="bg-[#0f1115] text-slate-200 min-h-screen flex items-center justify-center p-4">
        <div class="w-full max-w-md bg-[#1a1d23] border border-slate-800 rounded-2xl shadow-2xl p-8 relative">
            <a href="/discord" target="_blank" class="absolute top-4 right-4 text-slate-500 hover:text-[#5865F2] transition-colors">
                <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.419-2.1568 2.419zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.419-2.1568 2.419z"/>
                </svg>
            </a>
            <header class="mb-8 text-center">
                <h1 class="text-2xl font-semibold text-white mb-2">Submit a Cat Fact</h1>
                <p class="text-slate-400 text-sm">Contribute to the global cat knowledge base.</p>
            </header>
            
            <form action="/submit" method="post" class="space-y-6">
                <div>
                    <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">The Fact</label>
                    <textarea name="fact" required rows="4" 
                        placeholder="Did you know cats have 32 muscles in each ear?"
                        class="w-full bg-[#111318] border border-slate-800 rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all resize-none"></textarea>
                </div>
                
                <div>
                    <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">Your Name</label>
                    <input type="text" name="author" required placeholder="Anonymous Cat Lover"
                        class="w-full bg-[#111318] border border-slate-800 rounded-xl p-3 text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all">
                </div>
                
                <button type="submit" 
                    class="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-3 rounded-xl transition-all shadow-lg shadow-indigo-500/20">
                    Submit Fact
                </button>
            </form>
            
            <div class="mt-6 pt-6 border-t border-slate-800 text-center space-y-3">
                <a href="/discord" target="_blank" 
                   class="block w-full bg-[#5865F2] hover:bg-[#4752C4] text-white font-semibold py-2 rounded-xl transition-all text-sm">
                   Join our Discord
                </a>
                <a href="/fact" class="block text-xs text-slate-500 hover:text-indigo-400 transition-colors">
                    View Random Fact (JSON API)
                </a>
            </div>
        </div>
    </body>
    </html>
    """

@app.get("/fact")
async def get_random_fact(db=Depends(get_db)):
    async with db.execute("SELECT text FROM facts WHERE status='approved' ORDER BY RANDOM() LIMIT 1") as cursor:
        row = await cursor.fetchone()
    return {"data": [row["text"]]} if row else {"data": ["No approved facts available."]}

@app.post("/submit")
async def submit_fact(request: Request, fact: str = Form(...), author: str = Form(...), db=Depends(get_db)):
    client_ip = request.client.host
    now = time.time()
    
    if client_ip in _rate_limit and now - _rate_limit[client_ip] < 600:
        return HTMLResponse(content="Rate limit exceeded. Please try again in 10 minutes.", status_code=429)
    
    _rate_limit[client_ip] = now
    await db.execute("INSERT INTO facts (text, author, status) VALUES (?, ?, 'pending')", (fact, author))
    await db.commit()
    
    return HTMLResponse(content=f"""
    <body class="bg-[#0f1115] text-slate-200 flex flex-col items-center justify-center h-screen font-sans">
        <div class="text-center bg-[#1a1d23] border border-slate-800 p-10 rounded-2xl shadow-xl">
            <div class="text-indigo-500 text-5xl mb-4">✔</div>
            <h1 class="text-xl font-bold mb-2 text-white">Submission Received</h1>
            <p class="text-slate-400 mb-6 text-sm">Your fact is now in the queue for moderator approval.</p>
            <a href="/" class="text-indigo-400 hover:underline text-sm">← Back to home</a>
        </div>
    </body>
    """)