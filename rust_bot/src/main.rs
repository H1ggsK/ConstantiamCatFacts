use azalea::prelude::*;
use azalea::{Account, Client, Event};
use sqlx::sqlite::SqlitePool;
use std::env;
use std::sync::Arc;
use tokio::sync::Mutex;

#[derive(Clone)]
struct State {
    msg_count: Arc<Mutex<u32>>,
    msg_threshold: u32,
    db_pool: SqlitePool,
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let _ = dotenv::dotenv();
    let db_url = "sqlite:////data/catfacts.db";
    let pool = SqlitePool::connect(db_url).await?;

    let threshold = env::var("CHAT_INTERVAL")
        .unwrap_or_else(|_| "100".to_string())
        .parse()
        .unwrap();

    let bot_state = State {
        msg_count: Arc::new(Mutex::new(0)),
        msg_threshold: threshold,
        db_pool: pool,
    };

    let email = env::var("MC_EMAIL").expect("MC_EMAIL required in .env");
    let server_addr = env::var("MC_SERVER_IP").expect("MC_SERVER_IP required");

    let account = Account::microsoft(&email).await.expect("Failed to create Microsoft account");

    azalea::ClientBuilder::new()
        .set_handler(handle)
        .set_state(bot_state)
        .start(&account, server_addr)
        .await?;

    Ok(())
}

async fn handle(mut bot: Client, event: Event, state: State) -> anyhow::Result<()> {
    if let Event::Chat(chat_packet) = event {
        let msg = chat_packet.message().to_string();

        if msg.starts_with("&d") || msg.starts_with("&5") {
            return Ok(());
        }

        let mut count = state.msg_count.lock().await;
        *count += 1;

        if *count >= state.msg_threshold {
            *count = 0;

            let row: Option<(String,)> = sqlx::query_as("SELECT text FROM facts WHERE status='approved' ORDER BY RANDOM() LIMIT 1")        
                .fetch_optional(&state.db_pool)
                .await?;

            if let Some((fact,)) = row {
                bot.chat(&format!("Cat Fact: {}", fact));
            }
        }
    }
    Ok(())
}