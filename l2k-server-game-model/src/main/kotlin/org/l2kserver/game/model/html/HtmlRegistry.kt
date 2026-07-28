package org.l2kserver.game.model.html

import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

/** Stores all the HTMLs for npc chat windows, quests, community table, etc. */
object HtmlRegistry {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val htmlData = ConcurrentHashMap<String, String>()

    /** Finds HTML text by identifier, or throws [IllegalArgumentException] if no data with [id] exists */
    fun findById(id: String) = requireNotNull(htmlData[id]) { "No HTML found by id=$id" }

    /**
     * Loads resource file or directory by provided path
     *
     * @param resourcePath Path to resource file
     * @param charset Charset to decode files. For our game default is Unicode (UTF16)
     */
    fun loadResource(resourcePath: String, charset: Charset = Charsets.UTF_16) {
        val file =  File(ClassLoader.getSystemResource(resourcePath).file)
        loadFile(file, charset)
    }

    /**
     * Scans provided [file] and saves it's data. If [file] is directory - loads all the files from it (recursively)
     *
     * @param file File or directory to scan
     * @param charset Charset to decode files. For our game default is Unicode (UTF16)
     */
    fun loadFile(file: File, charset: Charset = Charsets.UTF_16) {
        if (file.isFile) {
            try {
                this.loadString(file.name, file.readText(charset))
            }
            catch (e: Exception) {
                log.error("Error while reading file {}", file, e)
            }
        }
        else {
            if (file.listFiles().isNullOrEmpty()) log.warn("$file is empty!")
            file.listFiles()?.forEach { file -> loadFile(file, charset) }
        }
    }

    fun loadString(id: String, value: String) {
        if (htmlData.containsKey(id))
            log.warn("Html text data '{}' is already registered - overriding", id)

        log.debug("Loaded html file {}", id)
        htmlData[id] = value
    }

}
