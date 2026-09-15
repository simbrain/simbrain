package org.simbrain.custom_sims.simulations.iac

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Student-built IAC network of board and card games in a household, ported from the Simbrain 3 workspace
 * GamesAtAlivias_2018.
 */
val iacGames = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Games at Alivia's")
    networkComponent.network.iacNetwork {
        val owner = pool(
            "Owner",
            "Alivia", "Blake", "Keddoe",
            at = point(-250, -180), columns = 3
        )
        val type = pool(
            "Type of Game",
            "Cooperative Deck Builder", "Cooperative Strategy", "Deck Builder", "Living Card Game", "Party", "Strategy",
            at = point(350, -180), columns = 3, hSpacing = 200.0
        )
        val players = pool(
            "Number of Players",
            "1-4", "1-5", "2-4", "2-5", "2-6", "2-7", "2-8", "3-5", "3-6", "3-10", "4-8", "4-10", "4-30", "5-6", "2+",
            at = point(800, 150), columns = 3, hSpacing = 70.0
        )
        val time = pool(
            "Time to Play in Minutes",
            ">5", ">15", ">30", "15+", "30+", "45+", "60+", "90+", "120+",
            at = point(-800, 150), columns = 3, hSpacing = 70.0
        )
        instances("Games", at = point(0, 150), columns = 10) {
            instance("Ascension", owner["Alivia"], type["Deck Builder"], players["1-4"], time["30+"])
            instance("Exploding Kittens: NSFW", owner["Alivia"], type["Party"], players["2-5"], time[">15"])
            instance("Game of Thrones The Board Game", owner["Alivia"], type["Strategy"], players["3-6"], time["120+"])
            instance("Palabra", owner["Alivia"], type["Party"], players["2-6"], time[">30"])
            instance("Phase 10", owner["Alivia"], type["Party"], players["2-6"], time["60+"])
            instance("Risk", owner["Alivia"], type["Strategy"], players["2-5"], time["90+"])
            instance("Unstable Unicorns", owner["Alivia"], type["Party"], players["2-8"], time[">30"])
            instance("We Didn't Playtest This At All", owner["Alivia"], type["Party"], players["2+"], time[">5"])
            instance("Yogi", owner["Alivia"], type["Party"], players["3-10"], time[">30"])
            instance("Anti-Monopoly", owner["Blake"], type["Strategy"], players["2-6"], time["90+"])
            instance("Apples to Apples", owner["Blake"], type["Party"], players["4-10"], time[">30"])
            instance("Cards Against Humanity", owner["Blake"], type["Party"], players["4-30"], time["30+"])
            instance("Citadels", owner["Blake"], type["Strategy"], players["2-7"], time["30+"])
            instance("Dominos", owner["Blake"], type["Party"], players["2-4"], time["30+"])
            instance("Exploding Kittens", owner["Blake"], type["Party"], players["2-5"], time[">15"])
            instance("Globalization", owner["Blake"], type["Strategy"], players["2-6"], time["120+"])
            instance("Illuminati", owner["Blake"], type["Party"], players["2-6"], time["60+"])
            instance("Medieval Mastery", owner["Blake"], type["Strategy"], players["2-6"], time["30+"])
            instance("Pandemic", owner["Blake"], type["Cooperative Strategy"], players["2-4"], time["45+"])
            instance("Pandemic: Reign of Cthulu", owner["Blake"], type["Cooperative Strategy"], players["2-4"], time["45+"])
            instance("Risk: GodStorm", owner["Blake"], type["Strategy"], players["2-5"], time["90+"])
            instance("Settlers of Catan", owner["Blake"], type["Strategy"], players["5-6"], time["90+"])
            instance("Sheriff of Nottingham", owner["Blake"], type["Party"], players["3-5"], time["30+"])
            instance("Superfight", owner["Blake"], type["Party"], players["3-10"], time["30+"])
            instance("Twilight Imperium 3rd Edition", owner["Blake"], type["Strategy"], players["3-6"], time["120+"])
            instance("XCOM", owner["Blake"], type["Cooperative Strategy"], players["1-4"], time["60+"])
            instance("Alicematic Heroes", owner["Keddoe"], type["Strategy"], players["3-5"], time["60+"])
            instance("Anima: The Twilight of the Gods", owner["Keddoe"], type["Strategy"], players["2-5"], time["60+"])
            instance("Ascendants of Aetheros", owner["Keddoe"], type["Strategy"], players["2-4"], time["30+"])
            instance("Ashes Rise of the Phoenixborn", owner["Keddoe"], type["Living Card Game"], players["2-4"], time["15+"])
            instance("Bears VS Babies", owner["Keddoe"], type["Party"], players["2-5"], time[">30"])
            instance("Chocobo's Crystal Hunt", owner["Keddoe"], type["Party"], players["3-5"], time[">30"])
            instance("Don't Turn Your Back", owner["Keddoe"], type["Strategy"], players["2-4"], time["45+"])
            instance("Dynamite Nurse", owner["Keddoe"], type["Deck Builder"], players["3-5"], time["45+"])
            instance("Harry Potter Hogwarts Battle", owner["Keddoe"], type["Cooperative Deck Builder"], players["2-4"], time["60+"])
            instance("Karate Fight", owner["Keddoe"], type["Party"], players["2-4"], time[">15"])
            instance("Munchkin Bites!", owner["Keddoe"], type["Party"], players["3-6"], time["90+"])
            instance("Munchkin Zombies", owner["Keddoe"], type["Party"], players["3-6"], time["90+"])
            instance("Nightfall", owner["Keddoe"], type["Deck Builder"], players["2-5"], time["45+"])
            instance("Ninja All-Stars", owner["Keddoe"], type["Strategy"], players["2-4"], time["90+"])
            instance("One Night Ultimate Werewolf", owner["Keddoe"], type["Party"], players["3-10"], time["15+"])
            instance("Scott Pilgram's Precious Little Card Game", owner["Keddoe"], type["Deck Builder"], players["1-4"], time["30+"])
            instance("Super Dungeon Explore", owner["Keddoe"], type["Strategy"], players["2-6"], time["120+"])
            instance("Tanto Cuore", owner["Keddoe"], type["Deck Builder"], players["2-4"], time["45+"])
            instance("Tentacle Bento", owner["Keddoe"], type["Party"], players["2-6"], time["30+"])
            instance("The Dresden Files Cooperative Card Game", owner["Keddoe"], type["Cooperative Strategy"], players["1-5"], time["30+"])
            instance("The Great Dalmuti", owner["Keddoe"], type["Party"], players["4-8"], time["15+"])
            instance("Three Dragon Ante: Emperor's Gambit", owner["Keddoe"], type["Party"], players["2-6"], time["30+"])
            instance("Wrath of Ashardalon", owner["Keddoe"], type["Cooperative Strategy"], players["1-5"], time["60+"])
            instance("XenoShyft Onslaught", owner["Keddoe"], type["Cooperative Deck Builder"], players["1-4"], time["30+"])
        }
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 1000, 500)
        addSidebarInfo(
            iacSidebarText(
                title = "Games at Alivia's",
                body = """
                    An IAC network modeling a person's conceptual understanding of the various games in their household.
                    There are four property pools: the owner of each game, the type of game, the number of players each
                    game can accommodate, and approximately how long each game takes to play at minimum. The 50 game titles
                    form the instance pool.

                    Six general concepts describe the types of games. A deck-building game is one in which your hand or
                    deck is created by purchasing or gaining cards within the game. A living card game comes with a set
                    number of decks but adds decks or cards later as expansions. Strategy games are won through particular
                    tactics, and cooperative games are based on a player-versus-game mechanic.

                    The author reported the following experiments:

                    1. Activating the Alivia node at first produced the correct games, but with further iterations the
                    network dropped those that did not share the strong characteristics of "her" games. The network decided
                    she is most related to party games for 2 to 6 players in under 30 minutes, and so only Palabra, Phase
                    10, Unstable Unicorns, and Yogi, despite her owning five others that do not fit this description.
                    2. Activating Blake produced the same effect. After 100 iterations the network decided that Blake is
                    related to party and strategy games for 2 to 6 players taking 30 minutes or more, above all Cards
                    Against Humanity, Dominos, Medieval Mastery, Sheriff of Nottingham, and Superfight.
                    3. Activating Keddoe likewise settled on party or strategy games for 2 to 4 or 2 to 6 players taking 30
                    minutes or more: Ascendants of Aetheros, Karate Fight, Tentacle Bento, and Three Dragon Ante.

                    The network was accurate about the types of games Alivia plays but less so for Blake and Keddoe, who by
                    their own account prefer deck-building games and long strategy games. The network does not take into
                    account how often each game is played; it only weighs all the games associated with a person and the
                    characteristics they share, which explains why it generalizes the way it does.
                """,
                credits = "Alivia Harris, COGS 110, Spring 2018. From Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
