package org.l2kserver.game.domain

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsertReturning
import org.l2kserver.game.model.actor.PlayerCharacter
import org.l2kserver.game.model.skill.ActiveSkill
import org.l2kserver.game.model.skill.PassiveSkill
import org.l2kserver.game.model.skill.ToggleSkill
import org.l2kserver.game.model.skill.instance.PassiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillInstance
import org.l2kserver.game.model.skill.template.ActiveSkillTemplate
import org.l2kserver.game.model.skill.template.PassiveSkillTemplate
import org.l2kserver.game.model.skill.template.SkillTemplateRegistry
import org.l2kserver.game.model.skill.template.ToggleSkillTemplate
import java.util.concurrent.ConcurrentHashMap

class SkillsAndMagic(val character: PlayerCharacter) : Map<Int, SkillInstance> {
    private val skills: MutableMap<Int, SkillInstance> = ConcurrentHashMap()

    init {
        reload()
    }

    /**
     * Saves skill to database
     *
     * @param skillId Skill ID
     * @param skillLevel Skill level
     */
    fun learn(skillId: Int, skillLevel: Int): SkillInstance = transaction {
        val queryResult = SkillTable
            .upsertReturning(SkillTable.characterId, SkillTable.skillId, SkillTable.subclassIndex) {
                it[characterId] = character.id
                it[subclassIndex] = character.activeSubclass
                it[SkillTable.skillId] = skillId
                it[SkillTable.skillLevel] = skillLevel
            }

        return@transaction SkillEntity.wrapRow(queryResult.first()).toSkill().also { skills[it.skillId] = it }
    }

    /** Finds all the passive skills of [SkillsAndMagic.character] learned with current subclass */
    fun passives() = skills.values.asSequence().filterIsInstance<PassiveSkillInstance>()

    /** Reloads skills from database */
    fun reload() = transaction {
        skills.putAll(
            SkillEntity.findAllByCharacterIdAndSubclassIndex(
                character.id, character.activeSubclass
            ).map { it.skillId to it.toSkill() })
    }

    override val entries by skills::entries
    override val keys by skills::keys
    override val size by skills::size
    override val values by skills::values

    override fun containsKey(key: Int) = skills.containsKey(key)
    override fun containsValue(value: SkillInstance) = skills.containsValue(value)
    override fun get(key: Int) = requireNotNull(skills[key]) {
        "Skill '$key' was not learnt or does not exist"
    }

    override fun isEmpty() = skills.isEmpty()

    override fun toString() = "SkillsAndMagic(character=$character, skills=$skills)"
}

private fun SkillEntity.Companion.findAllByCharacterIdAndSubclassIndex(
    characterId: Int, subclassIndex: Int
) = SkillEntity.find { (SkillTable.characterId eq characterId) and (SkillTable.subclassIndex eq subclassIndex) }

private fun SkillEntity.toSkill() = when (val template = SkillTemplateRegistry.findById(this.skillId)) {
    is ActiveSkillTemplate -> ActiveSkill(this, template)
    is PassiveSkillTemplate -> PassiveSkill(this, template)
    is ToggleSkillTemplate -> ToggleSkill()
}
