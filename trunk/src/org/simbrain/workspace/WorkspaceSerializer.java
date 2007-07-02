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
package org.simbrain.workspace;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.gauge.GaugeComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.util.Utils;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;


/**
 * <b>WorkspaceSerializer</b> handles workspace persistence.  It contains static methods for reading and writing
 * workspace files, and also serves as a buffer for Castor initialization.
 */
public class WorkspaceSerializer {

    /** File system property. */
    private static final String FS = System.getProperty("file.separator");

    /**
     * Read  workspace file.
     *
     * @param f file containing new workspace information
     * @param isImport whether this workspace is being imported or opened
     */
    public static void readWorkspace(final File f, final boolean isImport) {
        Workspace.getInstance().clearWorkspace();

        WorkspaceSerializer wSerializer = new WorkspaceSerializer();

        try {
            Reader reader = new FileReader(f);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "workspace_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(wSerializer);
            unmarshaller.setMapping(map);

            // unmarshaller.setDebug(true);
            wSerializer = (WorkspaceSerializer) unmarshaller.unmarshal(reader);
        } catch (java.io.FileNotFoundException e) {
            if (Workspace.getInstance().isInitialLaunch()) {
                Workspace.getInstance().setInitialLaunch(false);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Could not find workspace file \n" + f, "Warning",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "There was a problem opening the workspace file \n" + f,
                    "Warning", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }

        for (WorkspaceComponent window : Workspace.getInstance().getComponentList()) {
            //window.setBounds(arg0, arg1, arg2, arg3);
            if (window.getGenericPath() != null) {
                if (isImport) {
                    String name = Utils.getDir(f) + Utils.getNameFromPath(window.getGenericPath());
                    window.open(new File(name));
                } else {
                    window.open(new File(window.getGenericPath()));
                }
            }
            Workspace.getInstance().addSimbrainComponent(window);
        }

        // Create couplings and attach agents to them
        //ArrayList couplings = wspace.getCouplingList();
        //wspace.attachAgentsToCouplings(couplings);

        // Graphics clean up
        Workspace.getInstance().setTitle(f.getName());
        Workspace.getInstance().setCurrentFile(f);
        Workspace.getInstance().setWorkspaceChanged(false);
    }

    /**
     * Save workspace information.
     *
     * @param ws reference to current workspace
     * @param theFile file to save information to
     */
    public static void writeWorkspace(final File theFile) {
        WorkspaceSerializer serializer = new WorkspaceSerializer();

        //initComponentBounds(ws);

        LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

        try {
            FileWriter writer = new FileWriter(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "workspace_mapping.xml");

            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            //marshaller.setDebug(true);
            marshaller.marshal(serializer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Workspace.getInstance().setTitle(theFile.getName());
        Workspace.getInstance().setWorkspaceChanged(false);
    }
}
