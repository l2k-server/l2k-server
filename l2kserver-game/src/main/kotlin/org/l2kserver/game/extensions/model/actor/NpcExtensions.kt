package org.l2kserver.game.extensions.model.actor

import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.NpcImpl
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.npc.SpawnedAt

fun NpcTemplate.toNpc(id: Int, position: Position, heading: Heading, spawnedAt: SpawnedAt) = NpcImpl(
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
    ai = this.ai
)
