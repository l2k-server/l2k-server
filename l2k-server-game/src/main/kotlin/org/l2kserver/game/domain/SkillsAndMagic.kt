package org.l2kserver.game.domain

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import org.l2kserver.game.model.actor.PlayerCharacterInstanceImpl
import org.l2kserver.game.model.skill.ActiveSkillInstanceImpl
import org.l2kserver.game.model.skill.PassiveSkillInstanceImpl
import org.l2kserver.game.model.skill.ToggleSkillInstanceImpl
import org.l2kserver.game.model.skill.instance.PassiveSkillInstance
import org.l2kserver.game.model.skill.instance.SkillInstance
import org.l2kserver.game.model.skill.template.ActiveSkill
import org.l2kserver.game.model.skill.template.PassiveSkill
import org.l2kserver.game.model.skill.template.SkillRegistry
import org.l2kserver.game.model.skill.template.ToggleSkill
import java.util.concurrent.ConcurrentHashMap

class SkillsAndMagic(val character: PlayerCharacterInstanceImpl): Collection<SkillInstance> {
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

    fun findById(skillId: Int) = requireNotNull(skills[skillId]) {
        "Skill '$skillId' was not learnt or does not exist"
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

    override val size by skills::size
    override fun contains(element: SkillInstance) = skills.values.contains(element)
    override fun containsAll(elements: Collection<SkillInstance>) = skills.values.containsAll(elements)
    override fun isEmpty() = skills.isEmpty()
    override fun iterator() = skills.values.iterator()

    override fun toString() = "SkillsAndMagic(character=$character, skills=$skills)"
}

private fun SkillEntity.Companion.findAllByCharacterIdAndSubclassIndex(
    characterId: Int, subclassIndex: Int
) = SkillEntity.find { (SkillTable.characterId eq characterId) and (SkillTable.subclassIndex eq subclassIndex) }

private fun SkillEntity.toSkill() = when (val template = SkillRegistry.findById(this.skillId)) {
    is ActiveSkill -> ActiveSkillInstanceImpl(this, template)
    is PassiveSkill -> PassiveSkillInstanceImpl(this, template)
    is ToggleSkill -> ToggleSkillInstanceImpl()
}
