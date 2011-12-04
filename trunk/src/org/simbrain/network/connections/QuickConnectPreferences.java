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
package org.simbrain.network.connections;


/**
 * Properties for quick connect, where a connection is applied using a key
 * command, without a dialog. In this way many connections can be quickly
 * created.
 *
 * @author jyoshimi
 */
public class QuickConnectPreferences {

    /**
     * Default connection type for quick connect.
     */
    private static final ConnectNeurons DEFAULT_CONNECTION = new AllToAll();

    /**
     * Static set of connection types. These are modified and hthe current one
     * is used for quick connect.
     */
    private static final ConnectNeurons[] connectionTypes = new ConnectNeurons[] {
            DEFAULT_CONNECTION, new OneToOne(), new Radial(), new Sparse() };

    /**
     * Holds "current" connection object.
     */
    private static ConnectNeurons currentConnection = DEFAULT_CONNECTION;

    /**
     * @return the currentConnection
     */
    public static ConnectNeurons getCurrentConnection() {
        return currentConnection;
    }

    /**
     * @param currentConnection the currentConnection to set
     */
    public static void setCurrentConnection(ConnectNeurons currentConnection) {
        QuickConnectPreferences.currentConnection = currentConnection;
    }

    /**
     * @return the connectiontypes
     */
    public static ConnectNeurons[] getConnectiontypes() {
        return connectionTypes;
    }

}
