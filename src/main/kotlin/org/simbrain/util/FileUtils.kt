package org.simbrain.util

import java.io.File
import java.net.URL

/**
 * Root is `src/main/resources`
 */
fun getFileFromResources(path: String): File? {
    val classLoader = ClassLoader.getSystemClassLoader()
    val resource: URL? = classLoader.getResource(path)
    return resource?.let { File(it.file) }
}

/**
 * Root is top level home directory that Simbrain is run from
 */
fun getFileFromRoot(relativePath: String): File {
    val homeDirectory = System.getProperty("user.dir")
    return File(homeDirectory, relativePath)
}

fun getFilesWithExtension(directoryPath: String, extension: String, searchInResources: Boolean = false) : Array<File> {
    val directory: File? = if (searchInResources) {
        getFileFromResources(directoryPath)
    } else {
        getFileFromRoot(directoryPath)
    }
    return directory?.let {
        it.listFiles { file -> file.isFile && file.extension == extension } ?: emptyArray()
    } ?: emptyArray()
}

// TODO: Move to unit test
fun main() {
    val file = getFileFromRoot("build.gradle.kts")
    // val file = getFileFromResources( "tinylog.properties")
    // val file = getFileFromResources( "imageworld/bobcat.jpg")
    if (file != null && file.exists()) {
        println("File found: ${file.absolutePath}")
    } else {
        println("File not found")
    }
}