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
     * The version number (e.g., "4.0.0")
     */
    val version: String
        get() = buildProperties.getProperty("version", "4.0.0")
    
    /**
     * The version name (e.g., "4Beta")
     */
    val versionName: String
        get() = buildProperties.getProperty("versionName", "4Beta")
    
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
     * Get the full version string including build number
     * Format: "Version 4.0.0 Beta (Build 123)"
     */
    val fullVersionString: String
        get() {
            val baseVersion = "Version $version Beta"
            return if (buildNumber != "dev" && buildNumber != "unknown") {
                "$baseVersion (Build $buildNumber)"
            } else {
                baseVersion
            }
        }
    
    /**
     * Get the application title including build number
     * Format: "Simbrain 4 Beta (Build 123)"
     */
    val applicationTitle: String
        get() {
            val baseTitle = "Simbrain $versionName"
            return if (buildNumber != "dev" && buildNumber != "unknown") {
                "$baseTitle (Build $buildNumber)"
            } else {
                baseTitle
            }
        }
    
    /**
     * Get detailed build information for debugging
     */
    val detailedBuildInfo: String
        get() = """
            Version: $version
            Version Name: $versionName
            Build Number: $buildNumber
            Commit SHA: $commitSha
            Build Timestamp: $buildTimestamp
        """.trimIndent()
}
