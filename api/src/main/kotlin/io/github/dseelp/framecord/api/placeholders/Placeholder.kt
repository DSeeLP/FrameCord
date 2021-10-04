package io.github.dseelp.framecord.api.placeholders

data class Placeholder(val type: PlaceholderType, val name: String, val factory: (PlaceholderContext) -> Any?)