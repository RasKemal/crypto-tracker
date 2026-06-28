package com.example.stocktracker.ui.watchlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.ui.model.WatchlistItemUiModel
import com.example.stocktracker.ui.model.toWatchlistItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class WatchlistUiState(
    val localQuery: String = "",
    val items: List<WatchlistItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

sealed interface WatchlistEvent {
    data class LocalQueryChanged(val query: String) : WatchlistEvent
    data class RemoveStock(val symbol: String) : WatchlistEvent
    data class StockClicked(val symbol: String) : WatchlistEvent
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    private val _localQuery = MutableStateFlow("")

    private val watchlist: StateFlow<List<Stock>> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // flatMapLatest restarts the WebSocket subscription when the symbol set changes.
    // scan accumulates the latest tick per symbol in memory — prices never go to Room.
    private val livePrices: StateFlow<Map<String, LivePrice>> = watchlist
        .map { it.map(Stock::symbol) }
        .distinctUntilChanged()
        .flatMapLatest { symbols ->
            if (symbols.isEmpty()) flowOf(emptyMap())
            else repository.observeLivePrices(symbols)
                .scan(emptyMap<String, LivePrice>()) { acc, tick -> acc + (tick.symbol to tick) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _quotesMap = MutableStateFlow<Map<String, StockQuote>>(emptyMap())

    init {
        watchlist
            .onEach { stocks ->
                stocks.filter { it.symbol !in _quotesMap.value }.forEach { fetchQuote(it.symbol) }
            }
            .launchIn(viewModelScope)
    }

    private fun fetchQuote(symbol: String) {
        viewModelScope.launch {
            repository.getStockQuote(symbol)
                .onSuccess { quote -> _quotesMap.update { it + (symbol to quote) } }
        }
    }

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlist, livePrices, _quotesMap, _localQuery,
    ) { stocks, prices, quotes, query ->
        val filtered = if (query.isBlank()) stocks
        else stocks.filter {
            it.symbol.contains(query, ignoreCase = true) ||
            it.displaySymbol.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
        WatchlistUiState(
            localQuery = query,
            items = filtered.mapIndexed { i, s -> s.toWatchlistItemUiModel(i + 1, prices[s.symbol], quotes[s.symbol]) },
            isLoading = false,
            isEmpty = stocks.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState(isLoading = true))

    fun onEvent(event: WatchlistEvent) {
        when (event) {
            is WatchlistEvent.LocalQueryChanged -> _localQuery.value = event.query
            is WatchlistEvent.RemoveStock       -> removeStock(event.symbol)
            is WatchlistEvent.StockClicked      -> Unit
        }
    }

    private fun removeStock(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
            _quotesMap.update { it - symbol }
        }
    }
}
