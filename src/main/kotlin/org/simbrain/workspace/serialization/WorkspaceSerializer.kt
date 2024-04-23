package org.simbrain.workspace.serialization

import org.simbrain.util.SFileChooser
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences.baseDirectory
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.gui.SimbrainDesktop.getDesktopComponent
import java.awt.Rectangle
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.swing.JOptionPane

class WorkspaceSerializer(val workspace: Workspace) {

    /**
     * Serializes the workspace to a zip compressed stream.
     *
     * @param output The output stream to write to.
     * @throws IOException If there is an IO error.
     */
    @JvmOverloads
    fun serialize(output: OutputStream, headless: Boolean = false) {
        // Create the zip output stream. ZipStream is a sequence of
        // ZipEntries, with extra utilities for iterating over them.
        // Each zipentry corresponds to a single file in the zip archive, a
        // String with the relative path in the archive to the entry (e.g.
        // "gui/network.xml"), and a bytearray for the file itself.

        val zipStream = ZipOutputStream(output)
        val serializer = WorkspaceComponentSerializer()

        // This archive object saves all the information about the workspace. It
        // will be saved as a zipentry "contents.xml"
        val archive = ArchivedWorkspace(workspace, serializer)

        // Currently sorts components by a serialization priority
        workspace.preSerializationInit()

        serializeComponents(serializer, archive, zipStream, headless)
        serializeCouplings(archive)

        // serializeUpdateActions(archive);
        val entry = ZipEntry("contents.xml")
        zipStream.putNextEntry(entry)
        archive.toXml(zipStream)
        zipStream.finish()
    }

    /**
     * Serializes all the components to the given archive and zipstream.
     *
     * @param serializer The serializer for the components.
     * @param archive    The archive contents to update.
     * @param zipStream  The zipstream to write to.
     * @throws IOException If there is an IO error.
     */
    @Throws(IOException::class)
    private fun serializeComponents(
        serializer: WorkspaceComponentSerializer,
        archive: ArchivedWorkspace,
        zipStream: ZipOutputStream,
        headless: Boolean
    ) {
        val components = sortComponentsByPriority()
        for (component in workspace.componentList) {
            serializeComponent(serializer, archive, component, zipStream, headless)
        }
    }

    private fun sortComponentsByPriority(): List<WorkspaceComponent> {
        val components: MutableList<WorkspaceComponent> = ArrayList()
        components.addAll(workspace.componentList)
        components.sortWith { c1, c2 -> c1.serializePriority.compareTo(c2.serializePriority) }
        return components
    }

