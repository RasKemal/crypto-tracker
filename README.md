# Crypto Tracker Application

An Android application built for the Binance Spot public API. This project implements a cached market catalog, real-time price streaming, and a responsive Jetpack Compose user interface for discovering and tracking cryptocurrency assets.

## Architecture and Design Patterns

The application follows Clean Architecture principles, separating concerns into three distinct layers:

* **Data Layer:** Orchestrates data between the Binance REST API, WebSocket streams, and a local persistent store. It implements a fail-soft caching strategy for the market catalog and keeps live prices ephemeral.
* **Domain Layer:** Contains pure Kotlin business logic, including UseCases and Repository interfaces.
* **Presentation Layer:** Utilizes a strict Unidirectional Data Flow (UDF) to manage UI state and user interactions.

## Technical Implementation Details

### Networking and Market Catalog

* **Binance REST Integration:** The app consumes Binance Spot public endpoints without authentication. `GET /ticker/24hr` request  powers both search and popular-asset discovery.
* **Room Backed Cache:** The market catalog is persisted in Room (`market_assets`). A 1-hour Time-to-Live (TTL) policy is tracked in DataStore (`market_last_fetched_at`). Before triggering a network refresh, the repository evaluates cache age and only fetches when stale or empty.

### Real-Time Price Streaming

* **WebSocket Combined Streams:** Live prices are delivered through Binance combined `@ticker` streams (`wss://stream.binance.com:9443/stream`). 
* **Collection Bound Lifecycle:** `BinanceStreamClient` is implemented as a `callbackFlow` over OkHttp's `WebSocketListener`. The socket is closed in `awaitClose` when the Flow collector is cancelled, preventing leaked connections.
* **Reconnection:** Exponential backoff (1s → 30s cap)

### Search

* **Client-Side Search Scoring:** There is no Binance search endpoint. `SearchCryptosUseCase` scores matches over the cached catalog.
* **Popular Assets:** `GetPopularCryptosUseCase` returns the top 10 assets by 24h quote volume from the cached catalog.

### UI and State Management

* **Unidirectional Data Flow (UDF):** ViewModels expose immutable state via `StateFlow`, while UI components remain stateless and communicate user intents through typed event contracts.
* **Dual Flow Price:** Stable list identity and volatile prices are kept in separate exposed flows (`uiState` + `livePrices`). ViewModels update price entries individually on WebSocket ticks.

## Technology Stack

* **UI:** Jetpack Compose, Material 3
* **Asynchrony:** Kotlin Coroutines & Flow
* **Dependency Injection:** Dagger Hilt
* **Database:** Room
* **Networking:** Retrofit & OkHttp (REST + WebSocket)
* **Storage:** Jetpack DataStore
* **Navigation:** Compose Navigation
* **Serialization:** Gson
* **Testing:** MockK, JUnit 4, Turbine


## Prerequisites

* **Android Studio:** Ladybug (2024.2.1) or newer.
* **JDK:** Version 11.
* **Android SDK:** API Level 36 (Android 16) is required for the build environment.
