package org.l2kserver.game.model.actor.character

import org.l2kserver.game.model.actor.ActorInstance
import org.l2kserver.game.model.actor.position.Position
import org.l2kserver.game.model.actor.position.Heading
import org.l2kserver.game.model.actor.MoveType
import org.l2kserver.game.model.actor.Posture
import org.l2kserver.game.model.actor.CollisionBox
import org.l2kserver.game.model.item.ItemInstance
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.CombatStats
import org.l2kserver.game.model.item.WeaponType
import org.l2kserver.game.model.skill.effect.TemporalAbnormalEffect
import org.l2kserver.game.model.skill.instance.SkillInstance

/**
 * Player-controlled character in the game world.
 *
 * This class encapsulates all data for a player character, providing:
 * - Access to core identifiers and attributes (name, gender, race, clan, title);
 * - Current resource stats (CP/HP/MP), experience/level, and reputation (karma, PK/PvP);
 * - World position and heading, movement/combat stats, and targeting information;
 * - Inventory and equipment that influence calculated combat statistics;
 * - Character skills (considering active subclass) and utilities for skill management;
 *
 * It also maintains auxiliary states (PvP, paralysis/posture, auto soulshot/spiritshot)
 * and provides relationship logic (e.g., [isEnemyOf]).
 *
 * Notes on computed properties:
 * - [level] is determined based on [exp];
 * - [stats] are assembled from class base parameters, equipment, and modifiers;
 */
interface PlayerCharacterInstance: ActorInstance {
    override val id: Int
    val accountName: String
    override val name: String
    val title: String

    val characterClass: CharacterClass
    val gender: Gender
    val race: CharacterRace

    val clanId: Int

    val currentCp: Int
    override val currentHp: Int
    override val currentMp: Int

    val sp: Int
    val exp: Long
    val expLostAfterDeath: Long
    val karma: Int
    val pvpCount: Int
    val pkCount: Int

    override val level: Int

    val hairStyle: Int
    val hairColor: Int
    val faceType: Int

    override val moveType: MoveType
    var posture: Posture

    override val position: Position
    override val heading: Heading

    val inventory: Collection<ItemInstance>
    val skillsAndMagic: Collection<SkillInstance>
    override val temporalEffects: Collection<TemporalAbnormalEffect>

    override val isImmobilized: Boolean
    override val isParalyzed: Boolean
    override val weaponType: WeaponType?
    override val hasShield: Boolean
    override val isFighting: Boolean
    override val isMoving: Boolean
    override val targetId: Int?
    override val targetedBy: Set<ActorInstance>

    val pvpState: PvpState

    override val collisionBox: CollisionBox
    override val stats: CombatStats
    override val basicStats: BasicStats

    val resurrectionIsPending: Boolean
}
