package org.simbrain.world.imageworld

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.awt.image.BufferedImage
import java.io.IOException
import org.simbrain.util.showWarningDialog

class ImageClipboard(private val world: ImageWorld) : ClipboardOwner {
    private inner class TransferableImage(private val image: Image) : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return image
            } else {
                throw UnsupportedFlavorException(flavor)
            }
        }

        override fun getTransferDataFlavors(): Array<DataFlavor?> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return flavor.equals(DataFlavor.imageFlavor)
        }
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
    }

    fun copyImage() {
        val image = world.imageAlbum.currentImage
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(TransferableImage(image), this)
    }

    suspend fun pasteImage() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = clipboard.getContents(null)
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                val image = contents.getTransferData(DataFlavor.imageFlavor) as Image
                val bufferedImage =
                    BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB)
                val graphics = bufferedImage.graphics
                graphics.drawImage(image, 0, 0, null)
                graphics.dispose()
                world.imageAlbum.addImage(bufferedImage)
            } catch (ex: UnsupportedFlavorException) {
                showWarningDialog("Unable to read image from clipboard: ${ex.message}")
            } catch (ex: IOException) {
                showWarningDialog("Unable to read image from clipboard: ${ex.message}")
            }
        }
    }

}