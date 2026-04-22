package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.format
import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.AbstractBorder
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import kotlin.reflect.KProperty1

/**
 * Tools for viewing genotypes.
 *
 * Each displayed chromosome appears as a row. Each gene is displayed as one "card" in this row.
 *
 */

/**
 * A column shown on each gene card in the display.
 */
sealed class GeneColumn<G>(val label: String) {

    abstract fun getValue(gene: G): String
}

private class TemplateColumn<G : Gene<*, T>, T>(
    label: String,
    val extractor: (G) -> Any?,
    val precision: Int?
) : GeneColumn<G>(label) {
    override fun getValue(gene: G): String {
        val raw = extractor(gene)
        if (raw is Double && precision != null) return raw.format(precision)
        if (raw is Float && precision != null) return raw.toDouble().format(precision)
        return raw?.toString() ?: ""
    }
}

private class FormattedColumn<G>(label: String, val formatter: (G) -> Any?) : GeneColumn<G>(label) {
    override fun getValue(gene: G) = formatter(gene)?.toString() ?: ""
}

/**
 * Builds the columns for a displayed gene group.
 *
 * Use [template] to show a template property, [formatted] for custom text,
 * `+` to add a column, and `-` to remove a default template column.
 */
class GeneColumnConfig<G : Gene<*, T>, T> internal constructor(
    private val defaultColumns: List<GeneColumn<G>> = emptyList(),
    private val defaultPrecision: Int
) {
    private val headers = mutableListOf<GeneColumn<G>>()
    private val added = mutableListOf<GeneColumn<G>>()
    private val removed = mutableSetOf<String>()

    /**
     * Create a column from a property on the gene template.
     */
    fun <V> template(prop: KProperty1<T, V>, precision: Int? = null): GeneColumn<G> =
        TemplateColumn(prop.name, { prop.get(it.template) }, precision ?: defaultPrecision)

    /**
     * Create a column whose value is computed from the gene.
     */
    fun formatted(label: String, block: (G) -> Any?): GeneColumn<G> =
        FormattedColumn(label, block)

    /**
     * Add a column to the front of the card, before default columns.
     */
    fun header(column: GeneColumn<G>) {
        headers.add(column)
    }

    /**
     * Add this column to the display.
     */
    operator fun GeneColumn<G>.unaryPlus() {
        added.add(this)
    }

    /**
     * Remove a default template column by property name.
     */
    operator fun KProperty1<T, *>.unaryMinus() {
        removed.add(this.name)
    }

    internal fun resolve(): List<GeneColumn<G>> {
        val defaults = defaultColumns.filter { it.label !in removed }
        return headers + defaults + added
    }
}

/**
 * The data needed to render one chromosome row in the panel.
 */
class ChromosomeDisplaySection<G : Gene<*, *>>(
    val label: String,
    val typeName: String,
    val genes: List<G>,
    val columns: List<GeneColumn<G>>
)

/**
 * Builds the sections used by [GeneDisplayPanel].
 *
 * Each `display(...)` call adds one chromosome or gene group to the panel.
 */
class GeneDisplayBuilder(
    private val genotype: Genotype,
    private val defaultPrecision: Int = 4
) {
    private val sections = mutableListOf<ChromosomeDisplaySection<*>>()

    /**
     * Display a group of node genes.
     *
     * By default this includes a `bias` column unless [noDefaults] is true.
     */
    fun display(
        slot: NodeGeneGroup,
        noDefaults: Boolean = false,
        block: GeneColumnConfig<NodeGene, Neuron>.() -> Unit = {}
    ) {
        val defaults = if (noDefaults) emptyList()
            else listOf(TemplateColumn<NodeGene, Neuron>("bias", { it.template.bias }, defaultPrecision))
        val config = GeneColumnConfig<NodeGene, Neuron>(
            defaultColumns = defaults,
            defaultPrecision = defaultPrecision
        )
        config.block()
        val name = findGroupName(slot) ?: "nodes"
        sections.add(ChromosomeDisplaySection(name, "NodeChromosome", slot.genes, config.resolve()))
    }

    /**
     * Display a group of connection genes.
     *
     * By default this includes a `strength` column unless [noDefaults] is true.
     */
    fun display(
        slot: ConnectionGeneGroup,
        noDefaults: Boolean = false,
        block: GeneColumnConfig<ConnectionGene, Synapse>.() -> Unit = {}
    ) {
        val defaults = if (noDefaults) emptyList()
            else listOf(TemplateColumn<ConnectionGene, Synapse>("strength", { it.template.strength }, defaultPrecision))
        val config = GeneColumnConfig<ConnectionGene, Synapse>(
            defaultColumns = defaults,
            defaultPrecision = defaultPrecision
        )
        config.block()
        val name = findGroupName(slot) ?: "connections"
        sections.add(ChromosomeDisplaySection(name, "ConnectionChromosome", slot.genes, config.resolve()))
    }

    /**
     * Display a linked gene group.
     */
    fun <G : Gene<Unit, T>, T> display(
        slot: LinkedGeneGroup<G>,
        block: GeneColumnConfig<G, T>.() -> Unit = {}
    ) {
        val config = GeneColumnConfig<G, T>(defaultPrecision = defaultPrecision)
        config.block()
        val name = findGroupName(slot) ?: "linked"
        sections.add(ChromosomeDisplaySection(name, "LinkedChromosome", slot.genes, config.resolve()))
    }

    /**
     * Display a collection gene group containing a single gene.
     */
    fun <G : Gene<Unit, T>, T> display(
        slot: CollectionGeneGroup<G>,
        block: GeneColumnConfig<G, T>.() -> Unit = {}
    ) {
        val config = GeneColumnConfig<G, T>(defaultPrecision = defaultPrecision)
        config.block()
        val name = findGroupName(slot) ?: "collection"
        sections.add(ChromosomeDisplaySection(name, "CollectionLinked", listOf(slot.gene), config.resolve()))
    }

    private fun findGroupName(geneGroup: GeneGroup): String? =
        genotype.geneGroups.firstOrNull { it.second.group === geneGroup }?.first

    /**
     * Build the configured sections without creating a panel yet.
     */
    internal fun buildSections(): List<ChromosomeDisplaySection<*>> = sections.toList()

    /**
     * Build a panel from the currently configured sections.
     */
    fun build(): GeneDisplayPanel = GeneDisplayPanel(sections)
}

