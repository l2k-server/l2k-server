package org.l2kserver.game.model.stats

enum class Attribute {
    WIND
}

data class AttributeStats(
    val attackAttribute: Pair<Attribute, Int>,
    val defenceAttributes: Map<Attribute, Int>
)
