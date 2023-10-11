package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.util.Utils.FS
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.PCAProjection
import java.io.File

/**
 * Study the geometric structure of a word embedding on a document.
 *
 * Load a text that was crafted so that certain words would be nearby each other, because of shared co-occurrences.
 *
 * Examples:
 *  bus ~ butterfly
 *  walked ~ along
 *  ...
 *
 */
val nlpSim_geomSpace = newSim {


    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = File("simulations" + FS + "texts" + FS + "corpus_artificial_similarity.txt").readText()
    textWorld.loadDictionary(text)
    textWorld.text = text

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
    withGui {
        place(projectionPlot) {
            location = point(450, 0)
            width = 500
            height = 500
        }
    }

    // Couple the text world to neuron collection
    with(couplingManager) {
        // createCoupling(
        //     textWorld.getProducer("getCurrentVector"),
        //     nc.getConsumer("addInputs")
        // )
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