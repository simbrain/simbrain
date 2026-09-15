package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * Student-built IAC network of movies and their genres and moods, ported from the Simbrain 3 workspace Movies_2015.
 */
val iacMovies = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Movies")
    networkComponent.network.iacNetwork {
        val genres = pool(
            "Genres",
            "Horror", "Documentary", "Comedy", "Romantic", "Family", "Sci-Fi", "Action",
            at = point(-460, 0), columns = 1
        )
        val moods = pool(
            "Moods",
            "Exciting", "Boring", "Sentimental", "Funny", "Scary",
            at = point(460, 0), columns = 1
        )
        instances("Movies", at = point(0, 0), columns = 4, hSpacing = 180.0, vSpacing = 85.0) {
            instance("The Notebook", genres["Romantic"], moods["Sentimental"])
            instance("Star Wars", genres["Sci-Fi"], moods["Exciting"])
            instance("Toy Story", genres["Family"], moods["Funny"])
            instance("Friday the 13th", genres["Horror"], moods["Scary"])
            instance("Bambi", genres["Family"], moods["Sentimental"])
            instance("The Godfather", genres["Action"], moods["Exciting"])
            instance("Lord of the Rings", genres["Action"], moods["Exciting"])
            instance("Forrest Gump", genres["Comedy"], moods["Funny"])
            instance("Lion King", genres["Family"], moods["Sentimental"])
            instance("Zoolander", genres["Comedy"], moods["Funny"])
            instance("The Hangover", genres["Comedy"], moods["Funny"])
            instance("An Inconvenient Truth", genres["Documentary"], moods["Boring"])
            instance("Man on Wire", genres["Documentary"], moods["Boring"])
            instance("Romeo and Juliet", genres["Romantic"], moods["Sentimental"])
            instance("Star Trek", genres["Sci-Fi"], moods["Exciting"])
            instance("Halloween", genres["Horror"], moods["Scary"])
        }
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 900, 600)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "Movies",
                body = """
                    An IAC network relating sixteen movies, from Bambi and The Lion King to The Godfather and Friday the
                    13th, to genre labels (Horror, Documentary, Comedy, Romantic, Sci-Fi, Action, Family) and mood labels
                    (Exciting, Boring, Sentimental, Funny, Scary). The central instance nodes are labelled with movie titles
                    and connect each movie directly to its properties.

                    Activate a title to retrieve its genre and mood, or activate a label such as Scary or Family to see which
                    movies the network associates with it, and which other labels tend to come along.
                """,
                credits = "Student network from 2015, from Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
