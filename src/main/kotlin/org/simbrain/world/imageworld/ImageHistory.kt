/**
 * Undo history for an [ImageAlbum], owned by the [ImageWorldPanel] showing it. Only edits a user makes are recorded: a
 * coupling or simulation writing to the album is a stream of changes with nothing sensible to step back to, so such a
 * write ends the history instead (see [ImageSource.revision]).
 *
 * There are two kinds of edit. A [PixelEdit] stores the rectangle of pixels an edit changed, before and after. An
 * [AlbumEdit] stores which images the album held, by reference. Because the stack unwinds in order, an image a
 * [PixelEdit] refers to is always back in the album by the time that edit is undone.
 */
package org.simbrain.world.imageworld

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.simbrain.util.UndoManager
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.util.Arrays
import java.util.Collections
import java.util.IdentityHashMap

/**
 * @param maxBytes memory to spend on history, after which the oldest edits are forgotten. A single edit can be most of
 * a large photo twice over, so this is a size rather than a number of edits.
 */
class ImageHistory(
    private val album: ImageAlbum,
    maxBytes: Long = Runtime.getRuntime().maxMemory() / 4
) {

    private val undoManager = UndoManager(maxBytes)

    /**
     * Undo and redo suspend while events fire, and loading images suspends for much longer, so a second edit could
     * otherwise start partway through the first and leave the stacks out of order.
     */
    private val lock = Mutex()

    private var syncedRevision = album.revision

    val canUndo get() = undoManager.undoStack.isNotEmpty()

    val canRedo get() = undoManager.redoStack.isNotEmpty()

    /**
     * Called when [canUndo] or [canRedo] may have changed, on any thread.
     */
    var onChanged: () -> Unit by undoManager::onChanged

    /**
     * Start an edit that happens over time, such as a brush stroke. Copies the current image, so it should be called
     * right before the first pixel changes. Pass the result to [record] when the edit is over.
     */
    fun beginPixelEdit(description: String) = PendingPixelEdit(description, album.currentImage)

    /**
     * An edit in progress: the target image and its pixels from before the edit began.
     */
    inner class PendingPixelEdit internal constructor(
        private val description: String,
        val image: BufferedImage
    ) {
        private val before: Raster = image.data

        /**
         * The finished edit, or null if no pixels changed. Compares the whole image, so call it once, when done.
         */
        fun finish(): UndoManager.UndoableAction? {
            val changed = changedBounds(before, image.raster) ?: return null
            return PixelEdit(description, image, before.copyOf(changed), image.getData(changed))
        }
    }

    /**
     * Add a finished [PendingPixelEdit] to the history.
     */
    suspend fun record(edit: UndoManager.UndoableAction) = lock.withLock {
        discardStale()
        undoManager.addUndoableAction(edit)
    }

    /**
     * Change the pixels of the current image in place as one undoable step, and show the result.
     */
    suspend fun pixelEdit(description: String, edit: (BufferedImage) -> Unit) = lock.withLock {
        discardStale()
        val pending = beginPixelEdit(description)
        edit(pending.image)
        pending.finish()?.let { undoManager.addUndoableAction(it) }
        album.fireImageUpdate()
    }

    /**
     * Run [edit], which adds, removes or replaces frames, as one undoable step. Nothing is recorded if the album holds
     * the same images afterwards.
     */
    suspend fun albumEdit(description: String, edit: suspend () -> Unit) = lock.withLock {
        discardStale()
        val before = album.captureState()
        edit()
        val after = album.captureState()
        syncedRevision = album.revision
        if (!after.isSameAs(before)) {
            undoManager.addUndoableAction(AlbumEdit(description, before, after))
        }
    }

    suspend fun undo() = lock.withLock {
        discardStale()
        undoManager.undo()
    }

    suspend fun redo() = lock.withLock {
        discardStale()
        undoManager.redo()
    }

    /**
     * Forget the history if a coupling or simulation has changed the album since the last recorded edit. Lets an edit in
     * progress finish rather than mistaking its changes for someone else's.
     */
    fun discardIfStale() {
        if (lock.tryLock()) {
            try {
                discardStale()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun discardStale() {
        if (album.revision != syncedRevision) {
            syncedRevision = album.revision
            undoManager.clear()
        }
    }

    private inner class PixelEdit(
        override val description: String,
        private val image: BufferedImage,
        private val before: Raster,
        private val after: Raster
    ) : UndoManager.UndoableAction {

        override val sizeBytes = before.sizeBytes() + after.sizeBytes()

        override suspend fun undo() = write(before)

        override suspend fun redo() = write(after)

        private suspend fun write(pixels: Raster) {
            album.showFrameOf(image)
            image.data = pixels
            album.fireImageUpdate()
        }
    }

    private inner class AlbumEdit(
        override val description: String,
        private val before: ImageAlbum.State,
        private val after: ImageAlbum.State
    ) : UndoManager.UndoableAction {

        /**
         * Frames in both states are in the album anyway. The ones in only one are kept alive by this edit alone.
         */
        override val sizeBytes = run {
            val imagesBefore = before.images()
            val imagesAfter = after.images()
            (imagesBefore.filter { it !in imagesAfter } + imagesAfter.filter { it !in imagesBefore })
                .sumOf { it.raster.sizeBytes() }
        }

        override suspend fun undo() = album.restoreState(before)

        override suspend fun redo() = album.restoreState(after)
    }
}

private fun ImageAlbum.State.images(): Set<BufferedImage> =
    Collections.newSetFromMap(IdentityHashMap<BufferedImage, Boolean>()).also {
        it.addAll(frames)
        it.add(currentImage)
    }

private fun Raster.sizeBytes(): Long =
    width.toLong() * height * numDataElements * DataBuffer.getDataTypeSize(transferType) / 8

/**
 * A copy of one region. A child raster would be cheaper, but it would keep this entire raster in memory.
 */
private fun Raster.copyOf(region: Rectangle): Raster = createCompatibleWritableRaster(region).also {
    it.setRect(createChild(region.x, region.y, region.width, region.height, region.x, region.y, null))
}

/**
 * The smallest rectangle containing every pixel that differs between two rasters of the same size, or null if none do.
 */
private fun changedBounds(a: Raster, b: Raster): Rectangle? {
    val bands = a.numBands
    val rowA = IntArray(a.width * bands)
    val rowB = IntArray(a.width * bands)
    var bounds: Rectangle? = null
    for (y in 0 until a.height) {
        a.getPixels(0, y, a.width, 1, rowA)
        b.getPixels(0, y, a.width, 1, rowB)
        val firstSample = Arrays.mismatch(rowA, rowB)
        if (firstSample < 0) continue
        val lastSample = rowA.indices.last { rowA[it] != rowB[it] }
        val row = Rectangle(firstSample / bands, y, (lastSample / bands) - (firstSample / bands) + 1, 1)
        bounds = bounds?.union(row) ?: row
    }
    return bounds
}
