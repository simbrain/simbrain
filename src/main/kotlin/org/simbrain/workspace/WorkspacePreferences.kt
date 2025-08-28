package org.simbrain.workspace

import org.simbrain.util.*

object WorkspacePreferences: PreferenceHolder() {

    @UserParameter(label = "Sim directory", useFileChooser = true)
    var simulationDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "workspaces")

    @UserParameter(label = "Show bottom dock by Default", description = "Show bottom dock by default")
    var showBottomDockByDefault by BooleanPreference(true)

    @UserParameter(label = "Bottom dock size", description = "Size of bottom dock in pixel")
    var bottomDockSize by IntegerPreference(800)
    
    // Onboarding popup preferences - stored as comma-separated suppressed popup keys
    @UserParameter(label = "Onboarding popups", description = "Delete all strings to reset all popups")
    var suppressedPopups by StringPreference("")

    @UserParameter(label = "Show Info Dock by Default", description = "Show simulation info by default")
    var showSimulationInfoByDefault by BooleanPreference(true)
    
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
