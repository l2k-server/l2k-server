package org.l2kserver.game.domain.character

import com.fasterxml.jackson.annotation.JsonRootName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.l2kserver.game.domain.item.template.Slot
import org.l2kserver.game.extensions.logger
import org.l2kserver.game.model.stats.Stats
import org.l2kserver.game.model.actor.enumeration.CharacterClassName
import org.l2kserver.game.model.stats.BasicStats
import org.l2kserver.game.model.stats.TradeAndInventoryStats
import org.l2kserver.game.utils.GameDataLoader

/**
 * Character class data - stats, skills, misc. information
 *
 * @property name Class name (Human Fighter, etc)
 * @property requiredLevel Level, required to take this class
 * @property parentClassName Parent classId (for example, for Human Warrior parent class will be Human Fighter)
 * @property baseClassName Base class name (for example, for Duelist base class will be Human Fighter)
 * @property name Character class name
 * @property initialBasicStats Initial values of character's basic stats
 * @property initialStats Initial values of character's stats
 * @property emptySlotStats Stats of character's empty slots. Will be applied if no item equipped in slot
 */
@JsonRootName("characterClass")
data class CharacterClass(
    val name: CharacterClassName,
    val requiredLevel: Int,
    val parentClassName: CharacterClassName?,
    val baseClassName: CharacterClassName = name,
    val initialBasicStats: BasicStats,
    val initialStats: Stats,
    val initialTradeAndInventoryStats: TradeAndInventoryStats,
    val emptySlotStats: Map<Slot, Stats>,
    val perLevelGain: PerLevelGain
) {
    companion object {
        private val characterClasses = ConcurrentHashMap<CharacterClassName, CharacterClass>()
        private val log = logger()

        init {
            log.info("Loading character classes...")
            GameDataLoader.scanDirectory(File("./data/character/character_class"), CharacterClass::class.java)
                .forEach { characterClasses[it.name] = it }
        }

        fun findByName(characterClassName: CharacterClassName) = requireNotNull(characterClasses[characterClassName]) {
            "No character class found by name $characterClassName"
        }
    }

    val baseAtkSpd = emptySlotStats.values.reduce { acc, stats -> acc + stats }.atkSpd
    val baseSpeed = initialStats.speed

}

data class PerLevelGain(
    val cpAdd: Double = 0.0,
    val cpMod: Double = 0.0,
    val hpAdd: Double = 0.0,
    val hpMod: Double = 0.0,
    val mpAdd: Double = 0.0,
    val mpMod: Double = 0.0
)
