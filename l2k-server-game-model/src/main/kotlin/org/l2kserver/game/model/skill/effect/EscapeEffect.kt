package org.l2kserver.game.model.skill.effect

/**
 * The effect of teleporting somebody
 */
//TODO ClanHall, Castle,  certain town
data class EscapeEffect(override val targetId: Int): Effect
