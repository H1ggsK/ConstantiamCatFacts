use azalea::prelude::*;
use azalea::{Account, Client, Event};
use sqlx::sqlite::SqlitePool;
use std::env;
use std::sync::Arc;
use tokio::sync::Mutex;
use anyhow::Result;

#[derive(Clone, Component, Default)]
struct State {
    msg_count: Arc<Mutex<u32>>,
    last_msg: Arc<Mutex<String>>, // Added for deduplication
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

    let account = Account::microsoft(&email).await?;
    azalea::ClientBuilder::new()
        .set_handler(handle)
        .set_state(bot_state)
        .start(account, server_addr_str.as_str())
        .await?;
        
    Ok(())
}

async fn handle(bot: Client, event: Event, state: State) -> Result<()> {
    if let Event::Chat(chat_packet) = event {
        let msg = chat_packet.message().to_string();

        // 1. Deduplicate: ignore if same as last message
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
            println!("Progress: {}/{}", *count, state.msg_threshold);
        }

        if *count >= state.msg_threshold {
            *count = 0;
            if let Some(pool) = &state.db_pool {
                let row: Option<(String,)> = sqlx::query_as("SELECT text FROM facts WHERE status='approved' ORDER BY RANDOM() LIMIT 1")
                    .fetch_optional(pool)
                    .await?;

                if let Some((fact,)) = row {
                    let _ = bot.chat(&format!("Cat Fact: {}", fact));
                }
            }
        }
    }
    Ok(())
}