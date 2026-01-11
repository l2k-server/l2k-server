package org.l2kserver.game.extensions.model.skill

import org.l2kserver.game.model.skill.instance.ActiveSkillInstance
import java.time.Instant

fun ActiveSkillInstance.isOnCooldown() = Instant.now().isBefore(this.nextUsageTime)
