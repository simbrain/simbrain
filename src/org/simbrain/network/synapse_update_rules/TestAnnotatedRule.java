/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.util.UserParameter;


/**
 * Test annotated param.
 * Extends PfisterGerstner2006Rule which defines floating params.
 * This also tests handling params defined in super-classes.
 *
 * @author Oliver J. Coleman
 */
public class TestAnnotatedRule extends PfisterGerstner2006Rule {
    @UserParameter(label = "test string", description = "Test string parameter.", order = -1)
    protected String ts;

    @UserParameter(label = "test bool", description = "Test boolean parameter.", order = -1)
    protected boolean tb;

    @UserParameter(label = "test int def", description = "Test int parameter with default.", order = -1, increment = .1)
    protected int ti;

    @UserParameter(label = "test int min", description = "Test int parameter with minimum.", order = -1, minimumValue = 1)
    protected int timin;

    @UserParameter(label = "test int max", description = "Test int parameter with maximum.", order = -1, maximumValue = -50)
    protected int timax;

    @UserParameter(label = "test int min max", description = "Test int parameter with min and max.", order = -1, minimumValue = -10, maximumValue = 200)
    protected int timinmax;

    @Override
    public String getName() {
        return "TestAnnotatedRule";
    }


    @Override
    public SynapseUpdateRule deepCopy() {
        try {
            return (PfisterGerstner2006Rule) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
