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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>WorkspaceSerializer</b> handles workspace persistence.  It contains static methods for reading and writing
 * workspace files, and also serves as a buffer for Castor initialization.  Was not sure how to persist a singleton so
 * I use this.
 *
 * Essentially the way this works is it holds a copy of the list of workspace components and uses these to rebuild a
 * workspace when it is open.
 *
 */
public class WorkspaceSerializer {

    /** List of workspace components. */
    private ArrayList<WorkspaceComponentProxy> componentList = new ArrayList<WorkspaceComponentProxy>();

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("workspace", WorkspaceSerializer.class);
        xstream.alias("workspaceComponent", WorkspaceComponentProxy.class);
        return xstream;
    }

    /**
     * Read  workspace file.
     *
     * @param f file containing new workspace information
     * @param isImport whether this workspace is being imported or opened
     */
    public static void readWorkspace(final File f, final boolean isImport) {

        Workspace.getInstance().clearWorkspace();
        WorkspaceSerializer serializer = null;

        FileReader reader;
        try {
            reader = new FileReader(f);
            serializer = (WorkspaceSerializer) getXStream().fromXML(reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (WorkspaceComponentProxy component : serializer.getComponentList()) {
            try {
                WorkspaceComponent theComponent = (WorkspaceComponent) component.getComponentClass().newInstance();
                Workspace.getInstance().addWorkspaceComponent(theComponent);
                theComponent.setBounds(component.getX(), component.getY(), component.getHeight(), component.getWidth());
                if (isImport) {
                    theComponent.open(new File(Workspace.getInstance().getCurrentDirectory() + "/" + theComponent.getTitle())); // TODO: This is not returning the right string yet....
                } else {
                    theComponent.open(new File(component.getPath())); 
                }

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // Graphics clean up
        Workspace.getInstance().setTitle(f.getName());
        Workspace.getInstance().setCurrentFile(f);
        Workspace.getInstance().setWorkspaceChanged(false);
    }

    /**
     * Save workspace information.
     *
     * @param theFile file to save information to
     */
    public static void writeWorkspace(final File theFile) {

        WorkspaceSerializer serializer = new WorkspaceSerializer();

        ArrayList<WorkspaceComponentProxy> list = new ArrayList<WorkspaceComponentProxy>();
        for (WorkspaceComponent component : Workspace.getInstance().getComponentList()) {
            WorkspaceComponentProxy proxy = new WorkspaceComponentProxy(component.getPath(), component.getName(), 
                            component.getClass(), component.getX(), component.getY(), component.getWidth(), component.getHeight());
            list.add(proxy);
        }
        serializer.setComponentList(list);

        String xml = getXStream().toXML(serializer);
        try {
            FileWriter writer  = new FileWriter(theFile);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Workspace.getInstance().setTitle(theFile.getName());
        Workspace.getInstance().setWorkspaceChanged(false);
    }


    /**
     * @return the componentList
     */
    public ArrayList<WorkspaceComponentProxy> getComponentList() {
        return componentList;
    }

    /**
     * @param componentList the componentList to set
     */
    public void setComponentList(ArrayList<WorkspaceComponentProxy> componentList) {
        this.componentList = componentList;
    }


}
