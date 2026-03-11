package org.simbrain.util

import com.Ostermiller.util.CSVParser
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.simbrain.util.widgets.ProgressWindow
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

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

fun fetchDataFromUrl(urlString: String): String? {
    var progressWindow: ProgressWindow? = null

    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Open progress window
        val contentLength = connection.contentLength
        progressWindow = ProgressWindow(contentLength, "Downloading...")

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = BufferedInputStream(connection.inputStream)
            val result = StringBuilder()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytesRead = 0

            // Read the data in chunks
            while (inputStream.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                result.append(String(buffer, 0, bytesRead))
                totalBytesRead += bytesRead

                // Update progress bar
                progressWindow.setValue(totalBytesRead)
                progressWindow.setText("Downloaded $totalBytesRead of $contentLength bytes")
            }

            progressWindow.close()
            return result.toString()
        } else {
            showWarningDialog("Failed to fetch data. Response code: $responseCode")
            progressWindow.close()
            return null
        }
    } catch (e: Exception) {
        progressWindow?.close()
        showWarningDialog("An error occurred: ${e.message}")
        return null
    }
}

fun csvToDouble2DArray(csvString: String): Array<DoubleArray> {
    try {
        // Parse the CSV string
        val parser = CSVParser(StringReader(csvString))
        val rows = parser.allValues
        val result = Array(rows.size) { DoubleArray(rows[0].size) }

        // Convert each value in the CSV to a Double
        for (i in rows.indices) {
            for (j in rows[i].indices) {
                val value = rows[i][j].trim()
                val doubleValue = value.toDoubleOrNull() ?: throw IllegalArgumentException("Non-numeric value found: '$value'")
                result[i][j] = doubleValue
            }
        }

        return result
    } catch (e: Exception) {
        throw IllegalArgumentException("Error parsing CSV: ${e.message}", e)
    }
}

// Helper to get the cache file based on the URL and cache directory
fun getCacheFile(urlString: String, cacheDir: String): File {

    val url = URI(urlString).toURL()

    val fileName = url.path.substringAfterLast('/')

    if (fileName.isEmpty()) throw IllegalArgumentException("Invalid URL: $urlString")

    return File(cacheDir, fileName)
}

// Function to check if the cache file is valid
fun isCacheValid(file: File): Boolean {
    // Example: you could add time-based cache expiration here if desired
    return file.exists() && file.length() > 0
}

// Function to read data from cache
fun readFromCache(file: File): String? {
    return try {
        file.readText()
    } catch (e: IOException) {
        null
    }
}

// Function to save downloaded data to the cache
fun saveToCache(file: File, data: String) {
    try {
        file.parentFile.mkdirs() // Create parent directories if they don't exist
        file.writeText(data)
    } catch (e: IOException) {
        showWarningDialog("Failed to save cache: ${e.message}")
    }
}

/**
 * Returns a system-appropriate directory for application cache files.
 * This ensures the application has write permissions regardless of installation location.
 * 
 * @param appName The name of the application, used to create an app-specific subfolder
 * @return A File object representing the cache directory
 */
fun getSystemCacheDirectory(appName: String = "Simbrain"): File {
    val userHome = System.getProperty("user.home")
    
    val cacheDir = when {
        // Windows
        System.getProperty("os.name").lowercase().contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (localAppData != null) {
                File(localAppData, appName)
            } else {
                File(userHome, "AppData/Local/$appName")
            }
        }
        // macOS
        System.getProperty("os.name").lowercase().contains("mac") -> {
            File(userHome, "Library/Caches/$appName")
        }
        // Linux and others
        else -> {
            File(userHome, ".cache/$appName")
        }
    }
    
    // Create the directory if it doesn't exist
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    
    return cacheDir
}

// Function to handle fetching data with caching
fun fetchDataWithCache(urlString: String): String? {
    val cacheDir = getSystemCacheDirectory().absolutePath
    val cacheFile = getCacheFile(urlString, cacheDir)

    // Check if cached file exists and is valid
    if (cacheFile.exists() && isCacheValid(cacheFile)) {
        return readFromCache(cacheFile)
    } else {
        // Download the data and cache it
        val data = fetchDataFromUrl(urlString)
        if (data != null) {
            saveToCache(cacheFile, data)
        }
        return data
    }
}

