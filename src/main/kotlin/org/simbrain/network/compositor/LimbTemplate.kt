package org.simbrain.network.compositor

/**
 * Declarative grid layout for one limb's interior, in the spirit of CSS grid-template-areas:
 * rows of whitespace-separated cells, where `.` is an empty cell and `a+b` puts two endpoints
 * side by side in one cell. Columns align across rows and size to their widest cell; rows size
 * to their tallest item. Keys are tile ids, or graph aliases for junction ops.
 *
 * A template applies to a limb when its keys exactly cover the limb's endpoints, so a template
 * written for one op structure silently steps aside — falling back to the rank-column layout —
 * if the model's plan changes shape.
 */
class LimbTemplate(
    /** rows → columns → the endpoint keys sharing that cell, left to right. */
    val cells: List<List<List<String>>>,
    /** Gap below each row but the last; null entries (or a null list) use the layout default. */
    val rowGaps: List<Double?>? = null,
) {

    val keys: Set<String> = cells.flatten().flatten().toSet()

    init {
        require(cells.isNotEmpty() && cells.first().isNotEmpty()) { "Template must have cells" }
        val columns = cells.first().size
        require(cells.all { it.size == columns }) { "Template rows must have equal column counts" }
        require(cells.flatten().flatten().size == keys.size) { "Duplicate key in template" }
        rowGaps?.let {
            require(it.size == cells.size - 1) { "Need one row gap per row boundary" }
        }
    }

    companion object {
        fun parse(areas: String, rowGaps: List<Double?>? = null) = LimbTemplate(
            areas.trim().lines().map { line ->
                line.trim().split(Regex("\\s+")).map { cell ->
                    if (cell == ".") emptyList() else cell.split("+")
                }
            },
            rowGaps,
        )
    }
}
