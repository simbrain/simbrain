package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.util.Utils.FS
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.world.textworld.TextWorld
import java.io.File
import javax.swing.JInternalFrame

/**
 * Basics of word embeddings and co-occurrence matrices
 */
val nlpSim_basic = newSim {

    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = File("simulations" + FS + "texts" + FS + "river_streams.txt").readText()
    // Example: check "river" and "ocean", "river" and "stream", "lake" and "ocean", n.b. regenerate embeddings after settings change
    textWorld.extractEmbedding(text)
    textWorld.text = text

    withGui {
        place(twc) {
            location = point(10, 10)
            width = 400
            height = 500
        }

        val internalFrame = JInternalFrame("Co-occurrence matrix", true, true)
        internalFrame.setLocation(400, 10)
        addInternalFrame(internalFrame)
        val tableViewer = SimbrainDataViewer(textWorld.tokenEmbedding.createTableModel(TextWorld.EmbeddingType.COC))
        internalFrame.contentPane = tableViewer
        internalFrame.isVisible = true
        internalFrame.pack()
        textWorld.events.tokenVectorMapChanged.on {
            tableViewer.model = textWorld.tokenEmbedding.createTableModel(TextWorld.EmbeddingType.COC)
        }
    }


   val projectionPlot = addProjectionPlot2("Activations")
   withGui {
       place(projectionPlot) {
           location = point(970, 10)
           width = 500
           height = 500
       }
   }
    with(couplingManager) {
        createCoupling(
            textWorld.getProducer("getCurrentVector"),
            projectionPlot.getConsumer("addPoint")
        )
        createCoupling(
            textWorld.getProducer("getCurrentToken"),
            projectionPlot.getConsumer("setLabel")
        )
    }

}