    /**
     * Serialize one component to the zip stream
     *
     * @param serializer The serializer for the components.
     * @param archive    The archive contents to update.
     * @param component  the component to serialize
     * @param zipStream  The zipstream to write to.
     */
    private fun serializeComponent(
        serializer: WorkspaceComponentSerializer,
        archive: ArchivedWorkspace,
        component: WorkspaceComponent,
        zipStream: ZipOutputStream,
        headless: Boolean
    ) {
        val archiveComp = archive.addComponent(component)
        var entry: ZipEntry? = ZipEntry(archiveComp.getUri())
        try {
            zipStream.putNextEntry(entry)
            serializer.serializeComponent(component, zipStream)
            if (!headless) {
                val desktopComponent = getDesktopComponent(component)
                // Makes it possible to save a non-GUI simulation
                if (desktopComponent != null) {
                    val dc = archiveComp.addDesktopComponent(desktopComponent)
                    entry = ZipEntry(dc.uri)
                    zipStream.putNextEntry(entry)
                    desktopComponent.save(zipStream)
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    /**
     * Serialize couplings.
     *
     * @param archive the archive objet to serialize to
     */
    private fun serializeCouplings(archive: ArchivedWorkspace) {
        val couplingComponents = mapCouplingComponents()
        for (coupling in workspace.couplings) {
            serializeCoupling(couplingComponents, coupling, archive)
        }
    }

    /**
     * Returns a map from coupling base objects ("models") to their parent components.
     */
    private fun mapCouplingComponents(): HashMap<Any, WorkspaceComponent> {
        val couplingComponents = HashMap<Any, WorkspaceComponent>()
        for (component in workspace.componentList) {
            for (`object` in component.attributeContainers) {
                couplingComponents[`object`] = component
            }
        }
        return couplingComponents
    }

    /**
     * Add a serialized coupling to the archive.
     *
     * @param couplingComponents a map from couplings to components
     * @param coupling           the coupling to save
     * @param archive            the archive object to save to
     */
    private fun serializeCoupling(
        couplingComponents: HashMap<Any, WorkspaceComponent>,
        coupling: Coupling,
        archive: ArchivedWorkspace
    ) {
        val producer = ArchivedAttribute(couplingComponents[coupling.producer.baseObject], coupling.producer)
        val consumer = ArchivedAttribute(couplingComponents[coupling.consumer.baseObject], coupling.consumer)
        archive.addCoupling(ArchivedCoupling(producer, consumer))
    }

    /**
     * Serialize all update actions  in the current workspace
     *
     * @param archive the archive object to serialize to
     */
    private fun serializeUpdateActions(archive: ArchivedWorkspace) {
        for (action in workspace.updater.updateManager.actionList) {
            archive.addUpdateAction(action)
        }
    }

    /**
     * Creates a workspace from a zip compressed input stream.
     *
     * @param stream The stream to read from. This is expected to be zip compressed.
     * @throws IOException if an IO error occurs.
     */
    @Throws(IOException::class)
    fun deserialize(stream: InputStream) {
        val byteArrays = processInputStream(stream)
        val archive =
            ArchivedWorkspace.xstream().fromXML(ByteArrayInputStream(byteArrays["contents.xml"])) as ArchivedWorkspace

        val deserializer = WorkspaceComponentDeserializer()
        deserializeComponents(archive, deserializer, byteArrays)

        deserializeCouplings(archive)
        // deserializeUpdateActions(archive, deserializer);
        deserializeWorkspaceParameters(archive)
    }

    @Throws(IOException::class)
    private fun processInputStream(stream: InputStream): Map<String, ByteArray?> {
        // Populate the byte stream BUFFER_SIZE at a time and create a zip input
        // stream (currently 1 kb at a time).
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        while ((stream.read(buffer).also { read = it }) >= 0) {
            bytes.write(buffer, 0, read)
        }
        var zip = ZipInputStream(ByteArrayInputStream(bytes.toByteArray()))

        // Populate a map from zip entries (strings containing path+file info in
        // zip archive) to the associated data
        val byteArrays: MutableMap<String, ByteArray> = HashMap()
        var entry = zip.nextEntry
        // Initialize byte arrays
        var next: ZipEntry?
        while (entry != null) {
            next = zip.nextEntry
            byteArrays[entry.name] = ByteArray(entry.size.toInt())
            entry = next
        }
        zip = ZipInputStream(ByteArrayInputStream(bytes.toByteArray()))
        // Populate byte arrays
        while ((zip.nextEntry.also { entry = it }) != null) {
            val data = byteArrays[entry!!.name]!!
            read(zip, data)
        }

        // Find the contents.xml file and set the zip entries relative to that
        val zipEntries: Set<String> = HashSet(byteArrays.keys)
        val contentsFile = "contents.xml"
        var contentsPath = ""
        for (entryName in zipEntries) {
            if (entryName.endsWith(contentsFile)) {
                contentsPath = entryName.substring(0, entryName.length - contentsFile.length)
            }
        }

        // Remove the contents path from all entries that have it
        if (!contentsPath.isEmpty()) {
            for (entryName in zipEntries) {
                if (entryName.startsWith(contentsPath)) {
                    byteArrays[entryName.replace(contentsPath, "")] = byteArrays[entryName]!!
                    byteArrays.remove(entryName)
                }
            }
        }
        return byteArrays
    }

    private fun deserializeComponents(
        archive: ArchivedWorkspace,
        deserializer: WorkspaceComponentDeserializer,
        byteArrays: Map<String, ByteArray?>
    ) {
        if (archive.archivedComponents != null) {
            for (archivedComponent in archive.archivedComponents) {
                try {
                    val wc = deserializer.deserializeWorkspaceComponent(
                        archivedComponent,
                        ByteArrayInputStream(byteArrays[archivedComponent.getUri()])
                    )
                    wc.postOpenInit(workspace)
                    workspace.addWorkspaceComponent(wc)
                    if (archivedComponent.desktopComponent != null) {
                        val bounds =
                            getSimbrainXStream().fromXML(ByteArrayInputStream(byteArrays[archivedComponent.desktopComponent.uri])) as Rectangle
                        val desktopComponent = getDesktopComponent(wc)
                        desktopComponent.parentFrame.bounds = bounds
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    val message = String.format("Failed to deserialize component %s.", archivedComponent.name)
                    JOptionPane.showMessageDialog(null, message)
                }
            }
        }
    }

    private fun deserializeCouplings(archive: ArchivedWorkspace) {
        if (archive.archivedCouplings != null) {
            for (archivedCoupling in archive.archivedCouplings) {
                val producer = archivedCoupling.createProducer(workspace)
                val consumer = archivedCoupling.createConsumer(workspace)
                workspace.couplingManager.createCoupling(producer, consumer, false)
            }
        }
    }

    private fun deserializeUpdateActions(archive: ArchivedWorkspace, deserializer: WorkspaceComponentDeserializer) {
        val manager = workspace.updater.updateManager
        manager.clear()
        if (archive.archivedActions != null) {
            for (archivedAction in archive.archivedActions) {
                manager.addAction(archive.createUpdateAction(workspace, deserializer, archivedAction))
            }
        }
    }

    private fun deserializeWorkspaceParameters(archive: ArchivedWorkspace) {
        if (archive.workspaceParameters != null) {
            workspace.updateDelay = archive.workspaceParameters.updateDelay
            workspace.updater.time = archive.workspaceParameters.savedTime
            workspace.initIdManager()
        }
    }

    companion object {
        /**
         * Helper method that will read the InputStream repeatedly until the given array is filled.
         *
         * @param istream the InputStream to read from.
         * @param bytes   the array to write to
         * @throws IOException if there is an IO error
         */
        @Throws(IOException::class)
        fun read(istream: InputStream, bytes: ByteArray) {
            var pos = 0
            while (pos < bytes.size) {
                val read = istream.read(bytes, pos, bytes.size - pos)
                if (read < 0) {
                    throw RuntimeException("premature EOF")
                }
                pos += read
            }
        }

        fun <T> showOpenComponentDialog(type: Class<T>): WorkspaceComponent? {
            val defaultDirectory = baseDirectory
            val chooser = SFileChooser(defaultDirectory, "XML File", "xml")
            val file = chooser.showOpenDialog()
            return if (file != null) {
                open(type, file)
            } else {
                null
            }
        }

        /**
         * Helper method for openings workspace components from a file.
         *
         *
         * A call might look like this `NetworkComponent networkComponent = (NetworkComponent)
         * WorkspaceFileOpener(NetworkComponent.class, new File("Net.xml"));`
         *
         * @param fileClass the type of Workpsace component to open; a subclass of WorkspaceComponent.
         * @param file      the File to open
         * @return the workspace component
         */
        fun open(fileClass: Class<*>, file: File): WorkspaceComponent {
            val extension = file.name.substring(file.name.indexOf("."))
            try {
                val method = fileClass.getMethod(
                    "open",
                    InputStream::class.java,
                    String::class.java,
                    String::class.java
                )
                val wc = method.invoke(
                    null, FileInputStream(file),
                    file.name, extension
                ) as WorkspaceComponent
                wc.currentFile = file
                wc.setChangedSinceLastSave(false)
                return wc
            } catch (e: java.lang.Exception) {
                throw java.lang.RuntimeException(e)
            }
        }
    }


}

const val BUFFER_SIZE = 1024