use azalea::prelude::*;
use azalea::{Account, Client, Event};
use sqlx::sqlite::SqlitePool;
use std::env;
use std::sync::Arc;
use tokio::sync::Mutex;
use anyhow::Result;
use std::time::Duration;
use std::path::PathBuf;

#[derive(Clone, Component, Default)]
struct State {
    msg_count: Arc<Mutex<u32>>,
    last_msg: Arc<Mutex<String>>,
    msg_threshold: u32,
    db_pool: Option<SqlitePool>,
}

#[tokio::main]
async fn main() -> Result<()> {
    let _ = dotenv::dotenv();
    let db_url = "sqlite:////data/catfacts.db";
    let pool = SqlitePool::connect(db_url).await.map_err(|e| anyhow::anyhow!(e))?;

    let threshold = env::var("CHAT_INTERVAL")
        .unwrap_or_else(|_| "1000".to_string())
        .parse()
        .unwrap();

    let bot_state = State {
        msg_count: Arc::new(Mutex::new(0)),
        last_msg: Arc::new(Mutex::new(String::new())),
        msg_threshold: threshold,
        db_pool: Some(pool),
    };

    let email = env::var("MC_EMAIL").expect("MC_EMAIL required");
    let server_addr_str = env::var("MC_SERVER_IP").expect("MC_SERVER_IP required");

    loop {
        println!("Authenticating as {}...", email);

        let account_result = azalea::Account::microsoft_with_opts(
            &email,
            azalea::AuthOpts {
                cache_file: Some(PathBuf::from("/data/auth.json")),
                ..Default::default()
            }
        ).await;

        match account_result {
            Ok(account) => {
                println!("Connecting to {}...", server_addr_str);
                
                let result = azalea::ClientBuilder::new()
                    .set_handler(handle)
                    .set_state(bot_state.clone())
                    .start(account, server_addr_str.as_str())
                    .await;

                if let Err(e) = result {
                    println!("❌ Bot disconnected with error: {:?}", e);
                } else {
                    println!("⚠️ Bot disconnected normally.");
                }
            }
            Err(e) => {
                println!("❌ Authentication failed: {:?}", e);
            }
        }

        println!("⏳ Waiting 30 seconds before reconnecting...");
        tokio::time::sleep(Duration::from_secs(30)).await;
        println!("♻️ Restarting loop...");
    }
}

async fn handle(bot: Client, event: Event, state: State) -> Result<()> {
    if let Event::Chat(chat_packet) = event {
        let msg = chat_packet.message().to_string();

        {
            let mut last = state.last_msg.lock().await;
            if msg == *last { return Ok(()); }
            *last = msg.clone();
        }

        println!("[CHAT] {}", msg);

        if msg.starts_with("&d") || msg.starts_with("&5") {
            return Ok(());
        }

        let mut count = state.msg_count.lock().await;
        *count += 1;

        if *count % 50 == 0 {
            println!("📊 Progress: {}/{}", *count, state.msg_threshold);
        }

        if *count >= state.msg_threshold {
            *count = 0;
            if let Some(pool) = &state.db_pool {
                let row: Option<(String,)> = sqlx::query_as("SELECT text FROM facts WHERE status='approved' ORDER BY RANDOM() LIMIT 1")
                    .fetch_optional(pool)
                    .await
                    .map_err(|e| anyhow::anyhow!(e))?;

                if let Some((fact,)) = row {
                    println!("🐱 Sending Fact: {}", fact);
                    let _ = bot.chat(&format!("Cat Fact: {}", fact));
                }
            }
        }
    }
    Ok(())
}