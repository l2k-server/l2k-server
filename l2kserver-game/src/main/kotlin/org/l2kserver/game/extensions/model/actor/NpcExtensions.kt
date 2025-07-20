package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.ai.L2AiScript
import org.l2kserver.game.model.position.Position
import org.l2kserver.game.model.actor.Npc
import org.l2kserver.game.domain.npc.NpcTemplate
import org.l2kserver.game.model.position.Heading
import org.l2kserver.game.model.actor.SpawnedAt
import org.l2kserver.game.model.actor.enumeration.MoveType

fun NpcTemplate.toNpc(id: Int, position: Position, heading: Heading, spawnedAt: SpawnedAt) = Npc(
    id = id,
    name = this.name,
    templateId = this.id,
    title = this.title,
    level = this.level,
    isEnemy = this.isEnemy,
    race = this.race,
    position = position,
    heading = heading,
    stats = this.stats,
    reward = this.reward,
    spawnedAt = spawnedAt,
    replicas = this.replicas,
    collisionBox = this.collisionBox,
    currentHp = this.stats.maxHp,
    currentMp = this.stats.maxMp,
    moveType = MoveType.WALK,
    weaponType = this.weaponType,
    hasShield = this.hasShield,
    ai = this.aiType?.let { L2AiScript.get(it) }
)
