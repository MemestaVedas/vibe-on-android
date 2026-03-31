package moe.memesta.vibeon.data.model

enum class TransitionMode(val value: String) {
    NONE("none"),
    OVERLAP("overlap");

    companion object {
        fun fromString(value: String): TransitionMode {
            return entries.find { it.value == value } ?: NONE
        }
    }
}

data class TransitionSettings(
    val mode: TransitionMode = TransitionMode.NONE,
    val durationMs: Int = 1200
)
