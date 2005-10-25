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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.simbrain.gauge.core.Gauge;
import org.simbrain.network.NetworkFrame;

import edu.umd.cs.piccolo.PNode;


/**
 * <b>GaugedVariables</b> contains information about what data this gauge represents.
 */
public class GaugedVariables {
    private Gauge parent;
    private ArrayList variables; // the variables this gauge gauges
    private String persistentVariables;
    private String networkName = null;

    public GaugedVariables() {
    }

    public GaugedVariables(Gauge gauge) {
        parent = gauge;
    }

    /**
     * Used in persisting.
     * @param net the network frame to which this is connected
     */
    public void initCastor(NetworkFrame net) {
        if (net == null) {
            return;
        }

        if (persistentVariables == null) {
            return;
        }

        variables = new ArrayList();

        StringTokenizer st = new StringTokenizer(persistentVariables, ",");

        while (st.hasMoreTokens()) {
            PNode pn = (PNode) net.getNetPanel().getPNode(st.nextToken());

            if (pn == null) {
                return;
            }

            variables.add(pn);
        }
    }

    /**
     * @return Returns the gaugedVars.
     */
    public ArrayList getVariables() {
        return variables;
    }

    public void clear() {
        networkName = null;
        variables = null;
    }

    /**
     * Get a string version of the list of gauged variables/ For persistence.
     * @return gauged variables string
     */
    private String getGaugedVarsString() {
        String ret = new String();

        for (int i = 0; i < variables.size(); i++) {
            String name = ((GaugeSource) variables.get(i)).getId();

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

    // Convert gauged variable states into a double array to be sent
    // to the hisee gauge
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
    public void setPersistentVariables(String persistentGaugedVars) {
        this.persistentVariables = persistentGaugedVars;
    }

    /**
     * @param gaugedVars The gaugedVars to set.
     */
    public void setVariables(ArrayList gaugedVars) {
        this.variables = gaugedVars;
        parent.init(gaugedVars.size());
        persistentVariables = getGaugedVarsString();
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
    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public Gauge getParent() {
        return parent;
    }

    public void setParent(Gauge parent) {
        this.parent = parent;
    }
}
