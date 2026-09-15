package org.simbrain.custom_sims.simulations.iac

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Student-built IAC network of movies and their genres and moods, ported from the Simbrain 3 workspace Movies_2015.
 */
val iacMovies = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Movies")
    networkComponent.network.iacNetwork {
        val titles = pool(
            "Titles",
            "The Notebook", "Star Wars", "Toy Story", "Friday the 13th", "Bambi", "The Godfather", "Lord of the Rings", "Forrest Gump", "Lion King", "Zoolander", "The Hangover", "An Inconvenient Truth", "Man on Wire", "Romeo and Juliet", "Star Trek", "Halloween",
            at = point(0, -160), columns = 8, hSpacing = 140.0
        )
        val genres = pool(
            "Genres",
            "Horror", "Documentary", "Comedy", "Romantic", "Family", "Sci-Fi", "Action",
            at = point(-750, -70), columns = 1
        )
        val moods = pool(
            "Moods",
            "Exciting", "Boring", "Sentimental", "Funny", "Scary",
            at = point(750, -70), columns = 1
        )
        instances("Movies", at = point(0, 20), columns = 8, hSpacing = 140.0) {
            instance(titles["The Notebook"], genres["Romantic"], moods["Sentimental"])
            instance(titles["Star Wars"], genres["Sci-Fi"], moods["Exciting"])
            instance(titles["Toy Story"], genres["Family"], moods["Funny"])
            instance(titles["Friday the 13th"], genres["Horror"], moods["Scary"])
            instance(titles["Bambi"], genres["Family"], moods["Sentimental"])
            instance(titles["The Godfather"], genres["Action"], moods["Exciting"])
            instance(titles["Lord of the Rings"], genres["Action"], moods["Exciting"])
            instance(titles["Forrest Gump"], genres["Comedy"], moods["Funny"])
            instance(titles["Lion King"], genres["Family"], moods["Sentimental"])
            instance(titles["Zoolander"], genres["Comedy"], moods["Funny"])
            instance(titles["The Hangover"], genres["Comedy"], moods["Funny"])
            instance(titles["An Inconvenient Truth"], genres["Documentary"], moods["Boring"])
            instance(titles["Man on Wire"], genres["Documentary"], moods["Boring"])
            instance(titles["Romeo and Juliet"], genres["Romantic"], moods["Sentimental"])
            instance(titles["Star Trek"], genres["Sci-Fi"], moods["Exciting"])
            instance(titles["Halloween"], genres["Horror"], moods["Scary"])
        }
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 900, 600)
        addSidebarInfo(
            iacSidebarText(
                title = "Movies",
                body = """
                    An IAC network relating sixteen movies, from Bambi and The Lion King to The Godfather and Friday the
                    13th, to genre labels (Horror, Documentary, Comedy, Romantic, Sci-Fi, Action, Family) and mood labels
                    (Exciting, Boring, Sentimental, Funny, Scary). The unlabelled nodes are instance nodes, one per movie,
                    that tie each title to its properties.

                    Activate a title to retrieve its genre and mood, or activate a label such as Scary or Family to see which
                    movies the network associates with it, and which other labels tend to come along.
                """,
                credits = "Student network from 2015, from Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
