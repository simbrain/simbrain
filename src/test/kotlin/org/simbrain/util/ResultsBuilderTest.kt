/** Checks the plain-text form of results that the results dialog's Copy button puts on the clipboard. */
package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResultsBuilderTest {

    @Test
    fun `results text separates values with tabs and sections with a blank line`() {
        val results = ResultsBuilder().apply {
            section("Matrix") {
                row("Size", "3 × 3")
            }
            section("Eigenvalues", columns = listOf("Real", "Imaginary")) {
                note("Largest magnitude first")
                row("1", "2.00", ".50")
            }
        }
        assertEquals(
            "Matrix\nSize\t3 × 3\n\nEigenvalues\tReal\tImaginary\nLargest magnitude first\n1\t2.00\t.50",
            results.toText()
        )
    }
}
