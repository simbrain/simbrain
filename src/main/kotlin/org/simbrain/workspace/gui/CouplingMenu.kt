package org.simbrain.workspace.gui

import org.simbrain.workspace.Attribute
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.WorkspaceComponent
import smile.math.matrix.Matrix
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JSeparator

/**
 * A JMenu that appears relative to some object (an [AttributeContainer]) in
 * a workspace component. The menu allows you to create a coupling from that object
 * to any other attribute container of the same data type in Simbrain.
 *
 * @param sourceComponent The workspace component where this menu will be shown.
 * @param source The source object that will be the producer in whatever coupling is created using this menu.
 */
class CouplingMenu(
        private val sourceComponent: WorkspaceComponent,
        private val source: AttributeContainer
) : JMenu() {

    private val maxVisibleMenuItems = 36

    /**
     * Construct the menu.
     */
    init {
        text = "Create ${source.javaClass.simpleName} Coupling"
        removeAll()
        val sources = buildList {
            var current = listOf(source)
            while (current.isNotEmpty()) {
                addAll(current)
                current = current.flatMap { it.childrenContainers ?: emptyList() }
            }
        }
        with(sourceComponent.couplingManager) {
            sources.flatMap { it.visibleProducers }.forEach { createProducerSubmenu(it) }
            // TODO: possibly check to make sure that itemcount is not empty before adding this, though that is not
            //  usually the case and the affordance of the lone separator might not be so bad
            addSeparator()
            sources.flatMap { it.visibleConsumers }.forEach { createConsumerSubmenu(it) }
        }
    }

    /**
     * Provides a human-readable name of an attribute type.
     */
    private val Attribute.typeName: String
        get() = with(this.type as Class<*>) {
            when(this) {
                Double::class.java -> "number"
                DoubleArray::class.java -> "array"
                Matrix::class.java -> "matrix"
                String::class.java -> "text"
                else -> simpleName
            }
        }

    /**
     * Create a custom name for this menu besides the default "Create X coupling".
     *
     * @param name the custom name.
     */
    fun setCustomName(name: String) {
        text = name
    }

    /**
     * Create a submenu for a specific producer, that will "send" to a consumer
     * to create a coupling.
     *
     * @param producer the producer to make a menu for
     */
    private fun createProducerSubmenu(producer: Producer) {
        val workspace = sourceComponent.workspace
        sequence {
            workspace.componentList.forEach { wc ->
                with(sourceComponent.couplingManager) {
                    producer.compatiblesOfComponent(wc).forEach { consumer ->
                        CouplingMenuItem(workspace,
                                "${wc.name} / ${consumer.simpleDescription}",
                                producer,
                                consumer
                        ).let { yield(it) }
                    }
                }
            }
        }.createSubmenu("Send ${producer.simpleDescription} (${producer.typeName}) to")
    }

    private fun createConsumerSubmenu(consumer: org.simbrain.workspace.Consumer) {
        val workspace = sourceComponent.workspace
        sequence {
            workspace.componentList.forEach { wc ->
                with(workspace.couplingManager) {
                    consumer.compatiblesOfComponent(wc)
                }.forEach { product ->
                    CouplingMenuItem(workspace, "${wc.name}/${product.simpleDescription}", product, consumer)
                            .let { yield(it) }
                }
            }
        }.createSubmenu("Receive ${consumer.simpleDescription} (${consumer.typeName}) from")
    }

    private fun Sequence<CouplingMenuItem>.createSubmenu(description: String) {
        val submenu = JMenu(description)
        // TODO: "..." menu has no action
        if (firstOrNull() != null) {
            take(maxVisibleMenuItems).let { items ->
                items.map { it.create() }.forEach { submenu.add(it) }
                if (items.count() > 35) {
                    submenu.add(JSeparator())
                    submenu.add(JMenuItem("... and more items"))
                }
            }
        }
        add(submenu)
    }
}