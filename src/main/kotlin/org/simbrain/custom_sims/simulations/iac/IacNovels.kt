package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * Student-built IAC network of novel characters, ported from the Simbrain 3 workspace Novels_2015.
 */
val iacNovels = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Novels")
    networkComponent.network.iacNetwork {
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
            at = point(0, 290), columns = 3, hSpacing = 150.0
        )
        instances("Characters", at = point(0, 0), columns = 3, hSpacing = 240.0, vSpacing = 85.0) {
            instance("Frankenstein", roles["Monster"], centuries["19th c"], genres["Fiction/Gothic"])
            instance("Phantom", roles["Monster"], centuries["20th c"], genres["Fiction/Gothic"])
            instance("Dracula", roles["Monster"], centuries["19th c"], genres["Fiction/Gothic"])
            instance("Don Quixote", roles["Hero"], centuries["17th c"], genres["Fiction/Drama"])
            instance("Raoul, Vicomte de Chagny", roles["Hero"], centuries["20th c"], genres["Fiction/Gothic"])
            instance("Jane Eyre", roles["Heroine"], centuries["19th c"], genres["Fiction/Classic"])
            instance("Dorothy", roles["Heroine"], centuries["20th c"], genres["Fiction/Classic"])
            instance("Jo March", roles["Heroine"], centuries["19th c"], genres["Fiction/Drama"])
            instance("Atticus Finch", roles["Hero"], centuries["20th c"], genres["Fiction/Drama"])
        }

        roles["Monster"].location = point(-590, -100)
        roles["Hero"].location = point(-490, -80)
        roles["Heroine"].location = point(-535, 10)
        centuries["17th c"].location = point(510, -90)
        centuries["20th c"].location = point(610, -110)
        centuries["19th c"].location = point(555, 5)
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 550)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "Novels",
                body = """
                    A small IAC network of characters from well-known novels, from Frankenstein's monster and Dracula to Jane
                    Eyre, Jo March, and Atticus Finch. Each central instance node is labelled with a character's name and linked to a role
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
