package org.l2kserver.game.configuration

import jakarta.annotation.PostConstruct
import org.l2kserver.game.ai.L2AiScript
import org.l2kserver.game.domain.character.CharacterClass
import org.l2kserver.game.domain.character.CharacterTemplate
import org.l2kserver.game.domain.item.template.ItemTemplate
import org.l2kserver.game.domain.map.Town
import org.l2kserver.game.domain.npc.NpcTemplate
import org.l2kserver.game.domain.skill.SkillTemplate
import org.springframework.context.annotation.Configuration

@Configuration
class EntityConfiguration {

    /**
     * Loads all the domain entities, which are provided as lazy kotlin singletons.
     * This is needed to create them on application start
     */
    @PostConstruct
    fun initialize() {
        CharacterTemplate
        CharacterClass
        ItemTemplate
        NpcTemplate
        Town
        L2AiScript
        SkillTemplate
    }

}
