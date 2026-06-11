package org.simbrain.workspace

import org.simbrain.util.*

object WorkspacePreferences: PreferenceHolder() {

    @UserParameter(
        label = "Show info dock by default",
        description = "Show simulation info by default",
        order = 10
    )
    var showSimulationInfoByDefault by BooleanPreference(true)

    @UserParameter(
        label = "Show beta simulations",
        description = "Show beta simulations in the Simulations menu",
        order = 15
    )
    var showBetaSimulations by BooleanPreference(false)

    @UserParameter(
        label = "Show bottom dock by default",
        description = "Show bottom dock by default",
        order = 20
    )
    var showBottomDockByDefault by BooleanPreference(false)

    @UserParameter(
        label = "Bottom dock size",
        description = "Size of bottom dock in pixel",
        order = 30
    )
    var bottomDockSize by IntegerPreference(800)

    @UserParameter(
        label = "Simulation directory",
        useFileChooser = true,
        order = 50
    )
    var simulationDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "workspaces")

    @UserParameter(
        label = "Table directory",
        description = "Directory for importing and exporting tables stored as csv files",
        useFileChooser = true,
        order = 60
    )
    var tableDirectory by StringPreference("." + Utils.FS + "simulations" + Utils.FS + "tables")

    @UserParameter(
        label = "Import / export directory",
        description = "Directory for importing and exporting xml files",
        useFileChooser = true,
        order = 70
    )
    var importExportDirectory by StringPreference(".")

    @UserParameter(
        label = "Theme",
        description = "Application color theme. System follows the OS appearance. On macOS the " +
            "Swing content switches immediately; the window frame and menu bar update on restart.",
        order = 5
    )
    var themeMode by EnumPreference(ThemeMode.SYSTEM)

    // Onboarding popup preferences - stored as comma-separated suppressed popup keys
    var suppressedPopups by StringPreference("")

    /**
     * Check if a popup with the given key has been suppressed by the user
     */
    fun isPopupSuppressed(popupKey: String): Boolean {
        return suppressedPopups.split(",").contains(popupKey)
    }
    
    /**
     * Mark a popup as suppressed (do not show again)
     */
    fun suppressPopup(popupKey: String) {
        val current = suppressedPopups.split(",").toMutableSet()
        current.add(popupKey)
        suppressedPopups = current.filter { it.isNotBlank() }.joinToString(",")
    }
    
    /**
     * Remove suppression for a popup (for testing or reset purposes)
     */
    fun unsuppressPopup(popupKey: String) {
        val current = suppressedPopups.split(",").toMutableSet()
        current.remove(popupKey)
        suppressedPopups = current.filter { it.isNotBlank() }.joinToString(",")
    }
    
    /**
     * Clear all popup suppressions
     */
    fun clearAllPopupSuppressions() {
        suppressedPopups = ""
    }

}
