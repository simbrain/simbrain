/**
 * Undo and redo of edits to an [ImageAlbum], without a GUI. See [ImageWorldUndoGuiTest] for the same edits made through
 * the mouse and toolbar.
 */
package org.simbrain.world.imageworld

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.fill
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.util.concurrent.atomic.AtomicBoolean

class ImageHistoryTest {

    private fun BufferedImage.pixels(): IntArray = getRGB(0, 0, width, height, null, 0, width)

    private suspend fun albumOfSize(size: Int) = ImageAlbum().also { it.reset(size, size) }

    @Test
    fun `undo and redo restore a pixel edit`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        val original = album.currentImage.pixels()

        history.pixelEdit("Draw") { it.setRGB(3, 4, Color.RED.rgb) }
        val edited = album.currentImage.pixels()
        assertFalse(edited.contentEquals(original))

        history.undo()
        assertArrayEquals(original, album.currentImage.pixels())
        assertTrue(history.canRedo)

        history.redo()
        assertArrayEquals(edited, album.currentImage.pixels())
    }

    @Test
    fun `undo works through a stack of edits in reverse`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("First") { it.fill(Color.RED) }
        history.pixelEdit("Second") { it.fill(Color.GREEN) }
        history.pixelEdit("Third") { it.fill(Color.BLUE) }

        history.undo()
        assertEquals(Color.GREEN.rgb, album.currentImage.getRGB(0, 0))
        history.undo()
        assertEquals(Color.RED.rgb, album.currentImage.getRGB(0, 0))
    }

    @Test
    fun `an edit that changes nothing is not recorded`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("Clear") { it.fill(Color.BLACK) }
        assertFalse(history.canUndo)
    }

    @Test
    fun `a new edit forgets what could be redone`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("First") { it.fill(Color.RED) }
        history.undo()
        history.pixelEdit("Second") { it.fill(Color.GREEN) }
        assertFalse(history.canRedo)
    }

    @Test
    fun `a pending edit keeps only the pixels that changed`() = runBlocking {
        val album = albumOfSize(100)
        // Room for a few pixels, but far from the 40 KB of a copy of this image
        val history = ImageHistory(album, maxBytes = 1000)

        val pending = history.beginPixelEdit("Draw")
        pending.image.setRGB(50, 50, Color.RED.rgb)
        pending.image.setRGB(51, 52, Color.RED.rgb)
        history.record(pending.finish()!!)

        assertTrue(history.canUndo)
        history.undo()
        assertEquals(Color.BLACK.rgb, album.currentImage.getRGB(50, 50))
        assertEquals(Color.BLACK.rgb, album.currentImage.getRGB(51, 52))
    }

    @Test
    fun `the corners of an image are restored`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        val original = album.currentImage.pixels()
        history.pixelEdit("Corners") {
            it.setRGB(0, 0, Color.RED.rgb)
            it.setRGB(9, 9, Color.RED.rgb)
        }
        history.undo()
        assertArrayEquals(original, album.currentImage.pixels())
    }

    @Test
    fun `an edit too large for the history is not recorded`() = runBlocking {
        val album = albumOfSize(100)
        val history = ImageHistory(album, maxBytes = 1000)
        history.pixelEdit("Fill") { it.fill(Color.RED) }
        assertFalse(history.canUndo)
    }

    @Test
    fun `an indexed image gets its exact pixels back`() = runBlocking {
        val palette = byteArrayOf(0, 40, 80, 120)
        val colors = IndexColorModel(2, 4, palette, palette.reversedArray(), palette)
        val image = BufferedImage(4, 4, BufferedImage.TYPE_BYTE_INDEXED, colors)
        image.raster.setSample(1, 1, 0, 2)
        val album = ImageAlbum()
        album.addImage(image)
        val history = ImageHistory(album)
        val original = IntArray(16).also { image.raster.getSamples(0, 0, 4, 4, 0, it) }

        history.pixelEdit("Draw") { it.raster.setSample(1, 1, 0, 3) }
        history.undo()

        assertArrayEquals(original, IntArray(16).also { image.raster.getSamples(0, 0, 4, 4, 0, it) })
    }

    @Test
    fun `undoing an edit on another frame moves to that frame`() = runBlocking {
        val album = albumOfSize(10)
        album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        album.setFrame(0)
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }

        album.setFrame(1)
        history.undo()

        assertEquals(0, album.frameIndex)
        assertEquals(Color.BLACK.rgb, album.currentImage.getRGB(0, 0))
    }

    @Test
    fun `undoing a delete brings back the same image`() = runBlocking {
        val album = albumOfSize(10)
        album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        val history = ImageHistory(album)
        val deleted = album.currentImage

        history.albumEdit("Delete image") { album.deleteCurrentImage() }
        assertEquals(1, album.numFrames)

        history.undo()
        assertEquals(2, album.numFrames)
        assertEquals(1, album.frameIndex)
        assertSame(deleted, album.currentImage)

        history.redo()
        assertEquals(1, album.numFrames)
    }

    @Test
    fun `a pixel edit made before a delete can still be undone after it`() = runBlocking {
        val album = albumOfSize(10)
        album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }
        history.albumEdit("Delete image") { album.deleteCurrentImage() }

        history.undo()
        history.undo()

        assertEquals(2, album.numFrames)
        assertEquals(Color.BLACK.rgb, album.currentImage.getRGB(0, 0))
    }

    @Test
    fun `undoing a reset restores the frames and their size`() = runBlocking {
        val album = albumOfSize(10)
        album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        val history = ImageHistory(album)
        val frames = album.frames.toList()

        history.albumEdit("Reset canvas") { album.reset(5, 5) }
        assertEquals(5, album.width)

        history.undo()
        assertEquals(10, album.width)
        assertEquals(2, album.numFrames)
        assertSame(frames[1], album.currentImage)
    }

    @Test
    fun `a coupling can write to an album whose reset was undone`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.albumEdit("Reset canvas") { album.reset(5, 5) }
        history.undo()
        album.setBrightness(DoubleArray(100) { 0.5 })
        assertEquals(10, album.width)
    }

    @Test
    fun `an album edit that changes nothing is not recorded`() = runBlocking {
        val album = ImageAlbum()
        val history = ImageHistory(album)
        history.albumEdit("Delete image") { album.deleteCurrentImage() }
        assertFalse(history.canUndo)
    }

    @Test
    fun `a coupling write ends the history`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }

        album.setBrightness(DoubleArray(100) { 0.5 })
        history.discardIfStale()

        assertFalse(history.canUndo)
    }

    @Test
    fun `undo after a coupling write leaves the pixels alone`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }
        album.setBrightness(DoubleArray(100) { 0.5 })
        val written = album.currentImage.pixels()

        history.undo()

        assertArrayEquals(written, album.currentImage.pixels())
    }

    @Test
    fun `a frame added by a simulation ends the history`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.albumEdit("Reset canvas") { album.reset(5, 5) }

        album.addImage(BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB))
        history.undo()

        assertEquals(2, album.numFrames)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `moving between frames does not end the history`() = runBlocking {
        val album = albumOfSize(10)
        album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }

        album.setFrame(0)
        history.discardIfStale()

        assertTrue(history.canUndo)
    }

    @Test
    fun `an album edit under way is not mistaken for someone else's write`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("Draw") { it.fill(Color.RED) }

        history.albumEdit("Take snapshot") {
            album.takeSnapshot()
            // The event an album edit fires can reach the view's toolbar before the edit is done
            history.discardIfStale()
        }

        history.undo()
        history.undo()
        assertEquals(1, album.numFrames)
        assertEquals(Color.BLACK.rgb, album.currentImage.getRGB(0, 0))
    }

    @Test
    fun `an undo waits for the one before it to finish`() = runBlocking {
        val album = albumOfSize(10)
        val history = ImageHistory(album)
        history.pixelEdit("First") { it.fill(Color.RED) }
        history.pixelEdit("Second") { it.fill(Color.GREEN) }
        history.pixelEdit("Third") { it.fill(Color.BLUE) }

        // Holds up the first undo partway through, leaving the second free to overtake it if nothing stops it
        val firstUpdate = AtomicBoolean(true)
        album.events.imageUpdate.on(Dispatchers.Default) {
            if (firstUpdate.getAndSet(false)) delay(200)
        }

        listOf(
            async(start = CoroutineStart.UNDISPATCHED) { history.undo() },
            async(start = CoroutineStart.UNDISPATCHED) { history.undo() }
        ).awaitAll()

        assertEquals(Color.RED.rgb, album.currentImage.getRGB(0, 0))
        history.redo()
        assertEquals(Color.GREEN.rgb, album.currentImage.getRGB(0, 0))
        history.redo()
        assertEquals(Color.BLUE.rgb, album.currentImage.getRGB(0, 0))
    }
}
