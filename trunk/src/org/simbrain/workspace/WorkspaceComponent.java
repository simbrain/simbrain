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

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.apache.log4j.Logger;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;

/**
 * Represents a window in the Simbrain desktop.   Services relating to
 * couplings and relations between are handled.  We may want to abstract
 * out some of the coupling management since much of this is focused on
 * the GUI aspects of the JInternalFrames.
 */
public abstract class WorkspaceComponent extends JInternalFrame {

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(WorkspaceComponent.class);

    /** File system seperator. */
    public static final String FS = System.getProperty("file.separator");

    /** Whether this component has changed since last save. */
    private boolean changedSinceLastSave = false;

    /** Current directory. So when re-opening this type of component the app remembers where to look. */
    private String currentDirectory = ".";

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name  = "";

    /** The path to the saved representation fo this component. Used in persisting the workspace. */
    private String path;

    /** Current file.  For save (vs. save-as). */
    private File currentFile;

    /**
     * Construct a workspace component.
     */
    public WorkspaceComponent() {
        super();
        logger.trace(this.getClass().getCanonicalName() + " created");
        setResizable(true);
        setMaximizable(true);
        setIconifiable(true);
        setClosable(true);
        addInternalFrameListener(new WindowFrameListener());
    }

    /**
     * If any initialization is needed after adding this component to workspace.
     *
     */
    public void postAddInit() {
    }

    /**
     * Used when saving a workspace.  All changed workspace components are saved using
     * this method.
     *
     * @param saveFile the file to save.
     */
    public abstract void save(File saveFile);

    /**
     * When workspaces are opened, a path to a file is passed in.
     * So, all components which can be saved should have this.
     *
     * @param openFile file representing saved component.
     */
    public abstract void open(File openFile);


    /**
     * The file extension for a component type, e.g. ".net".
     *
     * @return the file extension
     */
    public abstract String getFileExtension();

    /**
     * Perform cleanup after closing.
    */
    public abstract void close();

    /**
     * Update that goes beyond updating couplings.
     * Called when global workspace update is called.
     */
    public void updateComponent() {
        repaint();
    }

    /**
     * Calls up a dialog for opening a workspace component.
     */
    public void showOpenFileDialog() {
        SFileChooser chooser = new SFileChooser(this.getCurrentDirectory(), this.getFileExtension());
        File theFile = chooser.showOpenDialog();
        if (theFile != null) {
            open(theFile);
            setCurrentDirectory(chooser.getCurrentLocation());
        }

    }

    /**
     * Show the dialog for saving a workspace component.
     */
    public void showSaveFileDialog() {
        SFileChooser chooser = new SFileChooser(this.getCurrentDirectory(), this.getFileExtension());
        if (getCurrentFile() != null) {
            chooser.setSelectedFile(getCurrentFile());
        } else {
            chooser.setSelectedFile(new File(getName() + "." + getFileExtension()));
        }
        File theFile = chooser.showSaveDialog();
        if (theFile != null) {
            save(theFile);
            setCurrentDirectory(chooser.getCurrentLocation());
            setChangedSinceLastSave(false);
        }
    }

    /**
     * Save vs. save-as.  Saves the currentfile.
     */
    public void save() {
        if (getCurrentFile() == null) {
            showSaveFileDialog();
        } else {
            save(currentFile);
        }
    }

    /**
     * Classes which override this method return a reference to coupling container,
     * which is used to manage couplings.
     *
     * @return coupling container, or null if the class has none.
     */
    public CouplingContainer getCouplingContainer() {
        return null;
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    private void showHasChangedDialog() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane
                .showInternalOptionDialog(this,
                 "This component has changed since last save,\nWould you like to save these changes?",
                 "Component Has Changed", JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            this.showSaveFileDialog();
            dispose();
        } else if (s == 1) {
            dispose();
        } else if (s == 2) {
            return;
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        setTitle(name);
        this.name = name;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the platform-specific path for this network frame.  Used in persistence.
     *
     * @return the platform-specific path for this network frame
     */
    public String getGenericPath() {
        String ret = path;

        if (path == null) {
            return null;
        }

        ret.replace('/', System.getProperty("file.separator").charAt(0));

        return ret;
    }

    /**
     * Sets a string path to this network in a manner independent of OS.  Used in persistence.
     *
     * @param path the path for this component
     */
    public void setStringReference(final File theFile) {
        String localDir = new String(System.getProperty("user.dir"));
        String thePath = Utils.getRelativePath(localDir, theFile.getAbsolutePath());
        if (thePath.length() > 2) {
            if (thePath.charAt(2) == '.') {
                thePath = path.substring(2, path.length());
            }
        }
        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
        setName(theFile.getName());
    }

    /**
     * Manage cleanup when a component is closed.
     */
    private class WindowFrameListener extends InternalFrameAdapter {
        /** @see InternalFrameAdapter */
        public void internalFrameClosing(final InternalFrameEvent e) {
            Workspace.getInstance().removeWorkspaceComponent(WorkspaceComponent.this);

            // NetworkPreferences.setCurrentDirectory(getNetworkPanel().getCurrentDirectory());

            if (isChangedSinceLastSave()) {
                showHasChangedDialog();
            } else {
                dispose();
            }
            close();
        }
    }

    /**
     * @param changedSinceLastSave the changedSinceLastSave to set
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        this.changedSinceLastSave = changedSinceLastSave;
    }

    /**
     * @return the changedSinceLastSave
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * This should be overriden if there are user preferences to get.
     *
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     *
     * This should be overriden if there are user preferences to set.
     *
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * @return the currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile the currentFile to set
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
}
