# Stock Tracker — Technical Documentation

Crypto watchlist app (Midas-style UI) backed by Binance Spot public API. Clean Architecture with strict layer separation.

```
Presentation (ui/)  →  Domain (domain/)  →  Data (data/)
```

- **Domain** — business contracts and models. No Android dependencies.
- **Data** — Binance REST + WebSocket, Room, in-memory cache.
- **Presentation** — Jetpack Compose, ViewModels, UDF.

DI via Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).

---

## Domain

### Models

| Model | Purpose |
|---|---|
| `CryptoAsset` | A tradable crypto pair. `id` = full pair (`BTCUSDT`), `symbol` = base ticker (`BTC`), `quoteAsset` = quote (`USDT`). Carries 24h REST stats. |
| `LivePrice` | Ephemeral WS tick. **Never persisted.** Optional `changePercent24Hr` from `@ticker` stream. |
| `MarketSnapshot` | Cached tickers + pair catalog for search/popular use cases. |
| `CryptoPair` | Trading pair metadata (`symbol`, `baseAsset`, `quoteAsset`). |

Watchlist membership is **not** on `CryptoAsset`. ViewModels derive it from Room.

### Use cases (`domain/usecase/`)

Only where there is real application logic — not thin repository wrappers:

| Use case | Responsibility |
|---|---|
| `SearchAssetsUseCase` | Client-side search scoring + stable-pair resolution |
| `GetPopularCryptosUseCase` | Top-10 by 24h quote volume, deduplicated by base |

ViewModels call `CryptoRepository` directly for watchlist, live prices, and single-asset fetch.

### Repository interface

**`CryptoRepository`** (data access only)
- `getMarketSnapshot()` — cached tickers + pair catalog
- `getAsset(id)` — single-symbol REST
- `getWatchlist()` / `observeLivePrices()` / add / remove

---

## Data Layer

### Binance REST (`BinanceApi`)

Public endpoints, no authentication. Base URL: `https://api.binance.com/api/v3/`

| Endpoint | Weight | Usage |
|---|---|---|
| `GET /ticker/24hr` (all) | 80 (~300 KB) | Search + popular. Cached 20s in `MarketSnapshotCache` |
| `GET /ticker/24hr?symbol=` | 2 | Detail + watchlist refresh |
| `GET /exchangeInfo` | 20 | Symbol catalog. Cached 1h |

All numeric fields in DTOs are `String?`; mappers use `toDoubleOrNull()`.

### `MarketSnapshotCache`

In-memory TTL cache with **single-flight** (`Mutex` + double-check):

- **`allTickers()`** — 20s TTL. Returns stale data on network failure.
- **`symbolMetadata()`** — `Map<symbol, SymbolInfoDto>`
- **`pairsByBaseAsset()`** — `Map<baseAsset, List<SymbolInfoDto>>`

Only `TRADING` symbols from `exchangeInfo` are indexed.

### Binance WebSocket (`BinanceStreamClient`)

Combined stream URL:
```
wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker
```

- Stream names must be **lowercase**
- `@ticker` pushes 24h rolling stats every ~1s (price + change %)
- Implemented as `callbackFlow` over OkHttp `WebSocketListener`
- `awaitClose` closes socket when collector is cancelled
- Abnormal disconnect → `StreamDroppedException` → `retryWhen` with exponential backoff (1s → 30s cap)
- Normal close (code 1000) does not retry

OkHttp replies to Binance server PINGs automatically. Client `pingInterval(30s)` keeps the shared HTTP pool alive.

### Repositories

**`BinanceCryptoRepositoryImpl`**
- `getMarketSnapshot()` — maps in-memory cache data to `MarketSnapshot`
- `getAsset`: direct REST + cached metadata
- Watchlist + live prices unchanged

Search and popular ranking live in use cases, not the repository.

### UI state

- `LoadState<T>` — `Loading` / `Success<T>` / `Error` for Search and Detail screens
- `PriceDisplayUiModel` — pre-formatted price, 24h change, and trend direction; mapped once via `UiMappers`
- `StableAssetUiModel` / `WatchlistStableItemUiModel` / `DetailStableUiModel` — identity and snapshot stats only; no embedded price

### Quote priority (`QuotePriority.kt`)

`USD_STABLE_QUOTE_PRIORITY = [USDT, USDC, FDUSD]`

When BTC trades as BTCUSDT, BTCUSDC, and BTCFDUSD, USDT wins. Prevents duplicate base assets in search/popular.

### Mappers (`BinanceMappers.kt`)

- `Ticker24hDto.toDomain(info?)` — `id` = pair symbol, `symbol` = base, `name` from `AssetNames`
- `CryptoEntity.toDomain()` — identity only, prices = 0 (filled by VM merge)
- `CryptoAsset.toEntity()` — id, symbol, name only

### Room (`data/local/`)

- **`CryptoEntity`** — watchlist table. Stores id, symbol, name, addedAt. **No prices.**
- **`CryptoDao`** — `observeWatchlist()` Flow, insert (REPLACE on conflict), delete
- **`MidasDatabase`** — `midas_binance.db`

