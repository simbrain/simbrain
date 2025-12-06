package org.simbrain.util

import java.io.IOException
import java.util.*

/**
 * Utility class to access build information generated during the build process.
 * This information includes version, build number, commit SHA, and build timestamp.
 */
object BuildInfo {

    private val buildProperties: Properties by lazy {
        val properties = Properties()
        try {
            val inputStream = BuildInfo::class.java.classLoader.getResourceAsStream("build-info.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                inputStream.close()
            }
        } catch (e: IOException) {
            // If we can't load the properties file, we'll use default values
            println("Warning: Could not load build-info.properties: ${e.message}")
        }
        properties
    }

    /**
     * The version number (e.g., "4.0.0" or "4.0.0-beta1")
     */
    val version: String
        get() = buildProperties.getProperty("version", "4.0.0")

    /**
     * The version name (same as version for consistency)
     */
    val versionName: String
        get() = buildProperties.getProperty("versionName", "4.0.0")

    /**
     * Whether this is a beta release (version contains "-beta")
     */
    val isBeta: Boolean
        get() = buildProperties.getProperty("isBeta", "false").toBoolean()

    /**
     * The build number from CI/CD (e.g., "123")
     */
    val buildNumber: String
        get() = buildProperties.getProperty("buildNumber", "dev")

    /**
     * The commit SHA (short form, e.g., "abc1234")
     */
    val commitSha: String
        get() = buildProperties.getProperty("commitSha", "unknown")

    /**
     * The build timestamp (e.g., "20240101_120000")
     */
    val buildTimestamp: String
        get() = buildProperties.getProperty("buildTimestamp", "unknown")

    /**
     * Get the full version string for the About dialog
     * Format: "Version 4.0.0" or "Version 4.0.0-beta1 Beta (Build 123)"
     */
    val fullVersionString: String
        get() {
            val betaSuffix = if (isBeta) " Beta" else ""
            val baseVersion = "Version $version$betaSuffix"
            return if (buildNumber != "dev" && buildNumber != "unknown") {
                "$baseVersion (Build $buildNumber)"
            } else {
                baseVersion
            }
        }

    /**
     * Get the application title for the window title bar
     * Format: "Simbrain 4.0.0" (simple, no build number)
     */
    val applicationTitle: String
        get() = "Simbrain $version"

    /**
     * Get detailed build information for debugging
     */
    val detailedBuildInfo: String
        get() = """
            Version: $version
            Version Name: $versionName
            Is Beta: $isBeta
            Build Number: $buildNumber
            Commit SHA: $commitSha
            Build Timestamp: $buildTimestamp
        """.trimIndent()
}
