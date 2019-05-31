/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.neuron_update_rules.interfaces;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.util.UserParameter;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract class for activity generators.
 */
public abstract class ActivityGenerator extends SpikingNeuronUpdateRule {


    // Extends SpikingNeuronUpdateRule because otherwise there is no way to have StochasticUpdateRule work

    /**
     * Rules for drop-down list used by {@link org.simbrain.util.propertyeditor.ObjectTypeEditor}
     * to set the update rule on an activity generator
     */
    public static List<Class> RULE_LIST = Arrays.asList(LogisticRule.class, StochasticRule.class,
        SinusoidalRule.class, RandomNeuronRule.class);

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return RULE_LIST;
    }


}
