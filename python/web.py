from fastapi import FastAPI, Request, Form, Depends
from fastapi.responses import HTMLResponse
import time
from db import init_db, get_db

app = FastAPI()

_rate_limit = {}

@app.on_event("startup")
async def startup():
    await init_db()

@app.get("/", response_class=HTMLResponse)
async def home():
    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Cat Facts | Submit</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet">
        <style>body { font-family: 'Inter', sans-serif; }</style>
    </head>
    <body class="bg-[#0f1115] text-slate-200 min-h-screen flex items-center justify-center p-4">
        <div class="w-full max-w-md bg-[#1a1d23] border border-slate-800 rounded-2xl shadow-2xl p-8">
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
            
            <footer class="mt-8 pt-6 border-t border-slate-800 text-center">
                <a href="/fact" class="text-xs text-slate-500 hover:text-indigo-400 transition-colors">View Random Fact (JSON API)</a>
            </footer>
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