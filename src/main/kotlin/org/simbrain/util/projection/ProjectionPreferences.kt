package org.simbrain.util.projection

import org.simbrain.util.DoublePreference
import org.simbrain.util.PreferenceHolder
import org.simbrain.util.UserParameter

object ProjectionPreferences: PreferenceHolder() {

    @UserParameter(label = "Projector tolerance")
    var tolerance by DoublePreference(.1)

    @UserParameter(label = "Sammon perturbation amount")
    var sammonPerturbationAmount by DoublePreference(.1)

    @UserParameter(label = "Sammon epsilon")
    var sammonEpsilon by DoublePreference(.5)
}