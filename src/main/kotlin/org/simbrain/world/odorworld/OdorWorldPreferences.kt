package org.simbrain.world.odorworld

import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.UserParameter

object OdorWorldPreferences: PreferenceHolder() {

    @UserParameter(label = "World directory")
    var odorWorldDirectory by StringPreference("./simulations/worlds");

    @UserParameter(label = "World directory")
    var tileMapDirectory by StringPreference("./simulations/worlds/tilemaps");


}
