package org.simbrain.workspace.gui

import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.WorkspaceComponent
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

    /**
     * Construct the menu.
     */
    init {
        text = "Create ${source.javaClass.simpleName} Coupling"
        removeAll()
        sourceComponent.couplingManager.run {
            source.visibleProducers.forEach { createProducerSubmenu(it) }
        }
        sourceComponent.couplingManager.run {
            source.visibleConsumers.forEach { createConsumerSubmenu(it) }
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
        sourceComponent.workspace.componentList.map { wc ->
            sourceComponent.couplingManager.run {
                producer.compatiblesOfComponent(wc)
            }.map { c ->
                CouplingMenuItem(sourceComponent.workspace, "${wc.name} / ${c.simpleDescription}", producer, c)
            }
        }.flatten().createSubmenu("${producer.simpleDescription} send to")
    }

    private fun createConsumerSubmenu(consumer: org.simbrain.workspace.Consumer) {
        sourceComponent.workspace.componentList.map { wc ->
            wc.couplingManager.run {
                consumer.compatiblesOfComponent(wc)
            }.map { p ->
                CouplingMenuItem(sourceComponent.workspace, "${wc.name}/${p.simpleDescription}", p, consumer)
            }
        }.flatten().createSubmenu("${consumer.simpleDescription} receive ${consumer.typeName} from")
    }

    private fun List<CouplingMenuItem>.createSubmenu(description: String) {

        val submenu = JMenu(description)

        // TODO: magic number
        // TODO: "..." menu has no action
        if (isNotEmpty()) {
            if (count() > 40) {
                this.take(35).map { it.create() }.forEach { submenu.add(it) }
                submenu.add(JSeparator())
                submenu.add(JMenuItem("... and ${count() - 35} more items"))
            } else {
                this.map { it.create() }.forEach { submenu.add(it) }
            }
            add(submenu)
        }
    }
}