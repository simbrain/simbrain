package org.simbrain.workspace

import org.simbrain.util.*

object WorkspacePreferences: PreferenceHolder() {

    @UserParameter(label = "Sim directory")
    var simulationDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "workspaces")

    var bottomDockSize by IntegerPreference(800)
    
    // Onboarding popup preferences - stored as comma-separated suppressed popup keys
    private var suppressedPopups by StringPreference("")
    
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
