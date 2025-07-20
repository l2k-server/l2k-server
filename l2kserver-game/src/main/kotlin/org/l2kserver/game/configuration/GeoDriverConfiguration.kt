package org.l2kserver.game.configuration

import com.l2jserver.geodriver.GeoDriver
import org.l2kserver.game.extensions.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

private const val TILE_X_MIN = 16
private const val TILE_X_MAX = 26
private const val TILE_Y_MIN = 10
private const val TILE_Y_MAX = 26

private const val GEODATA_PATH = "./data/geodata"

@Configuration
class GeoDriverConfiguration {
    private val log = logger()

    @Bean
    fun geoDriver(): GeoDriver {
        val geoDriver = GeoDriver()

        for (i: Int in TILE_X_MIN..TILE_X_MAX) {
            for (j: Int in TILE_Y_MIN..TILE_Y_MAX) {
                val geoDataFile = File("$GEODATA_PATH/${i}_${j}.l2j")
                if (geoDataFile.exists()) {
                    geoDriver.loadRegion(geoDataFile.toPath(), i, j)
                    log.debug("Successfully loaded region {}_{}", i, j)
                }
                else
                    log.warn("GeoData file '{}' does not exist!", geoDataFile)
            }
        }

        return geoDriver
    }

}
