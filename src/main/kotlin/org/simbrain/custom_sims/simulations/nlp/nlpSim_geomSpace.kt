package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.util.div
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.HaloColoringManager
import org.simbrain.util.projection.PCAProjection
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder

/**
 * Initial study of word embeddings.
 *
 * Loads a text that was crafted so that certain words would be nearby each other, because of shared co-occurrences.
 *
 * Examples:
 *  bus ~ butterfly
 *  walked ~ along
 *  ...
 *
 */
val nlpSimBasic = newSim {

    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = readSimulationFileContents("texts" / "corpus_artificial_similarity.txt")
    textWorld.text = text
    textWorld.tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.COC
    }.build(text)

    withGui {
        place(twc) {
            location = point(0, 0)
            width = 400
            height = 500
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    projectionPlot.projector.tolerance = .2
    projectionPlot.projector.projectionMethod = PCAProjection()
    projectionPlot.projector.coloringManager = HaloColoringManager().apply{
        radius = 10.0
    }
    withGui {
        place(projectionPlot) {
            location = point(450, 0)
            width = 500
            height = 500
        }
    }

    workspace.addUpdateAction(updateAction("Couplings") {
        val point = DataPoint(textWorld.currentVector).apply {
            label = textWorld.currentToken
        }
        projectionPlot.addPoint(point)
    })

}