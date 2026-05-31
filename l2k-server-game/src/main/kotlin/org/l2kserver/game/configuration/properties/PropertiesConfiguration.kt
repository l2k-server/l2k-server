package org.l2kserver.game.configuration.properties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(LevelProperties::class, EnchantProperties::class)
class PropertiesConfiguration
