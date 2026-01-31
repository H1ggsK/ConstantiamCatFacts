# Build Stage
FROM rust:1.85-slim as builder
WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    git \
    curl \
    && rustup toolchain install nightly-2024-10-01 \
    && rm -rf /var/lib/apt/lists/*

COPY . .

ENV CARGO_NET_GIT_FETCH_WITH_CLI=true

RUN cargo +nightly-2024-10-01 build --release

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update && apt-get install -y \
    ca-certificates \
    openssl \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/cat_bot .
CMD ["./cat_bot"]