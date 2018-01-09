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

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateAction;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Serializes and deserializes workspaces. Custom serialization (beyond what
 * XStream can do) is required, in order to recreate workspace components and
 * couplings from a legible xml form / zipped directory structure. Mainly this
 * means recreating components, couplings, and update actions. Also some effort
 * has been made to allow reuse between individual component save / reopen and
 * workspace level save / reopen.
 *
 * @author Matt Watson
 */
public class WorkspaceSerializer {

    /** The number of bytes to attempt to read at a time from an InputStream. */
    private static final int BUFFER_SIZE = 1024;

    /** The current workspace. */
    private final Workspace workspace;

    /** The desktop component for the workspace. */
    private final SimbrainDesktop desktop;

    /**
     * Creates a new serializer.
     *
     * @param workspace The workspace to serialize to or from.
     */
    public WorkspaceSerializer(final Workspace workspace) {
        this.workspace = workspace;
        this.desktop = SimbrainDesktop.getDesktop(workspace);
    }

    /**
     * Serializes the workspace to a zip compressed stream.
     *
     * @param output The output stream to write to.
     * @throws IOException If there is an IO error.
     */
    public void serialize(final OutputStream output) throws IOException {

        // Create the zip output stream. ZipStream is a sequence of
        // ZipEntries, with extra utilities for iterating over them.
        // Each zipentry corresponds to a single file in the zip archive, a
        // String with the relative path in the archive to the entry (e.g.
        // "gui/network.xml"), and a bytearray for the file itself.
        ZipOutputStream zipStream = new ZipOutputStream(output);
        WorkspaceComponentSerializer serializer = new WorkspaceComponentSerializer();

        // This archive object saves all the information about the workspace. It
        // will be saved as a zipentry "contents.xml"
        ArchiveContents archive = new ArchiveContents(workspace, serializer);

        // Currently sorts components by a serialization priority
        workspace.preSerializationInit();

        // Serialize components
        serializeComponents(serializer, archive, zipStream);

        // Serialize couplings
        for (Coupling<?> coupling : workspace.getCouplings()) {
            archive.addCoupling(coupling);
        }

        // Serialize update actions
        for (UpdateAction action : workspace.getUpdater().getUpdateManager()
                .getActionList()) {
            archive.addUpdateAction(action);
        }

        ZipEntry entry = new ZipEntry("contents.xml");
        zipStream.putNextEntry(entry);
        archive.toXml(zipStream);
        zipStream.finish();
    }

    /**
     * Serializes all the components to the given archive and zipstream.
     *
     * @param serializer The serializer for the components.
     * @param archive The archive contents to update.
     * @param zipStream The zipstream to write to.
     * @throws IOException If there is an IO error.
     */
    private void serializeComponents(
            final WorkspaceComponentSerializer serializer,
            final ArchiveContents archive, final ZipOutputStream zipStream)
            throws IOException {

        // Save the component entries and saves links to them in contents.xml
        for (WorkspaceComponent component : workspace.getComponentList()) {

            ArchiveContents.ArchivedComponent archiveComp = archive
                    .addComponent(component);

            // Initialize the entry with a path+name in the zip archive
            // e.g. "components/1_Network1.xml"
            ZipEntry entry = new ZipEntry(archiveComp.getUri());
            zipStream.putNextEntry(entry);

            // Write the data to the zipstream using the component's save
            // function which is also used in saving individual components
            serializer.serializeComponent(component, zipStream);

            /*
             * If there is a desktop component associated with the component
             * it's serialized here.
             */
            GuiComponent<?> desktopComponent = SimbrainDesktop
                    .getDesktop(workspace).getDesktopComponent(component);
            if (desktopComponent != null) {
                ArchiveContents.ArchivedComponent.ArchivedDesktopComponent dc = archiveComp
                        .addDesktopComponent(desktopComponent);
                entry = new ZipEntry(dc.getUri());
                zipStream.putNextEntry(entry);
                desktopComponent.save(zipStream);
            }
        }

        // Add couplings to the archive
        for (Coupling<?> coupling : workspace.getCouplings()) {
            archive.addCoupling(coupling);
        }

        // Add workspace actions to the archive
        for (UpdateAction action : workspace.getUpdater().getUpdateManager()
                .getActionList()) {
            archive.addUpdateAction(action);
        }
    }

    /**
     * Deserializes all the entries in the provided stream.
     *
     * @param stream The input stream.
     * @throws IOException If an IO error occurs.
     */
    public void deserialize(final InputStream stream) throws IOException {
        Collection<? extends String> empty = Collections.emptySet();
        deserialize(stream, empty);
    }