/**
 * Rounded border used for each gene card.
 */
private class RoundedBorder(
    private val radius: Int,
    private val borderColor: Color,
    private val fillColor: Color?
) : AbstractBorder() {

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val shape = RoundRectangle2D.Float(
            x + 0.5f, y + 0.5f, w - 1f, h - 1f, radius.toFloat(), radius.toFloat()
        )
        if (fillColor != null) {
            g2.color = fillColor
            g2.fill(shape)
        }
        g2.color = borderColor
        g2.draw(shape)
        g2.dispose()
    }

    override fun getBorderInsets(c: Component) = Insets(radius / 2, radius / 2, radius / 2, radius / 2)

    override fun isBorderOpaque() = false
}

private val LABEL_COLOR = Color(100, 100, 100)
private val VALUE_FONT = Font("SansSerif", Font.BOLD, 11)
private val LABEL_FONT = Font("SansSerif", Font.PLAIN, 10)
private val TYPE_FONT = Font("SansSerif", Font.ITALIC, 9)
private val SECTION_FONT = Font("SansSerif", Font.BOLD, 12)
private val TITLE_FONT = Font("SansSerif", Font.BOLD, 14)
private val CARD_BORDER_COLOR = Color(180, 180, 180)
private val CARD_BG = Color(250, 250, 250)

/**
 * Swing panel that renders chromosome sections as labeled rows of gene cards.
 */
