import discord
from discord.ext import commands, tasks
import os
import aiosqlite
from db import DB_PATH

intents = discord.Intents.default()
intents.message_content = True
intents.members = True 
bot = commands.Bot(command_prefix="!", intents=intents)

APPROVE_CHANNEL_ID = int(os.getenv("DISCORD_APPROVE_CHANNEL", "0"))
ACCEPTED_CHANNEL_ID = int(os.getenv("DISCORD_ACCEPTED_CHANNEL", "0"))
ADMIN_ROLE_ID = int(os.getenv("ADMIN_ROLE_ID", 0))

class ApprovalView(discord.ui.View):
    def __init__(self, fact_id, fact_text, author_name):
        super().__init__(timeout=None)
        self.fact_id = fact_id
        self.fact_text = fact_text
        self.author_name = author_name

    @discord.ui.button(label="Approve", style=discord.ButtonStyle.green, custom_id="approve_btn")
    async def approve(self, interaction: discord.Interaction, button: discord.ui.Button):
        async with aiosqlite.connect(DB_PATH) as db:
            await db.execute("UPDATE facts SET status='approved' WHERE id=?", (self.fact_id,))
            await db.commit()
        
        public_channel = bot.get_channel(ACCEPTED_CHANNEL_ID)
        if public_channel:
            embed = discord.Embed(
                title="✨ New Cat Fact Approved",
                description=self.fact_text,
                color=0x2ecc71
            )
            embed.set_footer(text=f"Submitted by {self.author_name}")
            await public_channel.send(embed=embed)

        for child in self.children:
            child.disabled = True
        await interaction.response.edit_message(content=f"**Fact #{self.fact_id} Approved & Posted**", view=self)

    @discord.ui.button(label="Deny", style=discord.ButtonStyle.red, custom_id="deny_btn")
    async def deny(self, interaction: discord.Interaction, button: discord.ui.Button):
        async with aiosqlite.connect(DB_PATH) as db:
            await db.execute("DELETE FROM facts WHERE id=?", (self.fact_id,))
            await db.commit()
            
        for child in self.children:
            child.disabled = True
        await interaction.response.edit_message(content=f"**Fact #{self.fact_id} Denied**", view=self)

@tasks.loop(seconds=60)
async def check_web_submissions():
    await bot.wait_until_ready()
    channel = bot.get_channel(APPROVE_CHANNEL_ID)
    if not channel: return

    async with aiosqlite.connect(DB_PATH) as db:
        async with db.execute("SELECT id, text, author FROM facts WHERE status='pending'") as cursor:
            pending_facts = await cursor.fetchall()

        for fact_id, text, author in pending_facts:
            embed = discord.Embed(title="Web Submission", description=text, color=0x3498db)
            embed.set_footer(text=f"Author: {author} | ID: {fact_id}")
            
            await channel.send(embed=embed, view=ApprovalView(fact_id, text, author))
            await db.execute("UPDATE facts SET status='voting' WHERE id=?", (fact_id,))
        
        if pending_facts:
            await db.commit()

@bot.event
async def on_ready():
    print(f"Logged in as {bot.user}")
    if not check_web_submissions.is_running():
        check_web_submissions.start()

@bot.command(name="suggest")
async def suggest(ctx, *, fact: str):
    """Usage: !suggest [cat fact]"""
    try:
        await ctx.message.delete()
    except:
        pass

    async with aiosqlite.connect(DB_PATH) as db:
        cursor = await db.execute(
            "INSERT INTO facts (text, author, status) VALUES (?, ?, 'voting')", 
            (fact, ctx.author.display_name)
        )
        await db.commit()
        fact_id = cursor.lastrowid

    user_pfp = ctx.author.display_avatar.url
    confirm_embed = discord.Embed(
        title="Fact Submission Logged",
        description=f"**Fact:** {fact}",
        color=0xf1c40f
    )
    confirm_embed.set_author(name=ctx.author.display_name, icon_url=user_pfp)
    confirm_embed.set_footer(text=f"ID: {fact_id} | Status: Pending Review")
    
    await ctx.send(embed=confirm_embed)

    staff_channel = bot.get_channel(APPROVE_CHANNEL_ID)
    if staff_channel:
        staff_embed = discord.Embed(title="Discord Submission", description=fact, color=0xe67e22)
        staff_embed.set_author(name=ctx.author.display_name, icon_url=user_pfp)
        staff_embed.set_footer(text=f"Author ID: {ctx.author.id} | Fact ID: {fact_id}")
        await staff_channel.send(embed=staff_embed, view=ApprovalView(fact_id, fact, ctx.author.display_name))

@bot.command(name="catfact")
async def catfact(ctx):
    async with aiosqlite.connect(DB_PATH) as db:
        async with db.execute("SELECT text FROM facts WHERE status='approved' ORDER BY RANDOM() LIMIT 1") as cursor:
            row = await cursor.fetchone()
            if row: await ctx.send(row[0])
            else: await ctx.send("No approved cat facts found.")

@bot.command()
async def play_sfx(ctx, name: str):
    if name not in ["meow", "click"]: return
    if not ctx.voice_client:
        if ctx.author.voice: await ctx.author.voice.channel.connect()
        else: return
    try:
        source = await discord.FFmpegOpusAudio.from_probe(f"/data/{name}.opus")
        ctx.voice_client.play(source)
    except Exception as e:
        print(f"SFX Error: {e}")

@bot.command(name="watching")
async def watching(ctx, *, text: str):
    """Sets the bot status to 'Watching <text>'."""
    if not any(role.id == ADMIN_ROLE_ID for role in ctx.author.roles):
        await ctx.send("You don't have permission to use this command.")
        return
    activity = discord.Activity(type=discord.ActivityType.watching, name=text)
    await bot.change_presence(activity=activity)
    await ctx.send(f"👀 Now watching **{text}**")

@bot.command(name="stopwatching")
async def stopwatching(ctx):
    """Resets the bot status."""
    if not any(role.id == ADMIN_ROLE_ID for role in ctx.author.roles):
        await ctx.send("You don't have permission to use this command.")
        return
    await bot.change_presence(activity=None)
    await ctx.send("Stopped watching everything.")

bot.run(os.getenv("DISCORD_TOKEN", ""))