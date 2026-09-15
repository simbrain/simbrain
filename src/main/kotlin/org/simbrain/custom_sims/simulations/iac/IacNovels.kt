package org.simbrain.custom_sims.simulations.iac

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Student-built IAC network of novel characters, ported from the Simbrain 3 workspace Novels_2015.
 */
val iacNovels = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Novels")
    networkComponent.network.iacNetwork {
        val characters = pool(
            "Characters",
            "Frankenstein", "Phantom", "Dracula", "Don Quixote", "Raoul, Vicomte de Chagny", "Jane Eyre", "Dorothy", "Jo March", "Atticus Finch",
            at = point(0, -160), columns = 5, hSpacing = 160.0
        )
        val roles = pool(
            "Roles",
            "Monster", "Hero", "Heroine",
            at = point(-550, -70), columns = 1
        )
        val centuries = pool(
            "Centuries",
            "17th c", "20th c", "19th c",
            at = point(550, -70), columns = 1
        )
        val genres = pool(
            "Genres",
            "Fiction/Gothic", "Fiction/Classic", "Fiction/Drama",
            at = point(0, 180), columns = 3, hSpacing = 150.0
        )
        instances("Novels", at = point(0, 20), columns = 5, hSpacing = 160.0) {
            instance(characters["Frankenstein"], roles["Monster"], centuries["19th c"], genres["Fiction/Gothic"])
            instance(characters["Phantom"], roles["Monster"], centuries["20th c"], genres["Fiction/Gothic"])
            instance(characters["Dracula"], roles["Monster"], centuries["19th c"], genres["Fiction/Gothic"])
            instance(characters["Don Quixote"], roles["Hero"], centuries["17th c"], genres["Fiction/Drama"])
            instance(characters["Raoul, Vicomte de Chagny"], roles["Hero"], centuries["20th c"], genres["Fiction/Gothic"])
            instance(characters["Jane Eyre"], roles["Heroine"], centuries["19th c"], genres["Fiction/Classic"])
            instance(characters["Dorothy"], roles["Heroine"], centuries["20th c"], genres["Fiction/Classic"])
            instance(characters["Jo March"], roles["Heroine"], centuries["19th c"], genres["Fiction/Drama"])
            instance(characters["Atticus Finch"], roles["Hero"], centuries["20th c"], genres["Fiction/Drama"])
        }
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 550)
        addSidebarInfo(
            iacSidebarText(
                title = "Novels",
                body = """
                    A small IAC network of characters from well-known novels, from Frankenstein's monster and Dracula to Jane
                    Eyre, Jo March, and Atticus Finch. Each character is linked through an instance node to a role
                    (Monster, Hero, Heroine), a century (17th, 19th, or 20th), and a genre (Gothic, Classic, or Drama
                    fiction).

                    Activate a character to retrieve their role, period, and genre. Activate Monster or Fiction/Gothic to
                    see which characters are recalled together, or combine cues such as Heroine and 20th c.
                """,
                credits = "Student network from 2015, from Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
