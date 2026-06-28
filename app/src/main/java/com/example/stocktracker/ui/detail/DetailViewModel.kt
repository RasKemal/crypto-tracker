package com.example.stocktracker.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stocktracker.domain.model.BasicFinancials
import com.example.stocktracker.domain.model.CompanyProfile
import com.example.stocktracker.domain.model.LivePrice
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote
import com.example.stocktracker.domain.repository.StockRepository
import com.example.stocktracker.ui.model.formatChangePercent
import com.example.stocktracker.ui.model.formatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@Immutable
data class DetailUiState(
    val symbol: String = "",
    val displaySymbol: String = "",
    val companyName: String = "",
    val industry: String = "",
    val country: String = "",
    val currency: String = "USD",
    val ipo: String = "",
    val webUrl: String = "",
    val formattedPrice: String = "--",
    val formattedChange: String = "--",
    val formattedChangePercent: String = "--",
    val isPositive: Boolean = true,
    val formattedOpen: String = "--",
    val formattedHigh: String = "--",
    val formattedLow: String = "--",
    val formattedPrevClose: String = "--",
    val formatted52WHigh: String = "--",
    val formatted52WLow: String = "--",
    val formattedPeRatio: String = "--",
    val formattedBeta: String = "--",
    val formattedDividendYield: String = "--",
    val formattedMarketCap: String = "--",
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface DetailEvent {
    data object ToggleWatchlist : DetailEvent
    data object NavigateBack : DetailEvent
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StockRepository,
) : ViewModel() {

    val symbol: String = checkNotNull(savedStateHandle["symbol"])

    private val _quote = MutableStateFlow<StockQuote?>(null)
    private val _profile = MutableStateFlow<CompanyProfile?>(null)
    private val _financials = MutableStateFlow<BasicFinancials?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    private val livePriceFlow = repository.observeLivePrices(listOf(symbol))
        .map<LivePrice, LivePrice?> { it }
        .onStart { emit(null) }

    private val isInWatchlistFlow = repository.getWatchlist()
        .map { stocks -> stocks.any { it.symbol == symbol } }
        .onStart { emit(false) }

    val uiState: StateFlow<DetailUiState> = combine(
        _quote, _profile, _financials, livePriceFlow, isInWatchlistFlow,
    ) { quote, profile, financials, livePrice, inWatchlist ->
        buildUiState(quote, profile, financials, livePrice, inWatchlist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState(symbol = symbol, isLoading = true))

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch { repository.getStockQuote(symbol).onSuccess { _quote.value = it }.onFailure { _error.value = it.message } }
            launch { repository.getCompanyProfile(symbol).onSuccess { _profile.value = it } }
            launch { repository.getBasicFinancials(symbol).onSuccess { _financials.value = it } }
        }.invokeOnCompletion { _isLoading.value = false }
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.ToggleWatchlist -> toggleWatchlist()
            is DetailEvent.NavigateBack    -> Unit
        }
    }

    private fun toggleWatchlist() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.isInWatchlist) {
                repository.removeFromWatchlist(symbol)
            } else {
                repository.addToWatchlist(
                    Stock(symbol, state.displaySymbol.ifBlank { symbol }, state.companyName, "", "", false)
                )
            }
        }
    }

    private fun buildUiState(
        quote: StockQuote?,
        profile: CompanyProfile?,
        financials: BasicFinancials?,
        livePrice: LivePrice?,
        isInWatchlist: Boolean,
    ): DetailUiState {
        val currentPrice = livePrice?.price ?: quote?.currentPrice
        val changePercent: Double? = when {
            livePrice != null && quote != null && quote.previousClose != 0.0 ->
                (livePrice.price - quote.previousClose) / quote.previousClose * 100.0
            else -> quote?.changePercent
        }
        val change: Double? = when {
            livePrice != null && quote != null -> livePrice.price - quote.previousClose
            else -> quote?.change
        }
        return DetailUiState(
            symbol = symbol,
            displaySymbol = profile?.symbol?.ifBlank { symbol } ?: symbol,
            companyName = profile?.name?.ifBlank { symbol } ?: symbol,
            industry = profile?.industry.orEmpty(),
            country = profile?.country.orEmpty(),
            currency = profile?.currency?.ifBlank { "USD" } ?: "USD",
            ipo = profile?.ipo.orEmpty(),
            webUrl = profile?.webUrl.orEmpty(),
            formattedPrice = currentPrice?.formatPrice() ?: "--",
            formattedChange = change?.formatSignedPrice() ?: "--",
            formattedChangePercent = changePercent?.formatChangePercent() ?: "--",
            isPositive = (changePercent ?: 0.0) >= 0.0,
            formattedOpen = quote?.openPrice?.formatPrice() ?: "--",
            formattedHigh = quote?.highPrice?.formatPrice() ?: "--",
            formattedLow = quote?.lowPrice?.formatPrice() ?: "--",
            formattedPrevClose = quote?.previousClose?.formatPrice() ?: "--",
            formatted52WHigh = financials?.high52Week?.formatPrice() ?: "--",
            formatted52WLow = financials?.low52Week?.formatPrice() ?: "--",
            formattedPeRatio = financials?.peRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
            formattedBeta = financials?.beta?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
            formattedDividendYield = financials?.dividendYield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "--",
            formattedMarketCap = profile?.marketCap?.formatMarketCap() ?: "--",
            isInWatchlist = isInWatchlist,
            isLoading = _isLoading.value,
            error = _error.value,
        )
    }
}

private fun Double.formatSignedPrice(): String {
    val prefix = if (this >= 0) "+" else ""
    return "$prefix${String.format(Locale.US, "$%.2f", this)}"
}

private fun Double.formatMarketCap(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "$%.2fT", this / 1_000_000)
    this >= 1_000     -> String.format(Locale.US, "$%.2fB", this / 1_000)
    else              -> String.format(Locale.US, "$%.2fM", this)
}
