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
package org.simbrain.gauge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.network.NetworkFrame;

/**
 * <b>GaugedVariables</b> contains information about what data this gauge represents.
 */
public class GaugedVariables {

    /** Parent Gauge. */
    private Gauge parent;

    /** The variables this gauge gauges.  HashSet prevents repeat elements. */
    private HashSet variables = new HashSet();

    /** Persistent variables, used for saving gauge files. */
    private String persistentVariables;

    /** Name of network gauge is attached to. */
    private String networkName = "";

    /** Default constructor for creating gauged variables. */
    public GaugedVariables() {
    }

    /**
     * Creates gauged Variables.
     *
     * @param gauge to be used for variables
     */
    public GaugedVariables(final Gauge gauge) {
        parent = gauge;
    }

    /**
     * Used in persisting.
     *
     * @param net the network frame to which this is connected
     */
    public void initCastor(final NetworkFrame net) {
        if ((net == null) || (persistentVariables == null)) {
            return;
        }
        
        variables = new HashSet();
        StringTokenizer st = new StringTokenizer(persistentVariables, ",");
        while (st.hasMoreTokens()) {
            GaugeSource gs = (GaugeSource) net.getNetworkPanel().getNetwork().getNeuron(st.nextToken());
            if (gs == null) {
                return;
            }
            variables.add(gs);
        }
   }

    /**
     * Clears all variables for new gauge.
     */
    protected void clear() {
        networkName = new String("");
        variables.clear();
        resetGauge();
    }


    /**
     * Reset the gauge if the gauged variables have changed.
     */
    private void resetGauge() {
        int gaugeNum = parent.getUpstairs().getDimensions();
        int varsNum = getNumVariables();
        if (gaugeNum != varsNum) {
            parent.init(getNumVariables());
        }
    }

    /**
     * Add a new gauged variable.
     *
     * @param toAdd variable to add.
     */
    public void addVariable(final GaugeSource toAdd) {
        variables.add(toAdd);
        resetGauge();
    }

    /**
     * Update a gauged variable.
     *
     * @param oldVar out with the old
     * @param newVar in with the new
     */
    public void changeVariable(final GaugeSource oldVar, final GaugeSource newVar) {
        variables.remove(oldVar);
        variables.add(newVar);
    }

    /**
     * Remove a gauged variable.
     *
     * @param toRemove the variable to remove.
     */
    public void removeVariable(final GaugeSource toRemove) {
        variables.remove(toRemove);
        resetGauge();
    }

    /**
     * Returns the number of gauged variables.
     *
     * @return the number of gauged variables.
     */
    public int getNumVariables() {
        return variables.size();
    }

    /**
     * Get a string version of the list of gauged variables/ For persistence.
     * @return gauged variables string
     */
    private String getGaugedVarsString() {
        String ret = new String();

        int i = 0;
        for (Iterator iter = variables.iterator(); iter.hasNext(); i++) {
            GaugeSource gs = (GaugeSource) iter.next();
            String name = gs.getId();

            if (name == null) {
                break;
            }

            if (i == (variables.size() - 1)) {
                ret = ret.concat(name);
            } else {
                ret = ret.concat(name + ",");
            }
        }

        return ret;
    }

    /**
     * Called before saving to set persistent variables.
     */
    public void prepareToSave() {
        persistentVariables = getGaugedVarsString();
    }

    /**
     * Converts gauged variable states into a double array to be sent
     * to the hisee gauge.
     *
     * @return variables in double
     */
    public double[] getState() {
        double[] ret = new double[variables.size()];
        Iterator it = variables.iterator();
        int i = 0;

        while (it.hasNext()) {
            GaugeSource gs = (GaugeSource) it.next();
            ret[i] = gs.getGaugeValue();
            i++;
        }
        return ret;
    }

    /**
     * @return Returns the persistentGaugedVars.
     */
    public String getPersistentVariables() {
        return persistentVariables;
    }

    /**
     * @param persistentGaugedVars The persistentGaugedVars to set.
     */
    public void setPersistentVariables(final String persistentGaugedVars) {
        this.persistentVariables = persistentGaugedVars;
    }

    /**
     * @param gaugedVars The gaugedVars to set.
     */
    protected void setVariables(final Collection gaugedVars) {
        variables.clear();
        variables.addAll(gaugedVars);
        parent.init(gaugedVars.size());
    }

    /**
     * @return Returns the networkName.
     */
    public String getNetworkName() {
        return networkName;
    }

    /**
     * @param networkName The networkName to set.
     */
    public void setNetworkName(final String networkName) {
        this.networkName = networkName;
    }

    /**
     * @return parent gauge.
     */
    public Gauge getParent() {
        return parent;
    }

    /**
     * Sets which gauge variables are attached to.
     *
     * @param parent gauge for gauge variable
     */
    public void setParent(final Gauge parent) {
        this.parent = parent;
    }
}
