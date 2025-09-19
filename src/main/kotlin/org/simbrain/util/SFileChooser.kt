package org.simbrain.util

import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.filechooser.FileFilter

/**
 * **SFileChooser** extends java's JFileChooser, providing for automatic
 * adding of file extensions, memory of file-locations, and checks to prevent
 * file-overwrites.
 *
 *
 * 2008-10-09 Matt Watson - modified to support new serialization approach more
 * easily.
 */
class SFileChooser(currentDirectory: String, description: String? = null, extension: String? = null, val checkEmptyFile: Boolean = true) {
    /**
     * The map of extensions and their descriptions in the order of their
     * addition. NOTE: uneven results with this.  TODO: Test and review.
     */
    private val exts = LinkedHashMap<String, String>()

    /**
     * The description of the file formats that are acceptable (shown in the
     * "File Format field" of the file chooser). For example,
     * "image files (.jpg, .gif, .png)".
     */
    private var description: String?

    /**
     * A memory of the last directory this FileChooser was in.
     *
     * @return the directory this chooser is in.
     */
    var currentLocation: String?
        private set

    init {
        this.currentLocation = currentDirectory
        this.description = description

        if (extension != null) {
            if (description == null) {
                addExtension(extension)
            } else {
                addExtension(description, extension)
            }
        }
    }

    /**
     * Use the image viewer to preview image files.
     */
    private var useViewer = false

    /**
     * Adds an extension with the provided description to the filenamefilter.
     *
     * @param extension   the extension
     * @param description the description
     */
    fun addExtension(description: String, extension: String) {
        exts.put(extension, description)
    }

    /**
     * Adds an extension with the default description to the filenamefilter.
     *
     * @param extension the extension to add
     */
    fun addExtension(extension: String) {
        addExtension("*.$extension", extension)
    }

    /**
     * Adds the filters for the extensions to the provided chooser.
     *
     * @param chooser the file chooser to add filters to
     * @return filter map
     */
    private fun addExtensions(chooser: JFileChooser): MutableMap<String?, ExtensionFileFilter?> {
        val filters: MutableMap<String?, ExtensionFileFilter?> = HashMap<String?, ExtensionFileFilter?>()

        for (entry in exts.entries) {
            val filter = ExtensionFileFilter(entry.key, entry.value)
            filters.put(entry.key, filter)
            chooser.addChoosableFileFilter(filter)
        }
        return filters
    }

    /**
     * Set the description used by this file chooser.
     */
    fun setDescription(value: String) {
        description = value
    }

    /**
     * Shows dialog for opening files.
     *
     * @return File if selected
     */
    fun showOpenDialog(): File? {
        return if (useNativeFileChooser) {
            showOpenDialogNative()
        } else {
            showOpenDialogSwing()
        }
    }

    /**
     * Native open dialog.
     *
     * @return file
     */
    private fun showOpenDialogNative(): File? {
        val chooser = FileDialog(JFrame(), "Open", FileDialog.LOAD)
        chooser.setDirectory(this.currentLocation)

        setFileChooserFilter(chooser)
        chooser.isVisible = true

        if (chooser.getFile() != null) {
            this.currentLocation = chooser.directory
            return File(chooser.directory + FS + chooser.getFile()).also { checkEmptyFileWarningDialog(it) }
        } else {
            // User canceled
            return null
        }
    }

    /**
     * Allows you to select multiple files.
     *
     * @return an array of selected files
     */
    fun showMultiOpenDialogNative(): Array<File?>? {
        val chooser = FileDialog(JFrame(), "Open", FileDialog.LOAD)
        chooser.isMultipleMode = true
        chooser.setDirectory(this.currentLocation)

        setFileChooserFilter(chooser)
        chooser.isVisible = true

        if (chooser.getFile() != null) {
            this.currentLocation = chooser.directory
            return chooser.files
        } else {
            // User canceled
            return null
        }
    }

