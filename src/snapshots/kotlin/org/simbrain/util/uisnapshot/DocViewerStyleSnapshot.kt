/**
 * Renders sample sidebar documentation in the doc viewer to check heading styles. The panel is always built under
 * the light theme; run with `-Ptheme=dark` and the harness switches it to dark afterwards, which exercises the
 * live theme color hook in DocViewerViewPanel.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.docviewer.DocViewer
import org.simbrain.docviewer.DocViewerViewPanel
import java.awt.Component
import java.awt.Dimension

private val sampleDoc = """
    # Visual World Paradigm
    A model of how hearing a word pulls the eyes and hand toward objects whose names sound similar (Spivey, 2025).

    # Activities

    ## Activity 1: Compare the three conditions
    See how a similar-sounding word bends the mouse trace.

    1. Click `Control condition`.
    2. Click `iterate` repeatedly.
    3. Repeat with `Cohort condition` and `Rhyming condition`.
       - Observe the cohort trace bulges most strongly.

    **What you should see:** the cohort trace bulges toward the candy; the rhyming bulge is smaller and later.

    ## Activity 2: Plot activations
    1. Right-click a group's interaction box.
    2. Select `Plot > Time Series Plot`.
    3. Run a condition again.

    ### Notes on the plot
    Colors differ from the paper's figure. See [the paper](https://example.com).

    ## Explore further
    - Change the competition weights.
    - Try a new word set.
""".trimIndent()

private fun buildDocPanel(): DocViewerViewPanel {
    val html = DocViewer().apply { text = sampleDoc; render() }.renderedText
    return DocViewerViewPanel().apply {
        text = html
        preferredSize = Dimension(340, 720)
    }
}

class DocViewerStyleSnapshot : UiSnapshotDef {
    override val name = "doc_viewer_style"
    override fun build(): Component {
        setupTheme("light")
        return buildDocPanel()
    }
}
