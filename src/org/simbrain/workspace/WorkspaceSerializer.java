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

import javax.swing.JFileChooser;

import org.simbrain.util.SFileChooser;
import org.simbrain.workspace.gui.WorkspaceChangedDialog;

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
    
    /** File system property. */
    private static final String FS = System.getProperty("file.separator");
    
    /** List of workspace components. */
    private ArrayList<WorkspaceComponentProxy> componentList = new ArrayList<WorkspaceComponentProxy>();

    private final Workspace workspace;
    
    public WorkspaceSerializer(Workspace workspace) {
        this.workspace = workspace;
    }
    
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
    public void readWorkspace(final File f, final boolean isImport) {

        workspace.clearWorkspace();
//        WorkspaceSerializer serializer = null;
//        FileReader reader;
//        try {
//            reader = new FileReader(f);
//            serializer = (WorkspaceSerializer) getXStream().fromXML(reader);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        for (WorkspaceComponentProxy component : serializer.getComponentList()) {
//            try {
//                WorkspaceComponent theComponent = (WorkspaceComponent) component.getComponentClass().newInstance();
//                // TODO handle desktop components
////                theComponent.setBounds(component.getX(), component.getY(), component.getHeight(), component.getWidth());
////                theComponent.setPath(component.getPath());
//
//		    workspace.addWorkspaceComponent(theComponent);
//                if (component.getPath() != null) {
//                    if (isImport) {
//                        // TODO fix
//                        //theComponent.open(new File(WorkspacePreferences.getCurrentDirectory() + "/" + theComponent.getTitle())); // TODO: This is not returning the right string yet....
//                    } else {
//                        theComponent.open(new File(component.getPath()));
//                    }
//                }
//
//            } catch (InstantiationException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//        }

        // Graphics clean up
        // TODO
//        Workspace.getInstance().setTitle(f.getName());
        workspace.setCurrentFile(f);
        workspace.setWorkspaceChanged(false);
    }

    /**
     * Save workspace information.
     *
     * @param theFile file to save information to
     */
    public void writeWorkspace(final File theFile) {

        ArrayList<WorkspaceComponentProxy> list = new ArrayList<WorkspaceComponentProxy>();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            // TODO fix
//            WorkspaceComponentProxy proxy = new WorkspaceComponentProxy(component.getPath(), component.getName(), 
//                            component.getClass(), component.getX(), component.getY(), component.getWidth(), component.getHeight());
//            list.add(proxy);
        }
        setComponentList(list);

        String xml = getXStream().toXML(this);
        try {
            FileWriter writer  = new FileWriter(theFile);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO
//        Workspace.getInstance().setTitle(theFile.getName());
        workspace.setWorkspaceChanged(false);
    }

    /**
     * Export a workspace file: that is, save all workspace components and then a simple
     * workspace file correpsonding to them.
     */
    public void exportWorkspace() {
        String currentDirectory = WorkspacePreferences.getCurrentDirectory();
        
        SFileChooser chooser = new SFileChooser(currentDirectory, "sim");
        File simFile = chooser.showSaveDialog();

        if (simFile == null) {
            return;
        }

        WorkspacePreferences.setCurrentDirectory(simFile.getParent());
        currentDirectory = simFile.getParent();

        String newDir = simFile.getName().substring(0, simFile.getName().length() - 4);
        String newDirPath = simFile.getParent() + FS + newDir;
        String exportName = newDirPath + FS + simFile.getName();

        // Make the new directory
        boolean success = new File(newDirPath).mkdir();
        if (!success) {
            return;
        }
        
        // TODO
//        for (WorkspaceComponent component : componentList) {
//            String pathName = checkName(component.getTitle(), component.getFileExtension());
//            File file = new File(newDirPath, pathName);
//            component.save(file);
//            component.setStringReference(new File(pathName));
//        }

        writeWorkspace(new File(exportName));
    }
    
    /**
     * Import a workspace.  Assumes the workspace file has the same name as the directory
     * which contains the exported workspace.
     */
    public void importWorkspace() {
        if (workspace.changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(workspace);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }
//        workspaceChanged = false;

        String currentDirectory = WorkspacePreferences.getCurrentDirectory();
        
        JFileChooser simulationChooser = new JFileChooser();
        simulationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File dir = new File(currentDirectory);
        try {
           simulationChooser.setCurrentDirectory(dir.getCanonicalFile());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        simulationChooser.showOpenDialog(null);
        File simFile = simulationChooser.getSelectedFile();

        if (simFile != null) {
            String path = simFile + FS + simFile.getName() + ".sim";
            File theFile = new File(path);
            currentDirectory = simFile.getParent();
            WorkspacePreferences.setCurrentDirectory(simFile.getParent());
            readWorkspace(theFile, true);
        }
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
