package moe.memesta.vibeon.core.presentation

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        val id: Int,
        val args: List<Any> = emptyList()
    ) : UiText
}
