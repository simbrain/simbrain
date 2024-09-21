package org.simbrain.world.textworld

import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.Utils

object TextWorldPreferences: PreferenceHolder() {

    var tokenEmbeddingDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "texts")

    var sampleTextsDirectory by StringPreference("." + Utils.FS +"simulations" + Utils.FS + "texts")

}