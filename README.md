# Crypto Tracker Application

An Android application built for the Binance Spot public API. This project implements a cached market catalog, real-time price streaming, and a responsive Jetpack Compose user interface for discovering and tracking cryptocurrency assets.

## Architecture and Design Patterns

The application follows Clean Architecture principles, separating concerns into three distinct layers:

* **Data Layer:** Orchestrates data between the Binance REST API, WebSocket streams, and a local persistent store. It implements a fail-soft caching strategy for the market catalog and keeps live prices ephemeral.
* **Domain Layer:** Contains pure Kotlin model definitions and a `CryptoRepository` interface. ViewModels depend directly on the repository — no UseCase layer is used, as the repository methods map 1-to-1 to domain operations without additional logic to encapsulate.
* **Presentation Layer:** Utilizes a strict Unidirectional Data Flow (UDF) to manage UI state and user interactions.

## Technical Implementation Details

### Networking and Market Catalog

* **Binance REST Integration:** The app consumes Binance Spot public endpoints without authentication. `GET /ticker/24hr` powers both search and popular-asset discovery.
* **Room-Backed Cache:** The market catalog is persisted in Room (`market_assets`). A 1-hour Time-to-Live (TTL) policy is tracked in DataStore (`market_last_fetched_at`). Before triggering a network refresh, the repository evaluates cache age and only fetches when stale or empty.
* **SQL-Level Filtering:** Popular asset and search queries are handled entirely in the DAO layer via Room `@Query`. Search results are scored and ranked directly in SQL using a `CASE`-weighted `ORDER BY`, avoiding in-memory filtering in the ViewModel.
* **Watchlist Price Enrichment:** The `watchlist` table stores only identity fields (`id`, `symbol`, `name`). When the repository emits the watchlist, each entry is cross-referenced against `market_assets` via `getById()` to embed the latest cached price, avoiding per-item REST calls.

### Real-Time Price Streaming

* **WebSocket Combined Streams:** Live prices are delivered through Binance combined `@ticker` streams (`wss://stream.binance.com:9443/stream`).
* **Collection-Bound Lifecycle:** `BinanceStreamClient` is implemented as a `callbackFlow` over OkHttp's `WebSocketListener`. The socket is closed in `awaitClose` when the Flow collector is cancelled, preventing leaked connections.
* **Reconnection:** Exponential backoff (1s → 30s cap).

### Search

* **Server-Side Search Scoring:** There is no Binance search endpoint. `MarketAssetDao.searchAssets()` scores matches over the cached catalog using a SQL `CASE` expression — exact symbol match scores highest, followed by prefix match, name contains, and symbol contains. Results are capped at 30.
* **Popular Assets:** `MarketAssetDao.getPopularAssets()` returns the top 10 assets by 24h quote volume filtered with `WHERE volumeUsd24Hr > 0`.
* **Debounced Input:** The search query is debounced at 350ms in `SearchViewModel` before triggering a DAO query, reducing unnecessary database reads while typing.

### UI and State Management

* **Unidirectional Data Flow (UDF):** ViewModels expose immutable state via `StateFlow`, while UI components remain stateless and communicate user intents through typed event contracts (`SearchEvent`, `WatchlistEvent`, `DetailEvent`).
* **Dual-Flow Price Architecture:** Stable list structure and volatile prices are kept in separate exposed flows (`uiState` + `livePrices`). `uiState` drives structural recomposition (list items, names, icons). `livePrices` is passed as a `State<Map<...>>` reference — not a value — so only the `PriceColumn` leaf composable reads `.value` and recomposes on each WebSocket tick. The rest of the screen tree is unaffected.
* **`@Immutable` on UI Models:** All UiState and UiModel data classes are annotated with `@Immutable`, allowing Compose's compiler to skip recomposition when parameters haven't changed structurally.
* **Deferred State Reads:** `livePrices` is collected at the screen root but passed as a `State<T>` wrapper (not unwrapped with `by`) all the way down to the leaf composable that needs it. This defers the `.value` read and limits recomposition scope to only that composable.
* **`flatMapLatest` for WebSocket Management:** When the watchlist or displayed asset list changes, `flatMapLatest` cancels the previous WebSocket subscription and opens a new one for the current set of IDs. Only one WebSocket connection is ever open at a time.

## Technology Stack

* **UI:** Jetpack Compose, Material 3
* **Asynchrony:** Kotlin Coroutines & Flow
* **Dependency Injection:** Dagger Hilt
* **Database:** Room
* **Networking:** Retrofit & OkHttp (REST + WebSocket)
* **Storage:** Jetpack DataStore
* **Navigation:** Compose Navigation
* **Serialization:** Gson
* **Testing:** JUnit 4, MockK, kotlinx-coroutines-test, Turbine

## Prerequisites

* **Android Studio:** Ladybug (2024.2.1) or newer.
* **JDK:** Version 11.
* **Android SDK:** API Level 36 (Android 16) is required for the build environment.
