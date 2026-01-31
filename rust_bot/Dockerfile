FROM rust:1.84.1-slim as builder
WORKDIR /app
RUN apt-get update && apt-get install -y pkg-config libssl-dev && rm -rf /var/lib/apt/lists/*
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update && apt-get install -y ca-certificates openssl libssl-dev && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/release/cat_bot .
CMD ["./cat_bot"]