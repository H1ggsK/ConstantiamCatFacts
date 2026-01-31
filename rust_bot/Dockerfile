FROM rust:1.85-slim as builder
WORKDIR /app

RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    git \
    curl \
    && rustup toolchain install nightly \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN cargo +nightly build --release

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update && apt-get install -y \
    ca-certificates \
    openssl \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/cat_bot .
CMD ["./cat_bot"]