package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.util.div
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.HaloColoringManager
import org.simbrain.util.projection.PCAProjection
import org.simbrain.util.updateAction
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder

/**
 * Initial study of word embeddings.
 *
 * Loads a text that that was crafted so that certain words would be nearby each other, because of shared co-occurrences.
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
    textWorld.autoAdvance = true
    val text = readSimulationFileContents("texts" / "corpus_artificial_similarity.txt")
    textWorld.text = text
    textWorld.tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.CoOccurrence()
    }.build(text)

    withGui {
        place(twc) {
            location = point(0, 0)
            width = 400
            height = 500
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
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

    addSidebarInfo(
        """
            
       # Basic Word Embeddings
    
       This simulation demonstrates a simple approach to word embeddings using a co-occurrence method. 
       Words that occur in similar contexts develop similar vector representations, allowing us to visualize 
       semantic similarity. The text used here was crafted so that certain words would appear in nearly identical 
       contexts, for example bus and butterfly or walked and along.
    
       ## Background
    
       Word embeddings are numerical representations of word meaning built from patterns of co-occurrence in text. 
       The basic idea is that words used in similar contexts tend to have similar meanings. This simulation uses a 
       simple co-occurrence matrix, where each row represents a target word and each column represents a context word.
       The values indicate how often a word appears near another within a moving window of text. These values are 
       normalized using Positive Pointwise Mutual Information (PPMI), which highlights informative co-occurrences and 
       downplays those that happen frequently by chance.
    
       To visualize embeddings, Principal Component Analysis (PCA) is used to reduce the high-dimensional co-occurrence 
       space to two dimensions. PCA preserves relative distances between words so that those with similar distributions 
       appear closer together. The projection plot makes it easy to see clusters such as `bus` and `butterfly`, 
       which share almost identical contexts in the training text.
    
       This approach is related to usage-based theories of language, which see meaning as emerging from statistical 
       regularities in linguistic experience. While our corpus is small and artificial, it demonstrates the same 
       principles that underlie larger-scale embeddings such as Word2Vec or GloVe.
    
       # Simulation Details
    
       The simulation consists of two main components:
    
       Text World: Displays a corpus of text and automatically iterates through tokens. The text is used to generate a 
       co-occurrence matrix and the resulting word embeddings.  
    
       Projection Plot (labeled `Activations`): Shows the embeddings in a two-dimensional scatterplot using PCA. 
       Words with  similar co-occurrence patterns appear near one another.
    
       You can also view the co-occurrence matrix directly by clicking the matrix button. Rows represent target words and 
       columns represent context words. The diagonal is usually near zero because words rarely co-occur with themselves. 
       Selecting rows and clicking the view embedding button produces a similarity heatmap based on cosine similarity, 
       where higher values indicate greater similarity.
    
       # What to Do
    
       1. Run the simulation.  
       2. Observe the interface: the text world on the left and the PCA projection on the right.  
       3. Step through the text using the play button. Each word’s embedding will be added to the projection plot.  
       4. Use the matrix and heatmap tools to examine co-occurrence structure and similarity values. Compare, for example, `bus` and `butterfly`.  
       5. Try loading a different corpus from the simulations/texts folder. Notice how changing the training text alters the positions of words in the projection plot.
    
       # Credits
    
       [Jeff Yoshimi](https://jeffyoshimi.net/index.html)  

       Ellis Cain
        
        """.trimIndent()
    )

}