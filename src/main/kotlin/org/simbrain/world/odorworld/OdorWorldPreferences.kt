package org.simbrain.world.odorworld

import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.UserParameter
import org.simbrain.util.div

object OdorWorldPreferences: PreferenceHolder() {

    @UserParameter(label = "World directory")
    var tileMapDirectory by StringPreference("." / "simulations" / "worlds" / "tilemaps");

}
