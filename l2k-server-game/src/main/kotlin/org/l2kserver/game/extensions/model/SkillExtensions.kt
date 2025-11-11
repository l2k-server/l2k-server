package org.l2kserver.game.extensions.model

import org.l2kserver.game.model.skill.instance.CastableSkillInstance
import java.time.Instant

fun CastableSkillInstance.isOnCooldown() = Instant.now().isBefore(this.nextUsageTime)
