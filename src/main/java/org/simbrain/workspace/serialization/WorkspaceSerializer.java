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
package org.simbrain.workspace.serialization;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateAction;
import org.simbrain.workspace.updater.UpdateActionManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Serializes and deserializes workspaces. Custom serialization (beyond what
 * XStream can do) is required, in order to recreate workspace components and
 * couplings from a legible xml form / zipped directory structure. Mainly this
 * means recreating components, couplings, and update actions. Also some effort
 * has been made to allow reuse between individual component save / reopen and
 * workspace level save / reopen.  Some additional information is in
 * {@link Workspace}.
 *
 * @author Matt Watson
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class WorkspaceSerializer {

    /**
     * The number of bytes to attempt to read at a time from an InputStream.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * The current workspace.
     */
    private Workspace workspace;

    /**
     * The desktop component for the workspace.
     */
    private SimbrainDesktop desktop;

    /**
     * Creates a new serializer.
     *
     * @param workspace The workspace to serialize to or from.
     */
    public WorkspaceSerializer(Workspace workspace) {
        this.workspace = workspace;
        this.desktop = SimbrainDesktop.getDesktop(workspace);
    }

    /**
     * Serializes the workspace to a zip compressed stream.
     *
     * @param output The output stream to write to.
     * @throws IOException If there is an IO error.
     */
    public void serialize(OutputStream output) throws IOException {

        // Create the zip output stream. ZipStream is a sequence of
        // ZipEntries, with extra utilities for iterating over them.
        // Each zipentry corresponds to a single file in the zip archive, a
        // String with the relative path in the archive to the entry (e.g.
        // "gui/network.xml"), and a bytearray for the file itself.
        ZipOutputStream zipStream = new ZipOutputStream(output);
        WorkspaceComponentSerializer serializer = new WorkspaceComponentSerializer();

        // This archive object saves all the information about the workspace. It
        // will be saved as a zipentry "contents.xml"
        ArchivedWorkspace archive = new ArchivedWorkspace(workspace, serializer);

        // Currently sorts components by a serialization priority
        workspace.preSerializationInit();

        serializeComponents(serializer, archive, zipStream);
        serializeCouplings(archive);
        serializeUpdateActions(archive);

        ZipEntry entry = new ZipEntry("contents.xml");
        zipStream.putNextEntry(entry);
        archive.toXml(zipStream);
        zipStream.finish();
    }

    /**
     * Serializes all the components to the given archive and zipstream.
     *
     * @param serializer The serializer for the components.
     * @param archive    The archive contents to update.
     * @param zipStream  The zipstream to write to.
     * @throws IOException If there is an IO error.
     */
    private void serializeComponents(WorkspaceComponentSerializer serializer, ArchivedWorkspace archive, ZipOutputStream zipStream) throws IOException {
        List<WorkspaceComponent> components = sortComponentsByPriority();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            serializeComponent(serializer, archive, component, zipStream);
        }
    }

    private List<WorkspaceComponent> sortComponentsByPriority() {
        List<WorkspaceComponent> components = new ArrayList<WorkspaceComponent>();
        components.addAll(workspace.getComponentList());
        Collections.sort(components, new Comparator<WorkspaceComponent>() {
            public int compare(WorkspaceComponent c1, WorkspaceComponent c2) {
                return Integer.compare(c1.getSerializePriority(), c2.getSerializePriority());
            }
        });
        return components;
    }

    /**
     * Serialize one component to the zip stream
     *
     * @param serializer The serializer for the components.
     * @param archive    The archive contents to update.
     * @param component the component to serialize
     * @param zipStream  The zipstream to write to.
     */
    private void serializeComponent(WorkspaceComponentSerializer serializer, ArchivedWorkspace archive, WorkspaceComponent component, ZipOutputStream zipStream) {
        ArchivedWorkspaceComponent archiveComp = archive.addComponent(component);
        ZipEntry entry = new ZipEntry(archiveComp.getUri());
        try {
            zipStream.putNextEntry(entry);
            serializer.serializeComponent(component, zipStream);
            GuiComponent<?> desktopComponent = SimbrainDesktop.getDesktop(workspace).getDesktopComponent(component);
            if (desktopComponent != null) {
                ArchivedWorkspaceComponent.ArchivedDesktopComponent dc = archiveComp.addDesktopComponent(desktopComponent);
                entry = new ZipEntry(dc.getUri());
                zipStream.putNextEntry(entry);
                desktopComponent.save(zipStream);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Serialize couplings.
     *
     * @param archive the archive objet to serialize to
     */
    private void serializeCouplings(ArchivedWorkspace archive) {
        HashMap<Object, WorkspaceComponent> couplingComponents = mapCouplingComponents();
        for (Coupling<?> coupling : workspace.getCouplings()) {
            serializeCoupling(couplingComponents, coupling, archive);
        }
    }

    /**
     * Returns a map from coupling base objects ("models") to their parent components.
     */
    private HashMap<Object, WorkspaceComponent> mapCouplingComponents() {
        HashMap<Object, WorkspaceComponent> couplingComponents = new HashMap<>();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            for (Object object : component.getAttributeContainers()) {
                couplingComponents.put(object, component);
            }
        }
        return couplingComponents;
    }

    /**
     * Add a serialized coupling to the archive.
     *
     * @param couplingComponents a map from couplings to components
     * @param coupling the coupling to save
     * @param archive the archive object to save to
     */
    private void serializeCoupling(HashMap<Object, WorkspaceComponent> couplingComponents, Coupling<?> coupling, ArchivedWorkspace archive) {
        ArchivedAttribute producer = new ArchivedAttribute(couplingComponents.get(coupling.getProducer().getBaseObject()), coupling.getProducer());
        ArchivedAttribute consumer = new ArchivedAttribute(couplingComponents.get(coupling.getConsumer().getBaseObject()), coupling.getConsumer());
        archive.addCoupling(new ArchivedCoupling(producer, consumer));
    }

    /**
     * Serialize all update actions  in the current workspace
     *
     * @param archive the archive object to serialize to
     */
    private void serializeUpdateActions(ArchivedWorkspace archive) {
        for (UpdateAction action : workspace.getUpdater().getUpdateManager().getActionList()) {
            archive.addUpdateAction(action);
        }
    }

    /**
     * Creates a workspace from a zip compressed input stream.
     *
     * @param stream The stream to read from. This is expected to be zip compressed.
     * @throws IOException if an IO error occurs.
     */
    @SuppressWarnings("unchecked")
    public void deserialize(InputStream stream) throws IOException {
        Map<String, byte[]> byteArrays = processInputStream(stream);
        ArchivedWorkspace archive = (ArchivedWorkspace) ArchivedWorkspace.xstream().fromXML(new ByteArrayInputStream(byteArrays.get("contents.xml")));

        WorkspaceComponentDeserializer deserializer = new WorkspaceComponentDeserializer();
        deserializeComponents(archive, deserializer, byteArrays);

        deserializeCouplings(archive);
        deserializeUpdateActions(archive, deserializer);
        deserializeWorkspaceParameters(archive);
    }

    private Map<String, byte[]> processInputStream(InputStream stream) throws IOException {
        // Populate the byte stream BUFFER_SIZE at a time and create a zip input
        // stream (currently 1 kb at a time).
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int read; (read = stream.read(buffer)) >= 0; ) {
            bytes.write(buffer, 0, read);
        }
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));

        // Populate a map from zip entries (strings containing path+file info in
        // zip archive) to the associated data
        Map<String, byte[]> byteArrays = new HashMap<String, byte[]>();
        ZipEntry entry = zip.getNextEntry();
        // Initialize byte arrays
        for (ZipEntry next; entry != null; entry = next) {
            next = zip.getNextEntry();
            byteArrays.put(entry.getName(), new byte[(int) entry.getSize()]);
        }
        zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        // Populate byte arrays
        while ((entry = zip.getNextEntry()) != null) {
            byte[] data = byteArrays.get(entry.getName());
            read(zip, data);
        }

        // Find the contents.xml file and set the zip entries relative to that
        Set<String> zipEntries = new HashSet<String>(byteArrays.keySet());
        String contentsFile = "contents.xml";
        String contentsPath = "";
        for (String entryName : zipEntries) {
            if (entryName.endsWith(contentsFile)) {
                contentsPath = entryName.substring(0, entryName.length() - contentsFile.length());
            }
        }

        // Remove the contents path from all entries that have it
        if (!contentsPath.isEmpty()) {
            for (String entryName : zipEntries) {
                if (entryName.startsWith(contentsPath)) {
                    byteArrays.put(entryName.replace(contentsPath, ""), byteArrays.get(entryName));
                    byteArrays.remove(entryName);
                }
            }
        }
        return byteArrays;
    }

    private void deserializeComponents(ArchivedWorkspace archive, WorkspaceComponentDeserializer deserializer, Map<String, byte[]> byteArrays) {
        if (archive.getArchivedComponents() != null) {
            for (ArchivedWorkspaceComponent archivedComponent : archive.getArchivedComponents()) {
                try {
                    WorkspaceComponent wc = deserializer.deserializeWorkspaceComponent(archivedComponent, new ByteArrayInputStream(byteArrays.get(archivedComponent.getUri())));
                    workspace.addWorkspaceComponent(wc);
                    if (archivedComponent.getDesktopComponent() != null) {
                        Rectangle bounds = (Rectangle) new XStream(new DomDriver()).fromXML(new ByteArrayInputStream(byteArrays.get(archivedComponent.getDesktopComponent().getUri())));
                        GuiComponent<?> desktopComponent = desktop.getDesktopComponent(wc);
                        desktopComponent.getParentFrame().setBounds(bounds);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String message = String.format("Failed to deserialize component %s.", archivedComponent.getName());
                    JOptionPane.showMessageDialog(null, message);
                }
            }
        }
    }

    private void deserializeCouplings(ArchivedWorkspace archive) {
        if (archive.getArchivedCouplings() != null) {
            for (ArchivedCoupling archivedCoupling : archive.getArchivedCouplings()) {
                Producer producer = archivedCoupling.createProducer(workspace);
                Consumer consumer = archivedCoupling.createConsumer(workspace);
                workspace.getCouplingManager().tryCoupling(producer, consumer);
            }
        }
    }

    private void deserializeUpdateActions(ArchivedWorkspace archive, WorkspaceComponentDeserializer deserializer) {
        UpdateActionManager manager = workspace.getUpdater().getUpdateManager();
        manager.clear();
        if (archive.getArchivedActions() != null) {
            for (ArchivedUpdateAction archivedAction : archive.getArchivedActions()) {
                manager.addAction(archive.createUpdateAction(workspace, deserializer, archivedAction));
            }
        }
        if (!manager.getActionList().contains(workspace.getUpdater().getSyncUpdateAction())) {
            manager.addAction(workspace.getUpdater().getSyncUpdateAction());
        }
    }

    private void deserializeWorkspaceParameters(ArchivedWorkspace archive) {
        if (archive.getWorkspaceParameters() != null) {
            workspace.setUpdateDelay(archive.getWorkspaceParameters().getUpdateDelay());
            workspace.getUpdater().setTime(archive.getWorkspaceParameters().getSavedTime());
        }
    }

    /**
     * Helper method that will read the InputStream repeatedly until the given
     * array is filled.
     *
     * @param istream the InputStream to read from.
     * @param bytes   the array to write to
     * @throws IOException if there is an IO error
     */
    private static void read(InputStream istream, byte[] bytes) throws IOException {
        int pos = 0;
        while (pos < bytes.length) {
            int read = istream.read(bytes, pos, bytes.length - pos);
            if (read < 0) {
                throw new RuntimeException("premature EOF");
            }
            pos += read;
        }
    }

    public static <T> WorkspaceComponent showOpenComponentDialog(Class<T> type) {
        String defaultDirectory = SimbrainPreferences.getString("workspace" + type.getSimpleName() + "Directory");
        SFileChooser chooser = new SFileChooser(defaultDirectory, "XML File", "xml");
        File file = chooser.showOpenDialog();
        if (file != null) {
            return WorkspaceSerializer.open(type, file);
        } else {
            return null;
        }
    }

    /**
     * Helper method for openings workspace components from a file.
     * <p>
     * A call might look like this <code>NetworkComponent networkComponent =
     * (NetworkComponent) WorkspaceFileOpener(NetworkComponent.class, new File("Net.xml"));</code>
     *
     * @param fileClass the type of Workpsace component to open; a subclass of WorkspaceComponent.
     * @param file      the File to open
     * @return the workspace component
     */
    public static WorkspaceComponent open(Class<?> fileClass, File file) {
        String extension = file.getName().substring(file.getName().indexOf("."));
        try {
            Method method = fileClass.getMethod("open", InputStream.class, String.class, String.class);
            WorkspaceComponent wc = (WorkspaceComponent) method.invoke(null, new FileInputStream(file),
                    file.getName(), extension);
            wc.setCurrentFile(file);
            wc.setChangedSinceLastSave(false);
            return wc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to save a specified file.
     *
     * @param file      file to save.
     * @param workspace reference to workspace
     */
    public static void save(File file, Workspace workspace) {
        if (file != null) {
            try {
                FileOutputStream ostream = new FileOutputStream(file);
                try {
                    WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);
                    serializer.serialize(ostream);
                    workspace.setWorkspaceChanged(false);
                } finally {
                    ostream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
