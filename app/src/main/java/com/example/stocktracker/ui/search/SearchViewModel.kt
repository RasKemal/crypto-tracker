package com.example.stocktracker.ui.search

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.MarketFilter
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.ui.model.StockUiModel
import com.example.stocktracker.ui.model.toSearchUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SearchVM"
// Caps /quote calls per search to stay within the Finnhub free-tier rate limit.
private const val MAX_QUOTES_PER_SEARCH = 15

@Immutable
data class SearchUiState(
    val query: String = "",
    val selectedFilter: MarketFilter = MarketFilter.ABD,
    val stocks: List<StockUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class FilterSelected(val filter: MarketFilter) : SearchEvent
    data class StockClicked(val symbol: String) : SearchEvent
    data class WatchlistToggled(val stock: StockUiModel) : SearchEvent
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    private val _rawQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(MarketFilter.ABD)
    private val _searchResults = MutableStateFlow<List<Stock>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Quote map lives in memory only — never written to Room.
    private val _quotesMap = MutableStateFlow<Map<String, StockQuote>>(emptyMap())

    // Filter is applied here; switching tabs never triggers a new network call.
    val uiState: StateFlow<SearchUiState> = combine(
        _rawQuery, _selectedFilter, _searchResults, _isLoading, _quotesMap,
    ) { query, filter, results, loading, quotes ->
        val filtered = results
            .filter { filter.matches(it.symbol) }
            .mapIndexed { index, stock -> stock.toSearchUiModel(index + 1, quotes[stock.symbol]) }
        SearchUiState(query = query, selectedFilter = filter, stocks = filtered, isLoading = loading, error = _error.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        @OptIn(FlowPreview::class)
        _rawQuery
            .debounce(350)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) performSearch(query)
                else { _searchResults.value = emptyList(); _error.value = null }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged     -> _rawQuery.update { event.query }
            is SearchEvent.FilterSelected   -> _selectedFilter.update { event.filter }
            is SearchEvent.WatchlistToggled -> toggleWatchlist(event.stock)
            is SearchEvent.StockClicked     -> Unit
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.searchStocks(query)
                .onSuccess { stocks ->
                    Log.d(TAG, "searchStocks(\"$query\") → ${stocks.size} results")
                    _searchResults.value = stocks
                    val visible = stocks.take(MAX_QUOTES_PER_SEARCH).map(Stock::symbol).toSet()
                    _quotesMap.update { current -> current.filterKeys { it in visible } }
                    fetchQuotesFor(visible)
                }
                .onFailure { e ->
                    Log.e(TAG, "searchStocks(\"$query\") failed", e)
                    _error.value = e.message ?: "Bir hata oluştu"
                    _searchResults.value = emptyList()
                }
            _isLoading.value = false
        }
    }

    private fun fetchQuotesFor(symbols: Set<String>) {
        val missing = symbols - _quotesMap.value.keys
        missing.forEach { symbol ->
            viewModelScope.launch {
                repository.getStockQuote(symbol)
                    .onSuccess { quote -> _quotesMap.update { it + (symbol to quote) } }
                    .onFailure { Log.w(TAG, "getStockQuote($symbol) failed: ${it.message}") }
            }
        }
    }

    private fun toggleWatchlist(stock: StockUiModel) {
        viewModelScope.launch {
            if (stock.isInWatchlist) {
                repository.removeFromWatchlist(stock.symbol)
            } else {
                repository.addToWatchlist(
                    Stock(stock.symbol, stock.displaySymbol, stock.description, "", "", false)
                )
            }
        }
    }
}
