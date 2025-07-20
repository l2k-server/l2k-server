package org.l2kserver.game.model.stats

/**
 * Character's trade and inventory stats
 *
 * @property privateStoreSize How many slots can the character sell/buy/craft in private store
 * @property weightLimit Character's weight limit
 */
data class TradeAndInventoryStats(
    val privateStoreSize: Int = 0,
    val weightLimit: Int = 0
)