    private fun setFileChooserFilter(chooser: FileDialog) {
        if (exts.isNotEmpty()) {
            if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")) {
                var file = ""
                for (ext in exts.keys) {
                    file = "$file*$ext;"
                }
                chooser.setFile(file)
            } else {
                chooser.setFilenameFilter(ExtensionSetFileFilter(exts.keys, description))
            }
        }
    }

    private fun showOpenDialogSwing(): File? {
        val chooser = JFileChooser(currentLocation)
        if (exts.size > 1) {
            chooser.addChoosableFileFilter(ExtensionSetFileFilter(exts.keys, description))
        }

        if (useViewer) {
            val preview = ImagePreviewPanel()
            chooser.setAccessory(preview)
            chooser.addPropertyChangeListener(preview)
        }

        addExtensions(chooser)

        if (chooser.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
            this.currentLocation = chooser.currentDirectory.path
            return chooser.selectedFile.also { checkEmptyFileWarningDialog(it) }
        } else {
            return null
        }
    }

    /**
     * Shows dialog for saving files.
     *
     * @param file initial file name
     * @return Name of file saved
     */
    fun showSaveDialog(file: File?): File? {
        if (useNativeFileChooser) {
            return showSaveDialogNative(file)
        } else {
            return showSaveDialogSwing(file)
        }
    }

    /**
     * Native save dialog.
     *
     * @return the saved file, or null if cancel, etc.
     */
    private fun showSaveDialogNative(file: File?): File? {
        val chooser = FileDialog(JFrame(), "Save", FileDialog.SAVE)
        chooser.setDirectory(currentLocation ?: Utils.USER_HOME)
        if (file != null) {
            if (exts.isNotEmpty()) {
                chooser.setFilenameFilter(ExtensionSetFileFilter(exts.keys, description))
                chooser.setFile(addExtension(file, ExtensionSetFileFilter(exts.keys, description)).getName())
            } else {
                chooser.setFile(file.getName())
            }
        }
        chooser.isVisible = true

        // Don't use confirmOverwrite because most native file choosers do this.
        if (chooser.getFile() == null) {
            return null
        } else {
            this.currentLocation = chooser.directory
            return File(chooser.directory + FS + chooser.getFile())
        }
    }

    /**
     * Swing save dialog.
     *
     * @return Name of file saved
     */
    private fun showSaveDialogSwing(file: File?): File? {
        val chooser = JFileChooser(currentLocation)
        chooser.setAcceptAllFileFilterUsed(false)
        val filters = addExtensions(chooser)

        if (file != null) {
            chooser.setSelectedFile(file)
            val extension: String? = getExtension(file)

            // System.out.println("extension: " + extension);
            if (extension != null) {
                chooser.setFileFilter(filters.get(extension))
            }
        }

        // TODO real parent?
        val result = chooser.showDialog(chooser, "Save")
        if (result != JFileChooser.APPROVE_OPTION) {
            return null
        }

        val tmpFile = addExtension(chooser.selectedFile, chooser.fileFilter)
        if (tmpFile.exists() && !confirmOverwrite(tmpFile)) {
            return null
        }
        this.currentLocation = chooser.currentDirectory.path
        return tmpFile
    }

    /**
     * Ask user whether to overwrite the give existing file.
     *
     * @param file the file in question
     * @return whether the user selected "yes"
     */
    fun confirmOverwrite(file: File): Boolean {
        val message = "The file \"" + file.getName() + "\" already exists. Overwrite?"

        val options = arrayOf<Any?>("OK", "Cancel")

        return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
            null,
            message,
            "Warning",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        )
    }

    /**
     * Shows the save dialog for the given string name.
     *
     * @param fileName the name of the file
     * @return the file name to save to
     */
    fun showSaveDialog(fileName: String): File? {
        return showSaveDialog(File(fileName))
    }

    /**
     * Shows the save dialog.
     *
     * @return the selected file
     */
    fun showSaveDialog(): File? {
        return showSaveDialog(null as File?)
    }

    fun checkEmptyFileWarningDialog(file: File?) {
        if (file != null && checkEmptyFile) {
            if (file.exists() && file.length() == 0L) {
                showWarningDialog("""
                    Warning: The file "${file.name}" is empty.
                """.trimIndent())
            }
        }
    }

    /**
     * Construct a file filter.
     *
     * @param extension   Extension to filter.
     * @param description Human readable description of extension.
     */
    private inner class ExtensionFileFilter(
        val extension: String,
        private val _description: String?
    ) : FileFilter(), FilenameFilter {

        /**
         * Determines if the file has the correct extension type.
         *
         * @param file File to be checked
         * @return whether the file has the correct extension type
         */
        override fun accept(file: File): Boolean {
            return file.isDirectory() || hasExtension(file, extension)
        }

        /**
         * @return description of the extension.
         */
        override fun getDescription(): String {
            return _description ?: "*.$extension"
        }

        /**
         * Implements file name filter for native file dialog.
         *
         * @param dir
         * @param name
         * @return
         */
        override fun accept(dir: File?, name: String): Boolean {
            return extension.equals(getExtension(name), ignoreCase = true)
        }
    }

    /**
     * Construct the file set filter.
     *
     * @param extensions  A collection of extension names.
     * @param description A human readable description for the set of extensions.
     */
    private inner class ExtensionSetFileFilter(
        private val extensions: MutableCollection<String>,
        private val _description: String? = null
    ) : FileFilter(), FilenameFilter {

        /**
         * Determines if the file has the correct extension type.
         *
         * @param file File to be checked
         * @return whether the file has the correct extension type
         */
        override fun accept(file: File): Boolean {
            return (file.isDirectory()) || extensions.contains(getExtension(file))
        }

        /**
         * @return description of the extension.
         */
        override fun getDescription(): String {
            if (_description != null) {
                return _description
            }

            val builder = StringBuilder()

            val i = extensions.iterator()
            while (i.hasNext()) {
                builder.append("*.")
                builder.append(i.next())
                if (i.hasNext()) builder.append(", ")
            }

            return builder.toString()
        }

        /**
         * Implements file name filter for native file dialog.
         *
         * @param dir
         * @param name
         * @return
         */
        override fun accept(dir: File?, name: String): Boolean {
            return extensions.contains(getExtension(name))
        }
    }

    /**
     * Returns whether the given file has the given extension
     *
     * @param theFile   the file to check
     * @param extension the extension to look for
     * @return whether the given file has the given extension
     */
    private fun hasExtension(theFile: File, extension: String): Boolean {
        return extension == getExtension(theFile)
    }

    /**
     * Check to see if the file has the extension, and if not, add it.
     *
     * @param theFile File to add extension to
     * @param filter  Extension to add to file
     * @return The file name with the correct extension
     */
    private fun addExtension(theFile: File, filter: FileFilter?): File {
        if (exts.size < 1) {
            return theFile
        }

        val extension: String

        if (filter is ExtensionFileFilter) {
            extension = filter.extension
        } else {
            extension = exts.keys.iterator().next()
        }

        if (hasExtension(theFile, extension)) {
            return theFile
        } else {
            // TODO JMW - this seems strange.
            val output = File(theFile.absolutePath + "." + extension)

            if (theFile.exists()) {
                theFile.renameTo(output)

                return theFile
            } else {
                return output
            }
        }
    }

    /**
     * Sets the file chooser to use image preview viewer.
     *
     * @param useViewer use image preview viewer.
     */
    fun setUseImagePreview(useViewer: Boolean) {
        this.useViewer = useViewer
    }

    companion object {
        // TODO: Consider removing or renaming the description field.
        /**
         * Whether to use the native file chooser, or the Swing file chooser.
         */
        private const val useNativeFileChooser = true

        /**
         * File separator.
         */
        private val FS: String? = System.getProperty("file.separator")

        /**
         * Returns all the characters after the last period in the file name.
         *
         * @param theFile the file
         * @return all the characters after the last period in the file name
         */
        fun getExtension(theFile: File): String? {
            return getExtension(theFile.getName())
        }

        /**
         * Returns all the characters after the last period in the file name.
         *
         * @param fileName the file's name
         * @return all the characters after the last period in the file name
         */
        fun getExtension(fileName: String): String? {
            val position = fileName.lastIndexOf('.')
            if (position > 0 && position < fileName.length) {
                return fileName.substring(position + 1)
            } else {
                return null
            }
        }
    }
}