    // TODO: Remove exclude thing?
    /**
     * Creates a workspace from a zip compressed input stream.
     *
     * @param stream The stream to read from. This is expected to be zip
     *            compressed.
     * @param exclude The list of uris to ignore on import.
     * @throws IOException if an IO error occurs.
     */
    @SuppressWarnings("unchecked")
    public void deserialize(final InputStream stream,
            final Collection<? extends String> exclude) throws IOException {

        // Populate the byte stream BUFFER_SIZE at a time and create a zip input
        // stream (currently 1 kb at a time).
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int read; (read = stream.read(buffer)) >= 0;) {
            bytes.write(buffer, 0, read);
        }
        ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));

        // Populate a map from zip entries (strings containing path+file info in
        // zip archive) to the associated data
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ZipEntry entry = zip.getNextEntry();
        // Initialize byte arrays
        for (ZipEntry next; entry != null; entry = next) {
            next = zip.getNextEntry();
            entries.put(entry.getName(), new byte[(int) entry.getSize()]);
        }
        zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        // Populate byte arrays
        while ((entry = zip.getNextEntry()) != null) {
            byte[] data = entries.get(entry.getName());
            read(zip, data);
        }

        // When a user unzips and rezips a workspace file, additional
        // information is added to the beginnings of the entries which must
        // be stripped away.
        Set<String> zipEntries = new HashSet<String>(entries.keySet());
        for (String entName : zipEntries) {
            // These guys are ok
            if (entName.startsWith("guis" + File.separator)
                    || entName.startsWith("components" + File.separator)) {
                break;
            }
            // Replace the bad with the good
            String newname = entName;
            // TODO: improve regex to handle underscores etc...
            newname = newname.replaceFirst("^[a-zA-Z1-9]*\\/", "");
            if (!newname.equals(entName)) {
                entries.put(newname, entries.get(entName));
                entries.remove(entName);
            }
        }

        // Read contents.xml file and create a new archived contents file,
        // which will be used to recreate the workspace
        ArchiveContents contents = (ArchiveContents) ArchiveContents.xstream().fromXML(
                new ByteArrayInputStream(entries.get("contents.xml")));

        // Add Components
        WorkspaceComponentDeserializer componentDeserializer = new WorkspaceComponentDeserializer();
        if (contents.getArchivedComponents() != null) {
            for (ArchiveContents.ArchivedComponent archivedComponent : contents
                    .getArchivedComponents()) {
                if (exclude.contains(archivedComponent.getUri())) {
                    continue;
                }

                WorkspaceComponent wc = componentDeserializer
                        .deserializeWorkspaceComponent(archivedComponent,
                                new ByteArrayInputStream(entries
                                        .get(archivedComponent.getUri())));

                // Add the component to the workspace.
                // (Note this causes a desktop component (GuiComponent) to be
                // created)
                workspace.addWorkspaceComponent(wc);

                if (archivedComponent.getDesktopComponent() != null) {
                    Rectangle bounds = (Rectangle) new XStream(new DomDriver())
                            .fromXML(new ByteArrayInputStream(
                                    entries.get(archivedComponent
                                            .getDesktopComponent().getUri())));
                    GuiComponent<?> desktopComponent = desktop
                            .getDesktopComponent(wc);
                    desktopComponent.getParentFrame().setBounds(bounds);
                }
            }
        }

        // Add Couplings
        if (contents.getArchivedCouplings() != null) {
            for (ArchiveContents.ArchivedCoupling couplingRef : contents
                    .getArchivedCouplings()) {
 
                // if (exclude.contains(couplingRef.getArchivedProducer()
                // .getParentRef())
                // || exclude.contains(couplingRef.getArchivedProducer()
                // .getParentRef())) {
                // continue;
                // }


                //TODO
//                CouplingFactory cf = workspace.getCouplingFactory();
//                Producer<?> producer = cf.getProducer(couplingRef);
//                Consumer<?> consumer = cf.getConsumer(couplingRef);
//                cf.tryCoupling(producer, consumer);
                
            }
        }

        // Add update actions
        workspace.getUpdater().getUpdateManager().clear();
        if (contents.getArchivedActions() != null) {
            for (ArchiveContents.ArchivedUpdateAction actionRef : contents
                    .getArchivedActions()) {
                workspace.getUpdater().getUpdateManager()
                        .addAction(contents.createUpdateAction(workspace,
                                componentDeserializer, actionRef));
            }
        }

        // Deserialize workspace parameters (serialization occurs in
        // ArchiveContents.java).
        if (contents.getWorkspaceParameters() != null) {
            workspace.setUpdateDelay(
                    contents.getWorkspaceParameters().getUpdateDelay());
            workspace.getUpdater()
                    .setTime(contents.getWorkspaceParameters().getSavedTime());
        }
    }

    /**
     * Helper method that will read the InputStream repeatedly until the given
     * array is filled.
     *
     * @param istream the InputStream to read from.
     * @param bytes the array to write to
     * @throws IOException if there is an IO error
     */
    private static void read(final InputStream istream, final byte[] bytes)
            throws IOException {
        int pos = 0;
        while (pos < bytes.length) {
            int read = istream.read(bytes, pos, bytes.length - pos);
            if (read < 0) {
                throw new RuntimeException("premature EOF");
            }
            pos += read;
        }
    }

    /**
     * Helper method for openings workspace components from a file.
     *
     * A call might look like this <code>NetworkComponent networkComponent =
     *      (NetworkComponent) WorkspaceFileOpener(NetworkComponent.class, new File("Net.xml"));</code>
     *
     * @param fileClass the type of Workpsace component to open; a subclass of
     *            WorkspaceComponent.
     * @param file the File to open
     * @return the workspace component
     */
    public static WorkspaceComponent open(final Class<?> fileClass,
            final File file) {
        String extension = file.getName()
                .substring(file.getName().indexOf("."));
        try {
            Method method = fileClass.getMethod("open", InputStream.class,
                    String.class, String.class);
            WorkspaceComponent wc = (WorkspaceComponent) method.invoke(null,
                    new FileInputStream(file), file.getName(), extension);
            wc.setCurrentFile(file);
            wc.setChangedSinceLastSave(false);
            return wc;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to save a specified file.
     *
     * @param file file to save.
     * @param workspace reference to workspace
     */
    public static void save(File file, Workspace workspace) {
        if (file != null) {
            // System.out.println("Workspace Save -->" + file);
            try {
                FileOutputStream ostream = new FileOutputStream(file);
                try {
                    WorkspaceSerializer serializer = new WorkspaceSerializer(
                            workspace);
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
