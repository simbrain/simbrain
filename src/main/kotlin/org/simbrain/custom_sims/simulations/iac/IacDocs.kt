/**
 * Shared sidebar documentation for the IAC simulations: each simulation supplies its own description and credits,
 * and this file adds the common background on IAC networks and the instructions for running them.
 */
package org.simbrain.custom_sims.simulations.iac

/**
 * Markdown for the sidebar of an IAC simulation. [body] describes the specific network and goes between the title
 * and the shared background and instructions; [credits] is one line naming who built the network.
 */
fun iacSidebarText(title: String, body: String, credits: String, whatToDo: String = defaultIacWhatToDo): String = """
# $title

${body.trimIndent()}

# How IAC Networks Work

[Interactive activation and competition](https://en.wikipedia.org/wiki/Interactive_activation_and_competition_networks)
(IAC) networks are hand-coded associative networks. Nodes are grouped into pools; nodes within a pool inhibit each
other, and nodes in different pools that belong together excite each other. Activating some nodes and running the
network spreads activation to related nodes, so the network settles into a pattern that completes the cue. This
models associative memory retrieval, including generalization from partial or ambiguous cues.

# What to Do

${whatToDo.trimIndent()}

# References

McClelland, J. L. (1981). [_Retrieving general and specific information from stored knowledge of specifics_](https://apps.dtic.mil/sti/citations/ADA100702). Proceedings of the Third Annual Conference of the Cognitive Science Society.

# Credits

${credits.trimIndent()}
""".trimIndent()

val defaultIacWhatToDo = """
- Click a node to select it and press the up arrow key to add activation to it. Then press play in the main toolbar and watch activation spread to related nodes.
- To try another cue, press `N` in the network window to select all neurons and `C` to clear their activations.
- The network has many synapses. Press `5` in the network window to hide the weight lines if they clutter the view.
- Try activating a node in a property pool rather than an instance to see which instances share that property, and try activating two nodes at once to see how the network resolves conflicting cues.
""".trimIndent()
