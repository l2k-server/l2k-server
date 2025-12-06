package org.l2kserver.game.data.npc

import org.l2kserver.game.data.character.classes.HUMAN_FIGHTER
import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.actor.character.CharacterInstance
import org.l2kserver.game.model.actor.npc.NpcRace
import org.l2kserver.game.model.actor.npc.NpcTemplate
import org.l2kserver.game.model.actor.npc.SpawnData
import org.l2kserver.game.model.actor.position.SpawnPosition
import org.l2kserver.game.model.html.HtmlRegistry
import org.l2kserver.game.model.stats.CombatStats

object GrandMasterRoien: NpcTemplate {
    override val id = 1_030_008
    override val name = "Roien"
    override val title = "Grand Master"
    override val level = 70
    override val race = NpcRace.HUMANS
    override val collisionBox = CollisionBox(8.0, 23.5)
    override val stats = CombatStats(
        maxHp = Int.MAX_VALUE,
        maxMp = Int.MAX_VALUE,
        pDef = Int.MAX_VALUE,
        mDef = Int.MAX_VALUE
    )
    override val basicStats = HUMAN_FIGHTER.basicStats
    override val spawn = SpawnData(
        positions = listOf(SpawnPosition(-71384, 258305, -3109, 42000))
    )

    override fun isEnemyOf(other: ActorInstance) = false
    override fun onTalkWith(character: CharacterInstance) = HtmlRegistry.findById("roien001.htm")
}

object GrandMagisterGallint: NpcTemplate {
    override val id = 1_030_017
    override val name = "Gallint"
    override val title = "Grand Magister"
    override val level = 70
    override val race = NpcRace.HUMANS
    override val collisionBox = CollisionBox(8.0, 24.0)
    override val stats = CombatStats(
        maxHp = Int.MAX_VALUE,
        maxMp = Int.MAX_VALUE,
        pDef = Int.MAX_VALUE,
        mDef = Int.MAX_VALUE
    )
    override val basicStats = HUMAN_FIGHTER.basicStats
    override val spawn = SpawnData(
        positions = listOf(SpawnPosition(-91008, 248016, -3568, 6000))
    )

    override fun isEnemyOf(other: ActorInstance) = false
    override fun onTalkWith(character: CharacterInstance) = HtmlRegistry.findById("gallint001.htm")
}
