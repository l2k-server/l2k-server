package org.l2kserver.game.configuration

import org.l2kserver.game.utils.ExpLossCalculator
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "game")
data class GameProperties(val expLoss: Map<Int, Double> = emptyMap())

@Configuration
@EnableConfigurationProperties(GameProperties::class)
class ExpLossConfiguration(private val gameProperties: GameProperties) {

    @Bean
    fun expLossCalculator() = ExpLossCalculator(gameProperties.expLoss)
}
