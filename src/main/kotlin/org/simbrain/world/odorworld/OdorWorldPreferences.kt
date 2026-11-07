package org.simbrain.world.odorworld

import org.simbrain.util.BooleanPreference
import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.UserParameter
import org.simbrain.util.div

object OdorWorldPreferences: PreferenceHolder() {

    @UserParameter(label = "Tile map directory")
    var tileMapDirectory by StringPreference("." / "simulations" / "worlds" / "tilemaps");

    @UserParameter(
        label = "Always show resize handles",
        description = "Show the corner handles that resize a selected entity without holding Alt (Option on macOS)"
    )
    var alwaysShowResizeHandles by BooleanPreference(false)

}
