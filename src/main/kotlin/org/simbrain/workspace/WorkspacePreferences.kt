package org.simbrain.workspace

import org.simbrain.util.*

object WorkspacePreferences: PreferenceHolder() {

    @UserParameter(label = "Sim directory")
    var simulationDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "workspaces")

    var bottomDockSize by IntegerPreference(800)

}