### `AssetNames.kt`

Static map of ~80 base tickers → friendly names (BTC → Bitcoin). Unknown bases fall back to the ticker.

---

## Presentation Layer

### UDF pattern

```
User event → ViewModel → StateFlow → Composable
```

Screens split into stateful entry (hiltViewModel + collectAsStateWithLifecycle) and stateless content (previewable).

### Search (`SearchViewModel`)

Two separate exposed flows — stable list and volatile prices are never merged in the VM:

- `uiState` — query, popular/search flag, `LoadState<List<StableAssetUiModel>>` (updates on snapshot + watchlist changes only)
- `livePrices` — `Map<String, PriceDisplayUiModel>` (updates on WS ticks; re-seeded from REST on snapshot change)

- Empty query → `GetPopularCryptosUseCase`
- Non-empty → debounced 350ms → `SearchAssetsUseCase`
- WS: `flatMapLatest` on visible id set; ticks update `livePrices` entry-by-entry

### Watchlist (`WatchlistViewModel`)

- `uiState` — filtered `List<WatchlistStableItemUiModel>`, empty/banner flags (updates on Room watchlist + local query)
- `livePrices` — `Map<String, PriceDisplayUiModel>` (updates on WS ticks and per-row REST quote fetch)

- WS restarts when watchlist ids change (`flatMapLatest`)
- New ids trigger one-shot `getAsset`; removed ids evicted from quote cache and price map
- Periodic REST refresh every 60s (high/low/volume/VWAP safety net)

### Detail (`DetailViewModel`)

- `uiState` — nav identity, `LoadState<DetailStableUiModel>`, watchlist flag (updates on REST snapshot)
- `livePrices` — single-entry map keyed by asset id (updates on WS ticks)

- Nav args pass `id`, `symbol`, `name` for instant top bar (no flicker)
- Single WS subscription for the asset id
- REST snapshot on load + every 60s
- Composables join stable content with `livePrices[id]` for the price block

### UI models (`AssetUiModel.kt`)

Stable and volatile models are composed at the screen layer, not in ViewModels:
- `formatUsd()` — scale-aware ($67,401.23 vs $0.1642)
- `formatChangePercent()` — Turkish locale (`%2,45`, `-%1,20`)
- `formatLargeUsd()` — K/M/B/T suffixes

`AnimatedPrice` — green/red flash on price change (skips first value to avoid load flash).

### Navigation

- Bottom tabs: Watchlist (start), Search
- Detail: `detail/{id}?symbol={symbol}&name={name}` — push route, bottom bar hidden
- IDs URL-encoded via `Uri.encode`

### Theme

- Custom Midas palette (Material You disabled)
- Dark/light toggle via DataStore (`is_dark_theme`)
- `ThemeViewModel` uses `SharingStarted.Eagerly` to avoid theme flash on cold start
- `avatarPalette` — deterministic color from symbol hash

---

## DI Modules

| Module | Provides |
|---|---|
| `NetworkModule` | OkHttpClient, Retrofit, Gson, `BinanceApi`, `@Named("binance_stream_url")` |
| `DatabaseModule` | `MidasDatabase`, `CryptoDao` |
| `DataStoreModule` | `DataStore<Preferences>` (`midas_settings`) |
| `RepositoryModule` | `BinanceCryptoRepositoryImpl`, `BinancePopularCryptoRepositoryImpl` |

---

## Logcat Tags

| Tag | Layer |
|---|---|
| `OkHttp` | HTTP requests (debug builds, BASIC level) |
| `MarketCache` | Ticker / exchangeInfo cache refresh |
| `BinanceWS` | WebSocket open, reconnect, parse errors |
| `BinanceCryptoRepo` | Search / getAsset |
| `BinancePopularRepo` | Popular list |
| `SearchVM` | Search + popular load |
| `WatchlistVM` | Live prices + quote fetch |
| `DetailVM` | Asset load + live tick |

Filter: `tag:BinanceWS | tag:MarketCache | tag:SearchVM | tag:WatchlistVM`

---

## Design Rules

1. **Live prices never go to Room** — WS ticks merged in ViewModels via separate `livePrices` flows.
2. **Watchlist SSOT** — Room flow drives bookmark icon; not stored on domain model.
3. **`id` = trading pair** — same identifier for REST, WS, Room, and navigation.
4. **Cache before search** — one bulk ticker fetch powers search + popular; avoids rate limits.
5. **WS lifecycle tied to collection** — `callbackFlow` + `awaitClose`; ViewModels use `flatMapLatest` when symbol set changes.
6. **Fail-soft cache** — network errors return stale snapshot, not crash.

---

## Known Trade-offs

- No Binance search API — search is client-side over cached catalog; very large result sets are capped at 30.
- `AssetNames` is partial — obscure tokens show ticker as name.
- Popular list is volume-based, not curated trending.
- Multiple stable pairs for same base exist on Binance; USDT priority is a product choice, not market truth.
- 60s REST refresh on watchlist/detail is a safety net; `@ticker` already carries live price and change %.
