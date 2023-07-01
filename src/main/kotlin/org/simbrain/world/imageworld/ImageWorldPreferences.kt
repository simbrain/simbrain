package org.simbrain.world.imageworld

import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils

object ImageWorldPreferences: PreferenceHolder() {

    @UserParameter(label = "Image directory")
    var imageDirectory by StringPreference("." + Utils.FS +"simulations")

}
