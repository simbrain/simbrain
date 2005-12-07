/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.interfaces;

import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.RiseAndDecay;
import org.simnet.synapses.spikeresponders.Step;


/**
 * <b>SpikeResponder</b>
 */
public abstract class SpikeResponder {
    protected double value = 0;
    protected boolean scaleByPSPDifference = false;
    protected double psRestingPotential = 0;
    protected Synapse parent;
    private static String[] typeList = {Step.getName(), JumpAndDecay.getName(), RiseAndDecay.getName() };

    public abstract SpikeResponder duplicate();

    public abstract void update();

    public SpikeResponder duplicate(final SpikeResponder s) {
        s.setScaleByPSPDifference(getScaleByPSPDifference());
        s.setPsRestingPotential(getPsRestingPotential());

        return s;
    }

    /**
     * @return the name of the class of this synapse
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * @return Returns the typeList.
     */
    public static String[] getTypeList() {
        return typeList;
    }

    /**
     * @param typeList The typeList to set.
     */
    public static void setTypeList(final String[] typeList) {
        SpikeResponder.typeList = typeList;
    }

    /**
     * @return Returns the psRestingPotential.
     */
    public double getPsRestingPotential() {
        return psRestingPotential;
    }

    /**
     * @param psRestingPotential The psRestingPotential to set.
     */
    public void setPsRestingPotential(final double psRestingPotential) {
        this.psRestingPotential = psRestingPotential;
    }

    /**
     * @return Returns the scaleByPSPDifference.
     */
    public boolean getScaleByPSPDifference() {
        return scaleByPSPDifference;
    }

    /**
     * @param scaleByPSPDifference The scaleByPSPDifference to set.
     */
    public void setScaleByPSPDifference(final boolean scaleByPSPDifference) {
        this.scaleByPSPDifference = scaleByPSPDifference;
    }

    /**
     * Helper function for combo boxes.  Associates strings with indices.
     */
    public static int getSpikerTypeIndex(final String type) {
        for (int i = 0; i < typeList.length; i++) {
            if (type.equals(typeList[i])) {
                return i;
            }
        }

        return 0;
    }

    /**
     * @return Returns the value.
     */
    public double getValue() {
        if (this.getScaleByPSPDifference() == true) {
            return value * (getPsRestingPotential() - parent.getTarget().getActivation());
        }

        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(final double value) {
        this.value = value;
    }

    /**
     * @return Returns the parent.
     */
    public Synapse getParent() {
        return parent;
    }

    /**
     * @param parent The parent to set.
     */
    public void setParent(final Synapse parent) {
        this.parent = parent;
    }
}