/**
 * Downloads a zip archive from [url], extracts it (preserving directory structure) into the
 * system cache directory, and returns the root extraction directory.
 *
 * On subsequent calls the zip is not re-downloaded: if the extraction directory already exists
 * and is non-empty it is returned immediately.
 *
 * The zip file itself is deleted after successful extraction to save disk space.
 *
 * @param url              URL of the zip archive. Query parameters (e.g. `?download=1`) are
 *                         stripped when deriving the local directory name.
 * @param expectedChecksum Optional hex checksum to verify the download. The algorithm is inferred
 *                         from the digest length: 32 hex chars → MD5, 40 → SHA-1, 64 → SHA-256.
 *                         If verification fails the partial download is deleted and null is returned.
 * @return The root extraction directory, or `null` on failure.
 */
fun fetchZipWithCache(url: String, expectedChecksum: String? = null): File? {
    val cacheDir = getSystemCacheDirectory()
    // Strip query string to get a clean filename for the cache dir name
    val zipName = URI(url).path.substringAfterLast('/')
    val extractDir = File(cacheDir, zipName.removeSuffix(".zip"))

    if (extractDir.exists() && extractDir.listFiles()?.isNotEmpty() == true) {
        return extractDir
    }

    // Resolve digest algorithm before downloading so we can compute it inline
    val digest: MessageDigest? = if (expectedChecksum != null) {
        val algorithm = when (expectedChecksum.length) {
            32   -> "MD5"
            40   -> "SHA-1"
            64   -> "SHA-256"
            else -> { showWarningDialog("Unrecognised checksum length (${expectedChecksum.length} hex chars)"); return null }
        }
        MessageDigest.getInstance(algorithm)
    } else null

    val zipFile = File(cacheDir, zipName)
    var progressWindow: ProgressWindow? = null
    try {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)  // follows cross-domain HTTPS→HTTPS redirects
            .build()
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() != 200) {
            showWarningDialog("Failed to download $zipName (HTTP ${response.statusCode()})")
            return null
        }

        val contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
        progressWindow = ProgressWindow(contentLength.toInt(), "Downloading $zipName…")

        response.body().use { input ->
            // Wrap output in DigestOutputStream when checksum verification is requested,
            // computing the hash inline during download without a second file read.
            val fileOut = FileOutputStream(zipFile)
            val output = if (digest != null) DigestOutputStream(fileOut, digest) else fileOut
            output.use { out ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var total = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                    total += bytesRead
                    progressWindow.setValue(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
            }
        }
        progressWindow.close()
    } catch (e: Exception) {
        progressWindow?.close()
        showWarningDialog("Error downloading $zipName: ${e.message}")
        zipFile.delete()
        return null
    }

    if (digest != null && expectedChecksum != null) {
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expectedChecksum, ignoreCase = true)) {
            showWarningDialog("Checksum mismatch for $zipName.\nExpected: $expectedChecksum\nActual:   $actual")
            zipFile.delete()
            return null
        }
    }

    try {
        val canonicalDest = extractDir.canonicalPath + File.separator
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(extractDir, entry.name)
                // Guard against Zip Slip: entry names with ../ can escape the destination
                if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile.mkdirs()
                    FileOutputStream(outFile).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    } catch (e: Exception) {
        extractDir.deleteRecursively()
        showWarningDialog("Error extracting $zipName: ${e.message}")
        return null
    } finally {
        zipFile.delete()
    }

    return extractDir
}

/**
 * Extracts a `.tar.gz` archive into [destDir] using Apache Commons Compress.
 *
 * Handles UStar, GNU long names, PAX extended headers, and all standard tar entry types.
 * Path-traversal entries (names containing `../` that escape [destDir]) are skipped.
 *
 * @return `true` on success, `false` on failure (partial output is cleaned up).
 */
fun extractTarGz(tarGzFile: File, destDir: File): Boolean {
    destDir.mkdirs()
    val canonicalDest = destDir.canonicalPath + File.separator
    return try {
        TarArchiveInputStream(GZIPInputStream(tarGzFile.inputStream().buffered())).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                    entry = tar.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> tar.copyTo(out) }
                }
                entry = tar.nextEntry
            }
        }
        true
    } catch (e: Exception) {
        destDir.deleteRecursively()
        showWarningDialog("Error extracting ${tarGzFile.name}: ${e.message}")
        false
    }
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