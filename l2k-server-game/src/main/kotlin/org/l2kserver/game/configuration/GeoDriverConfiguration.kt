package org.l2kserver.game.configuration

import com.l2jserver.geodriver.GeoDriver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeoDriverConfiguration {

    @Bean
    fun geoDriver() = GeoDriver()

}
