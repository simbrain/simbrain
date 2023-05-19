package org.simbrain.world.odorworld

import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils

object OdorWorldPreferences: PreferenceHolder {

    @UserParameter(label = "World directory")
    var directory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "worlds");

}
