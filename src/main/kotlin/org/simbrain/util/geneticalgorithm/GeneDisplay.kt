package org.simbrain.util.geneticalgorithm

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.Theme
import org.simbrain.util.format
import java.awt.*
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import kotlin.reflect.KProperty1

/**
 * Tools for viewing genotypes.
 *
 * Each displayed chromosome appears as a row. Each gene is displayed as one "card" in this row.
 *
 */

/**
 * A field shown on each gene card in the display.
 */
sealed class GeneField<G>(val label: String) {

    abstract fun getValue(gene: G): String
}

private class TemplateField<G : Gene<*, T>, T>(
    label: String,
    val extractor: (G) -> Any?,
    val precision: Int?
) : GeneField<G>(label) {
    override fun getValue(gene: G): String {
        val raw = extractor(gene)
        if (raw is Double && precision != null) return raw.format(precision)
        if (raw is Float && precision != null) return raw.toDouble().format(precision)
        return raw?.toString() ?: ""
    }
}

private class FormattedField<G>(label: String, val formatter: (G) -> Any?) : GeneField<G>(label) {
    override fun getValue(gene: G) = formatter(gene)?.toString() ?: ""
}

/**
 * Builds the fields shown on each gene card in a section.
 *
 * Use [template] to show a template property, [formatted] for custom text,
 * `+` to add a field, and `-` to remove a default template field.
 */
class GeneCardConfig<G : Gene<*, T>, T> internal constructor(
    private val defaultFields: List<GeneField<G>> = emptyList(),
    private val defaultPrecision: Int
) {
    private val headers = mutableListOf<GeneField<G>>()
    private val added = mutableListOf<GeneField<G>>()
    private val removed = mutableSetOf<String>()

    /**
     * Create a field from a property on the gene template.
     */
    fun <V> template(prop: KProperty1<T, V>, precision: Int? = null): GeneField<G> =
        TemplateField(prop.name, { prop.get(it.template) }, precision ?: defaultPrecision)

    /**
     * Create a field whose value is computed from the gene.
     */
    fun formatted(label: String, block: (G) -> Any?): GeneField<G> =
        FormattedField(label, block)

    /**
     * Add a field to the front of the card, before default fields.
     */
    fun header(field: GeneField<G>) {
        headers.add(field)
    }

    /**
     * Add this field to the display.
     */
    operator fun GeneField<G>.unaryPlus() {
        added.add(this)
    }

    /**
     * Remove a default template field by property name.
     */
    operator fun KProperty1<T, *>.unaryMinus() {
        removed.add(this.name)
    }

    internal fun resolve(): List<GeneField<G>> {
        val defaults = defaultFields.filter { it.label !in removed }
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
    val fields: List<GeneField<G>>
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
     * By default this includes a `bias` field unless [noDefaults] is true.
     */
    fun display(
        slot: NodeGeneGroup,
        noDefaults: Boolean = false,
        block: GeneCardConfig<NodeGene, Neuron>.() -> Unit = {}
    ) {
        val defaults = if (noDefaults) emptyList()
            else listOf(TemplateField<NodeGene, Neuron>("bias", { it.template.bias }, defaultPrecision))
        val config = GeneCardConfig<NodeGene, Neuron>(
            defaultFields = defaults,
            defaultPrecision = defaultPrecision
        )
        config.block()
        val name = findGroupName(slot) ?: "nodes"
        sections.add(ChromosomeDisplaySection(name, "NodeChromosome", slot.genes, config.resolve()))
    }

    /**
     * Display a group of connection genes.
     *
     * By default this includes a `strength` field unless [noDefaults] is true.
     */
    fun display(
        slot: ConnectionGeneGroup,
        noDefaults: Boolean = false,
        block: GeneCardConfig<ConnectionGene, Synapse>.() -> Unit = {}
    ) {
        val defaults = if (noDefaults) emptyList()
            else listOf(TemplateField<ConnectionGene, Synapse>("strength", { it.template.strength }, defaultPrecision))
        val config = GeneCardConfig<ConnectionGene, Synapse>(
            defaultFields = defaults,
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
        block: GeneCardConfig<G, T>.() -> Unit = {}
    ) {
        val config = GeneCardConfig<G, T>(defaultPrecision = defaultPrecision)
        config.block()
        val name = findGroupName(slot) ?: "linked"
        sections.add(ChromosomeDisplaySection(name, "LinkedChromosome", slot.genes, config.resolve()))
    }

    /**
     * Display a collection gene group containing a single gene.
     */
    fun <G : Gene<Unit, T>, T> display(
        slot: CollectionGeneGroup<G>,
        block: GeneCardConfig<G, T>.() -> Unit = {}
    ) {
        val config = GeneCardConfig<G, T>(defaultPrecision = defaultPrecision)
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
            add(JLabel("Genome").apply { font = Theme.heading })
            metadata?.let { meta ->
                add(JLabel("Genome ${meta.id}").apply { font = Theme.label; foreground = Theme.mutedText })
                meta.parentId?.let { pid ->
                    add(JLabel("Parent $pid").apply { font = Theme.label; foreground = Theme.mutedText })
                }
                add(JLabel("Generation ${meta.generation}").apply { font = Theme.label; foreground = Theme.mutedText })
                add(JLabel("$metricLabel ${meta.fitness.format(4)}").apply { font = Theme.bodyBold })
            }
        }
    }

    private fun buildLabelPanel(section: ChromosomeDisplaySection<*>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JLabel(section.label).apply {
                font = Theme.section
                alignmentX = RIGHT_ALIGNMENT
            })
            add(JLabel(section.typeName).apply {
                font = Theme.type
                foreground = Theme.mutedText
                alignmentX = RIGHT_ALIGNMENT
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <G : Gene<*, *>> buildCardsPanel(section: ChromosomeDisplaySection<G>): JPanel {
        val fields = section.fields as List<GeneField<G>>

        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            isOpaque = false
        }

        if (section.genes.isEmpty() || fields.isEmpty()) {
            panel.add(JLabel("(empty)").apply {
                font = Theme.label
                foreground = Theme.mutedText
            })
            return panel
        }

        for (gene in section.genes) {
            panel.add(buildCard(gene, fields))
        }

        return panel
    }

    private fun <G : Gene<*, *>> buildCard(gene: G, fields: List<GeneField<G>>): JPanel {
        val card = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = Theme.roundedCard(radius = 8, padding = 8)
            isOpaque = false
        }

        for (field in fields) {
            val value = field.getValue(gene)
            val line = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply { isOpaque = false }
            line.add(JLabel(field.label).apply {
                font = Theme.label
                foreground = Theme.mutedText
            })
            line.add(JLabel(value).apply {
                font = Theme.bodyBold
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
