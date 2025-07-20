package org.l2kserver.game.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import org.l2kserver.game.extensions.logger
import org.springframework.stereotype.Component

private val mapper = YAMLMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
    .registerKotlinModule()

/**
 * Loader for game data, stored in data files (yml format).
 */
@Component
object GameDataLoader {

    private val log = logger()

    /**
     * Scans directory (recursively) for data of specified type
     *
     * @param directory Directory to scan
     * @param dataType Type of data objects
     *
     * @return List of data objects of set type
     */
    fun <T> scanDirectory(directory: File, dataType: Class<T>): List<T> {
        require(directory.exists() && directory.isDirectory) {
            "$directory does not exist or not a directory"
        }

        log.debug("Scanning {}...", directory.path)

        return buildList {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) addAll(scanDirectory(file, dataType))
                else if (file.name.endsWith(".yml") || file.name.endsWith(".yaml")) {
                    try {
                        val data = mapper.readValue(file, dataType)
                        add(data)
                        log.info("Loaded '{}':'{}'", file.name, data)
                    } catch (e: Exception) {
                        log.error("Error while parsing ${file.name}", e)
                    }
                }
            }
        }

    }

}
