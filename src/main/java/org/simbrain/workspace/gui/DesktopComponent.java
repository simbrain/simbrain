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
package org.simbrain.workspace.gui;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.pmw.tinylog.Logger;
import org.simbrain.network.NetworkComponent;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.events.WorkspaceComponentEvents2;
import org.simbrain.workspace.serialization.WorkspaceComponentDeserializer;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * A gui view on a {@link org.simbrain.workspace.WorkspaceComponent}.
 *
 * All desktop component graphical updates should happen via events.
 *
 * For custom size overrides {@link #getPreferredSize()}
 *
 * @param <E> the type of the workspace component.
 */
public abstract class DesktopComponent<E extends WorkspaceComponent> extends JPanel {

    /**
     * serial version UID.
     */
    private final long serialVersionUID = 1L;

    /**
     * Default size for new components;
     */
    private final Dimension DEFAULT_SIZE = new Dimension(500,500);

    /**
     * Reference to workspace component.
     */
    private E workspaceComponent;

    /**
     * File Chooser.
     */
    private final SFileChooser chooser;

    /**
     * Reference to parent frame.
     */
    private GenericFrame parentFrame;

    /**
     * Reference to parent desktop.
     */
    private SimbrainDesktop desktop;

    /**
     * Construct a workspace component.
     *
     * @param frame              the parent frame.
     * @param workspaceComponent the component to wrap.
     */
    public DesktopComponent(final GenericFrame frame, final E workspaceComponent) {
        super();
        this.parentFrame = frame;
        this.workspaceComponent = workspaceComponent;
        String defaultDirectory = getDefaultDirectory(workspaceComponent.getClass());

        chooser = new SFileChooser(defaultDirectory, null);
        for (String format : workspaceComponent.getFormats()) {
            chooser.addExtension(format);
        }

        // Add a default update listener
        WorkspaceComponentEvents2 events = workspaceComponent.getEvents();

        events.getGuiToggled().on(() -> DesktopComponent.this.getParentFrame().setVisible(workspaceComponent.isGuiOn()));
        events.getComponentClosing().on(this::close);

        Logger.trace(this.getClass().getCanonicalName() + " created");
    }

    /**
     * Closes this view.
     */
    public void close() {
        if (workspaceComponent.hasChangedSinceLastSave()) {
            boolean hasCancelled = showHasChangedDialog();
            if (hasCancelled) {
                return;
            }
        }
        workspaceComponent.close();
    }

    /**
     * Calls up a dialog for opening a workspace component.
     */
    @SuppressWarnings("unchecked")
    public void showOpenFileDialog() {

        SFileChooser chooser = new SFileChooser(getDefaultDirectory(workspaceComponent.getClass()), null);

        for (String format : workspaceComponent.getFormats()) {
            chooser.addExtension(format);
        }

        File file = chooser.showOpenDialog();
        String dir = file.getParentFile().getAbsolutePath();
        String name = file.getName();
        String ext = SFileChooser.getExtension(file);

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, String.format("File %s was not found.", file));
            return;
        }

        E newComponent;
        try {
            newComponent = (E) WorkspaceComponentDeserializer.deserializeWorkspaceComponent(workspaceComponent.getClass(), name, inputStream, ext);
        } catch (ReflectiveOperationException ex) {
            String message = String.format("Failed to deserialize workspace component %s\nCould not execute open method in class %s.", name, workspaceComponent.getClass().getSimpleName());
            JOptionPane.showMessageDialog(null, message);
            ex.printStackTrace();
            return;
        }

        Rectangle bounds = getParentFrame().getBounds();
        Workspace workspace = workspaceComponent.getWorkspace();
        workspace.removeWorkspaceComponent(workspaceComponent);
        workspaceComponent = newComponent;

        workspace.addWorkspaceComponent(workspaceComponent);
        workspaceComponent.setCurrentFile(file);
        setDefaultDirectory(workspaceComponent.getClass(), dir);
        SimbrainDesktop desktop = SimbrainDesktop.getDesktop(workspace);
        DesktopComponent desktopComponent = desktop.getDesktopComponent(workspaceComponent);
        desktop.registerComponentInstance(workspaceComponent, desktopComponent);
        desktopComponent.getParentFrame().setBounds(bounds);
        workspaceComponent.setName(name);
        getParentFrame().setTitle(name);
    }

    /**
     * Show the dialog for saving a workspace component.
     */
    public void showSaveFileDialog() {
        File theFile = workspaceComponent.getCurrentFile();

        if (theFile == null) {
            theFile = new File(getName());
        }

        theFile = chooser.showSaveDialog(theFile);

        if (theFile != null) {
            workspaceComponent.setCurrentFile(theFile);

            try {
                FileOutputStream stream = new FileOutputStream(theFile);
                // TODO format?
                workspaceComponent.save(stream, null);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            // workspaceComponent.setCurrentDirectory(theFile.getParentFile()
            // .getAbsolutePath());
            setDefaultDirectory(workspaceComponent.getClass(), theFile.getParentFile().getAbsolutePath());
            workspaceComponent.setName(theFile.getName());
            getParentFrame().setTitle(workspaceComponent.getName());
        }
    }

    /**
     * Save vs. save-as. Saves the currentfile.
     */
    public void save() {
        // System.out.println("Network save:" +
        // workspaceComponent.getCurrentFile());
        if (workspaceComponent.getCurrentFile() == null) {
            showSaveFileDialog();
        } else {
            try {
                FileOutputStream stream = new FileOutputStream(workspaceComponent.getCurrentFile());
                workspaceComponent.save(stream, null);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Writes the bounds of this desktop component to the provided stream.
     *
     * @param ostream the stream to write to
     * @throws IOException if an IO error occurs
     */
    public void save(final OutputStream ostream) throws IOException {
        new XStream(new DomDriver()).toXML(this.getParentFrame().getBounds(), ostream);
    }

    /**
     * Creates a new desktop component from the provided stream.
     *
     * @param component the component to create the desktop component for.
     * @param istream   the inputstream containing the serialized data.
     * @param name      the name of the desktop component.
     * @return a new component.
     */
    public static DesktopComponent<?> open(final WorkspaceComponent component, final InputStream istream, final String name) {
        // SimbrainDesktop desktop =
        // SimbrainDesktop.getDesktop(component.getWorkspace());
        DesktopComponent<?> dc = SimbrainDesktop.createDesktopComponent(null, component);
        Rectangle bounds = (Rectangle) new XStream(new DomDriver()).fromXML(istream);

        dc.setTitle(name);
        dc.setBounds(bounds);

        return dc;
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     *
     * @return true if user cancels
     */
    public boolean showHasChangedDialog() {
        Object[] options = {"Save", "Don't Save", "Cancel"};
        int s = JOptionPane.showInternalOptionDialog(this, "This component has changed since last save,\n" + "Would you like to save these changes?", "Component Has Changed", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == JOptionPane.OK_OPTION) {
            this.save();
            workspaceComponent.close();
            return false;
        } else if (s == JOptionPane.NO_OPTION) {
            workspaceComponent.close();
            return false;
        } else if (s == JOptionPane.CANCEL_OPTION) {
            return true;
        }
        return false;
    }

    /**
     * Return name of underlying component.
     *
     * @return the name of underlying component.
     */
    public String getName() {
        return (workspaceComponent == null) ? "null" : workspaceComponent.getName();
    }

    public void setTitle(final String name) {
        getParentFrame().setTitle(name);
    }

    public String getTitle() {
        return getParentFrame().getTitle();
    }

    /**
     * Retrieves a simple version of a component name from its class, e.g.
     * "Network" from "org.simbrain.network.NetworkComponent"/
     *
     * @return the simple name.
     */
    public String getSimpleName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Component")) {
            simpleName = simpleName.replaceFirst("Component", "");
        }
        return simpleName;
    }

    /**
     * Returns the workspace component wrapped by this instance.
     *
     * @return the workspace component wrapped by this instance.
     */
    public E getWorkspaceComponent() {
        return workspaceComponent;
    }

    /**
     * Sets the parent frame of this view.
     *
     * @param parentFrame the new parent.
     */
    public void setParentFrame(final GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Returns the parent from of this view.
     *
     * @return the parent from of this view.
     */
    public GenericFrame getParentFrame() {
        return this.parentFrame;
    }

    public SimbrainDesktop getDesktop() {
        return desktop;
    }

    /**
     * TODO: This should really be set at construction time, but that would
     * require deep changes so this should suffice for now.
     *
     * @param desktop the desktop to set
     */
    public void setDesktop(SimbrainDesktop desktop) {
        this.desktop = desktop;
    }

    /**
     * Returns the default directory for specific component types.
     *
     * @param componentType the component type
     * @return the directory
     */
    private String getDefaultDirectory(Class<? extends WorkspaceComponent> componentType) {
        String defaultDirectory;
        if (componentType == OdorWorldComponent.class) {
            defaultDirectory = SimbrainPreferences.getString("workspaceOdorWorldDirectory");
        } else if (componentType == DataWorldComponent.class) {
            defaultDirectory = SimbrainPreferences.getString("workspaceTableDirectory");
        } else if (componentType == NetworkComponent.class) {
            defaultDirectory = SimbrainPreferences.getString("workspaceNetworkDirectory");
        } else {
            defaultDirectory = SimbrainPreferences.getString("workspaceBaseDirectory");
        }
        return defaultDirectory;
    }

    /**
     * Set the default directory for specific component types.
     *
     * @param componentType the component type
     * @param dir           the directory to set
     */
    private void setDefaultDirectory(Class<? extends WorkspaceComponent> componentType, String dir) {
        if (componentType == OdorWorldComponent.class) {
            SimbrainPreferences.putString("workspaceOdorWorldDirectory", dir);
        } else if (componentType == DataWorldComponent.class) {
            SimbrainPreferences.putString("workspaceTableDirectory", dir);
        } else if (componentType == NetworkComponent.class) {
            SimbrainPreferences.putString("workspaceNetworkDirectory", dir);
        } else {
            SimbrainPreferences.putString("workspaceBaseDirectory", dir);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return DEFAULT_SIZE;
    }
}
