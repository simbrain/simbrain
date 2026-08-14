package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenProbabilityCardNodeTest {

    private fun rankedSnapshot(count: Int) = TokenProbabilitySnapshot(
        entries = List(count) { TokenProbabilitySnapshot.Entry(it, 1.0 / (it + 1)) },
        argmaxTokenId = 0,
        sampledTokenId = count - 1,
        showAll = false,
    )

    @Test
    fun `ranked mode never lays a row past the card height`() {
        val style = TokenProbabilityCardStyle()
        val card = TokenProbabilityCardNode({ "t$it" }, style)
        card.refresh(rankedSnapshot(21))
        val bottom = card.fullBoundsReference.maxY
        assertTrue(bottom <= style.height + 1.0,
            "card content must stay inside its ${style.height}px background, was $bottom")
    }

    @Test
    fun `ranked pager walks all candidates`() {
        val style = TokenProbabilityCardStyle()
        val card = TokenProbabilityCardNode({ "t$it" }, style)
        val snapshot = rankedSnapshot(21)
        card.refresh(snapshot)

        val seen = mutableSetOf<Int>()
        var guard = 0
        while (guard++ < 10) {
            val before = seen.size
            seen += visibleTokenIds(card)
            if (seen.size == snapshot.entries.size) break
            card.scroll(1)
            if (seen.size == before && visibleTokenIds(card).all { it in seen }) break
        }
        assertEquals(snapshot.entries.size, seen.size, "paging must reach every candidate")
    }

    private fun visibleTokenIds(card: TokenProbabilityCardNode): List<Int> {
        // Ranked rows render one label PText per entry beside the circle: "t<id>".
        val texts = mutableListOf<Int>()
        fun walk(node: org.piccolo2d.PNode) {
            if (node is org.piccolo2d.nodes.PText) {
                Regex("^t(\\d+)$").find(node.text ?: "")?.let { texts.add(it.groupValues[1].toInt()) }
            }
            for (i in 0 until node.childrenCount) walk(node.getChild(i))
        }
        walk(card)
        return texts
    }
}
