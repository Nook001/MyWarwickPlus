package uk.ac.warwick.plus.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

// ViewModel describes messages without retaining Context or eagerly resolving a language.
sealed interface UiText {
    data class Literal(val value: String) : UiText
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Quantity(@param:PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
}

internal fun text(@StringRes id: Int, vararg args: Any): UiText = UiText.Resource(id, args.toList())

fun UiText.resolve(resources: Resources): String {
    fun arguments(args: List<Any>) = args.map { if (it is UiText) it.resolve(resources) else it }.toTypedArray()
    return when (this) {
        is UiText.Literal -> value
        is UiText.Resource -> resources.getString(id, *arguments(args))
        is UiText.Quantity -> resources.getQuantityString(id, count, *arguments(args))
    }
}

@Composable
internal fun UiText.render(): String = resolve(LocalResources.current)
