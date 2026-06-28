package com.example.stocktracker.data.mapper

import com.example.stocktracker.data.local.entity.StockEntity
import com.example.stocktracker.data.remote.dto.BasicFinancialsDto
import com.example.stocktracker.data.remote.dto.CompanyProfileDto
import com.example.stocktracker.data.remote.dto.StockQuoteDto
import com.example.stocktracker.data.remote.dto.StockSearchResultDto
import com.example.stocktracker.domain.model.BasicFinancials
import com.example.stocktracker.domain.model.CompanyProfile
import com.example.stocktracker.domain.model.Stock
import com.example.stocktracker.domain.model.StockQuote

fun StockSearchResultDto.toDomain(isInWatchlist: Boolean = false): Stock = Stock(
    symbol = symbol,
    displaySymbol = displaySymbol,
    description = description,
    type = type,
    exchange = primaryExchange.orEmpty(),
    isInWatchlist = isInWatchlist,
)

fun StockQuoteDto.toDomain(symbol: String): StockQuote = StockQuote(
    symbol = symbol,
    currentPrice = currentPrice,
    change = change,
    changePercent = changePercent,
    highPrice = highPrice,
    lowPrice = lowPrice,
    openPrice = openPrice,
    previousClose = previousClose,
)

fun CompanyProfileDto.toDomain(symbol: String): CompanyProfile = CompanyProfile(
    symbol = ticker ?: symbol,
    name = name.orEmpty(),
    country = country.orEmpty(),
    currency = currency.orEmpty(),
    exchange = exchange.orEmpty(),
    ipo = ipo.orEmpty(),
    marketCap = marketCap,
    sharesOutstanding = sharesOutstanding,
    webUrl = webUrl.orEmpty(),
    logoUrl = logoUrl.orEmpty(),
    industry = industry.orEmpty(),
)

fun BasicFinancialsDto.toDomain(): BasicFinancials = BasicFinancials(
    symbol = symbol.orEmpty(),
    high52Week = metric?.high52Week,
    low52Week = metric?.low52Week,
    peRatio = metric?.peRatio,
    beta = metric?.beta,
    dividendYield = metric?.dividendYield,
    avgVolume10Day = metric?.avgVolume10Day,
)

fun StockEntity.toDomain(): Stock = Stock(
    symbol = symbol,
    displaySymbol = displaySymbol,
    description = description,
    type = type,
    exchange = exchange,
    isInWatchlist = true,
)

fun Stock.toEntity(): StockEntity = StockEntity(
    symbol = symbol,
    displaySymbol = displaySymbol,
    description = description,
    type = type,
    exchange = exchange,
)