class GeneDisplayPanel(
    sections: List<ChromosomeDisplaySection<*>> = emptyList(),
    internal var metadata: SimMetadata? = null,
    internal var metricLabel: String = "Score",
    private val renderer: ((Genotype) -> List<ChromosomeDisplaySection<*>>)? = null
) : JPanel(GridBagLayout()) {

    init {
        border = EmptyBorder(8, 12, 8, 12)
        background = Color.WHITE
        renderSections(sections)
    }

    /**
     * Replace the current sections and optional metadata.
     */
    fun refresh(sections: List<ChromosomeDisplaySection<*>>, metadata: SimMetadata? = this.metadata) {
        this.metadata = metadata
        SwingUtilities.invokeLater {
            removeAll()
            renderSections(sections)
            revalidate()
            repaint()
        }
    }

    /**
     * Refresh with a new genotype, using the baked-in renderer.
     */
    fun refreshFrom(genotype: Genotype, metadata: SimMetadata? = this.metadata) {
        val sections = renderer?.invoke(genotype) ?: error("No renderer — use the factory function geneDisplayPanel()")
        refresh(sections, metadata)
    }

    /**
     * Bind to an [EvolutionRunner], auto-refreshing from the best [EvoSim]'s genotype each generation.
     */
    fun bind(runner: EvolutionRunner) {
        require(renderer != null) { "No renderer — use the factory function geneDisplayPanel()" }
        runner.events.generationUpdated.on { state ->
            val g = state.best.genotype
            refreshFrom(g, state.bestMetadata)
        }
    }

    private fun renderSections(sections: List<ChromosomeDisplaySection<*>>) {
        val gbc = GridBagConstraints()

        // Title row with metadata
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(0, 0, 6, 0)
        add(buildTitlePanel(), gbc)

        gbc.gridwidth = 1
        for ((i, section) in sections.withIndex()) {
            val row = i + 1
            gbc.gridy = row
            gbc.insets = Insets(3, 0, 3, 8)

            // Label column (fixed width, right-aligned to the cards)
            gbc.gridx = 0
            gbc.anchor = GridBagConstraints.NORTHEAST
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            add(buildLabelPanel(section), gbc)

            // Cards column (takes remaining space)
            gbc.gridx = 1
            gbc.anchor = GridBagConstraints.WEST
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            add(buildCardsPanel(section), gbc)
        }
    }

    private fun buildTitlePanel(): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            isOpaque = false
            add(JLabel("Genome").apply { font = TITLE_FONT })
            metadata?.let { meta ->
                add(JLabel("Genome ${meta.id}").apply { font = LABEL_FONT; foreground = LABEL_COLOR })
                meta.parentId?.let { pid ->
                    add(JLabel("Parent $pid").apply { font = LABEL_FONT; foreground = LABEL_COLOR })
                }
                add(JLabel("Generation ${meta.generation}").apply { font = LABEL_FONT; foreground = LABEL_COLOR })
                add(JLabel("$metricLabel ${meta.fitness.format(4)}").apply { font = VALUE_FONT })
            }
        }
    }

    private fun buildLabelPanel(section: ChromosomeDisplaySection<*>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JLabel(section.label).apply {
                font = SECTION_FONT
                alignmentX = RIGHT_ALIGNMENT
            })
            add(JLabel(section.typeName).apply {
                font = TYPE_FONT
                foreground = LABEL_COLOR
                alignmentX = RIGHT_ALIGNMENT
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <G : Gene<*, *>> buildCardsPanel(section: ChromosomeDisplaySection<G>): JPanel {
        val columns = section.columns as List<GeneColumn<G>>

        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            isOpaque = false
        }

        if (section.genes.isEmpty() || columns.isEmpty()) {
            panel.add(JLabel("(empty)").apply {
                font = LABEL_FONT
                foreground = LABEL_COLOR
            })
            return panel
        }

        for (gene in section.genes) {
            panel.add(buildCard(gene, columns))
        }

        return panel
    }

    private fun <G : Gene<*, *>> buildCard(gene: G, columns: List<GeneColumn<G>>): JPanel {
        val card = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = CompoundBorder(
                RoundedBorder(8, CARD_BORDER_COLOR, CARD_BG),
                EmptyBorder(4, 8, 4, 8)
            )
            isOpaque = false
        }

        for (col in columns) {
            val value = col.getValue(gene)
            val line = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply { isOpaque = false }
            line.add(JLabel(col.label).apply {
                font = LABEL_FONT
                foreground = LABEL_COLOR
            })
            line.add(JLabel(value).apply {
                font = VALUE_FONT
            })
            card.add(line)
        }

        return card
    }
}

/**
 * Create a [GeneDisplayPanel] with a baked-in rendering recipe.
 * The panel can then [bind][GeneDisplayPanel.bind] to a runner or [refreshFrom][GeneDisplayPanel.refreshFrom]
 * with just a genotype — no display block needed at the call site.
 */
@Suppress("UNCHECKED_CAST")
fun <G : Genotype> geneDisplayPanel(
    precision: Int = 4,
    displayBlock: (G) -> GeneDisplayBuilder.() -> Unit
): GeneDisplayPanel {
    return GeneDisplayPanel(renderer = { genotype ->
        val g = genotype as G
        GeneDisplayBuilder(g, precision).apply(displayBlock(g)).buildSections()
    })
}

/**
 * Build a gene display panel for this genotype using the display DSL.
 *
 * Example:
 * ```kotlin
 * genotype.geneticsDisplay {
 *     display(hidden)
 *     display(connections) {
 *         +template(Synapse::strength)
 *         +formatted("In") { nodeIndex(it.source) }
 *     }
 * }
 * ```
 */
fun Genotype.geneticsDisplay(
    precision: Int = 4,
    metricLabel: String = "Score",
    block: GeneDisplayBuilder.() -> Unit
): GeneDisplayPanel {
    return GeneDisplayBuilder(this, precision).apply(block).build().also {
        it.metricLabel = metricLabel
    }
}

/**
 * Bind this panel to an [EvolutionRunner], auto-refreshing each generation.
 * The [extract] lambda maps generation state to the genotype and display block to render.
 * Use [GeneDisplayPanel.bind] (no-arg) with a panel from [geneDisplayPanel] for simpler usage.
 */
fun GeneDisplayPanel.bind(
    runner: EvolutionRunner,
    precision: Int = 4,
    extract: (GenerationState) -> Pair<Genotype, GeneDisplayBuilder.() -> Unit>
) {
    runner.events.generationUpdated.on { state ->
        val (genotype, block) = extract(state)
        val newSections = GeneDisplayBuilder(genotype, precision).apply(block).buildSections()
        refresh(newSections, state.bestMetadata)
    }
}

/**
 * Re-run the display DSL on a (possibly different) genotype and refresh an existing panel.
 */
fun GeneDisplayPanel.refreshFrom(
    genotype: Genotype,
    precision: Int = 4,
    metadata: SimMetadata? = this.metadata,
    metricLabel: String = this.metricLabel,
    block: GeneDisplayBuilder.() -> Unit
) {
    val newSections = GeneDisplayBuilder(genotype, precision).apply(block).buildSections()
    this.metricLabel = metricLabel
    refresh(newSections, metadata)
}
