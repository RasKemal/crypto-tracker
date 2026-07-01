package com.example.stocktracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

@Immutable
sealed interface UiMessage {
    @Immutable
    data class Resource(@param:StringRes val resId: Int) : UiMessage

    @Immutable
    data class Text(val value: String) : UiMessage
}

fun Throwable.toUiMessage(@StringRes fallbackRes: Int): UiMessage =
    UiMessage.Resource(fallbackRes)

@Composable
fun UiMessage.asString(): String = when (this) {
    is UiMessage.Resource -> stringResource(resId)
    is UiMessage.Text -> value
}
