package org.simbrain.workspace;

import java.io.File;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.simbrain.util.SFileChooser;

/**
 * Represents a window in the Simbrain desktop.   Services relating to
 * couplings and relations between are handled.  We may want to abstract
 * out some of the coupling management since much of this is focused on
 * the GUI aspects of the JInternalFrames.
 */
public abstract class WorkspaceComponent extends JInternalFrame {

    /** File system seperator. */
    public static final String FS = System.getProperty("file.separator");

    /** Has gauge changed since last save. */
    private boolean changedSinceLastSave = false;

    /** Current directory. So when re-opening this type of component the app remembers where to look. */
    private String currentDirectory;

    /** The name of this component.  Used in the title, in saving, etc. */
    private String name;

    /** The path to the saved representation fo this component. Used in persisting the workspace. */
    private String path;

    /**
     * Construct a workspace component.
     */
    public WorkspaceComponent() {
        super();
        setName(this.getClass().getSimpleName() + getWindowIndex());
        setResizable(true);
        setMaximizable(true);
        setIconifiable(true);
        setClosable(true);
        addInternalFrameListener(new WindowFrameListener());
    }

    /**
     * Used to name components, as in Network1, Network2, etc.
     *
     * @return the curent index
     */
    public abstract int getWindowIndex();

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
     * The window width to use when creating a new instance of this component.
     *
     * @return width
     */
    public abstract int getDefaultWidth();

    /**
     * The window height to use when creating a new instance of this component.
     *
     * @return height
     */
    public abstract int getDefaultHeight();

    /**
     * The default y location on the desktop to use when creating a new instance of this component.
     *
     * @return x location
     */
    public abstract int getDefaultLocationX();

    /**
     * The default x location on the desktop to use when creating a new instance of this component.
     *
     * @return y location
     */
    public abstract int getDefaultLocationY();

    /**
     * Update that goes beyond updating couplings.
     * Called when global workspace update is called.
     */
    public void updateComponent() {
        repaint();
    }

    /**
     * Return an unmodifiable list of producers for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of producers for this component
     */
    public abstract List<Producer> getProducers();

    /**
     * Return an unmodifiable list of consumers for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of consumers for this component
     */
    public abstract List<Consumer> getConsumers();

    /**
     * Return an unmodifiable list of couplings for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of couplings for this component
     */
    public abstract List<Coupling> getCouplings();

    /** Perform cleanup after closing. */
    public abstract void close();


    /**
     * Opens a file-save dialog and saves world information to the specified file.
     */
    public void save() {
        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), getFileExtension());
        File file = chooser.showSaveDialog();

        if (file != null) {
            save(file);
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    public void hasChanged() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane
                .showInternalOptionDialog(this,
                 "This SimbrainComponent has changed since last save,\nWould you like to save these changes?",
                 "SimbrainComponent Has Changed", JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            save();
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
     * Sets a path to this network in a manner independent of OS.  Used in persistence.
     *
     * @param path the path for this network frame
     */
    public void setPath(final String path) {
        String thePath = path;

        if (thePath.length() > 2) {
            if (thePath.charAt(2) == '.') {
                thePath = path.substring(2, path.length());
            }
        }

        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
    }

    /**
     * Network frame listener.
     */
    private class WindowFrameListener extends InternalFrameAdapter {

        /** @see InternalFrameAdapter */
        public void internalFrameClosing(final InternalFrameEvent e) {

//            workspace.getNetworkList().remove(NetworkComponent.this);
//
//            NetworkComponent lastNetworkFrame = workspace.getLastNetwork();
//            if (lastNetworkFrame != null) {
//                lastNetworkFrame.grabFocus();
//                workspace.repaint();
//            }
//            NetworkPreferences.setCurrentDirectory(getNetworkPanel().getCurrentDirectory());
            
            if (isChangedSinceLastSave()) {
                hasChanged();
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
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }
}
