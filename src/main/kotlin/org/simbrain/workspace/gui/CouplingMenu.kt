package org.simbrain.workspace.gui

import org.simbrain.util.createAction
import org.simbrain.util.displayInDialog
import org.simbrain.workspace.Attribute
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import smile.math.matrix.Matrix
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JSeparator
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import kotlin.math.max

/**
 * A JMenu that appears relative to some object (an [AttributeContainer]) in
 * a workspace component. The menu allows you to create a coupling from that object
 * to any other attribute container of the same data type in Simbrain.
 *
 * Populates lazily when the menu is opened, so it always reflects the current
 * workspace state without needing event-driven refresh.
 *
 * @param sourceComponent The workspace component where this menu will be shown.
 * @param source The source object that will be the producer in whatever coupling is created using this menu.
 */
class CouplingMenu(
        private val sourceComponent: WorkspaceComponent,
        private val source: AttributeContainer
) : JMenu() {

    private val maxVisibleMenuItems = 36

    init {
        text = "Create ${source.javaClass.simpleName} coupling"
        addMenuListener(object : MenuListener {
            override fun menuSelected(e: MenuEvent?) {
                populate()
            }
            override fun menuDeselected(e: MenuEvent?) {}
            override fun menuCanceled(e: MenuEvent?) {}
        })
    }

    /**
     * Create a custom name for this menu besides the default "Create X coupling".
     *
     * @param name the custom name.
     */
    fun setCustomName(name: String) {
        text = name
    }

    private fun populate() {
        removeAll()
        val sources = buildList {
            var current = listOf(source)
            while (current.isNotEmpty()) {
                addAll(current)
                current = current.flatMap { it.childrenContainers ?: emptyList() }
            }
        }
        with(sourceComponent.couplingManager) {
            sources.flatMap { it.producers.toList() }.forEach { createProducerSubmenu(it) }
            sources.flatMap { it.consumers.toList() }.forEach { createConsumerSubmenu(it) }
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
     * Create a submenu for a specific producer, that will "send" to a consumer
     * to create a coupling.
     *
     * @param producer the producer to make a menu for
     */
    private fun createProducerSubmenu(producer: Producer) {
        val workspace = sourceComponent.workspace
        val compatibleConsumers = workspace.componentList.flatMap { wc ->
            with(workspace.couplingManager) {
                producer.compatiblesOfComponent(wc).map { wc to it }
            }
        }
        val menuItems = compatibleConsumers.take(maxVisibleMenuItems).map { (wc, consumer) ->
            CouplingMenuItem(workspace,
                "${wc.name} / ${consumer.simpleDescription}",
                producer,
                consumer
            )
        }
        menuItems.createSubmenu(
            "Send ${producer.simpleDescription} (${producer.typeName}) to",
            max(0, compatibleConsumers.count() - maxVisibleMenuItems)
        )
    }

    private fun createConsumerSubmenu(consumer: org.simbrain.workspace.Consumer) {
        val workspace = sourceComponent.workspace
        val compatibleProducers = workspace.componentList.flatMap { wc ->
            with(workspace.couplingManager) {
                consumer.compatiblesOfComponent(wc).map { wc to it }
            }
        }
        val menuItems = compatibleProducers.take(maxVisibleMenuItems).map { (wc, producer) ->
            CouplingMenuItem(workspace, "${wc.name}/${producer.simpleDescription}", producer, consumer)
        }
        menuItems.createSubmenu(
            "Receive ${consumer.simpleDescription} (${consumer.typeName}) from",
            max(0, compatibleProducers.count() - maxVisibleMenuItems)
        )
    }

    private fun List<CouplingMenuItem>.createSubmenu(description: String, moreItems: Int) {
        if (isEmpty()) {
            add(JMenuItem(description).apply {
                toolTipText = "No compatible coupling found"
                isEnabled = false
            })
            return
        }
        val submenu = JMenu(description)
        map { it.create() }.forEach { submenu.add(it) }
        if (moreItems > 0) {
            submenu.add(JSeparator())
            submenu.add(createAction("... and $moreItems more ${if (moreItems == 1) "item" else "items"}") {
                DesktopCouplingManager(SimbrainDesktop).displayInDialog {  }
            })
        }
        add(submenu)
    }

    private fun Sequence<CouplingMenuItem>.createSubmenu(description: String) {
        val submenu = JMenu(description)
        // TODO: "..." menu has no action
        if (firstOrNull() != null) {
            take(maxVisibleMenuItems).let { items ->
                items.map { it.create() }.forEach { submenu.add(it) }
                if (items.count() > maxVisibleMenuItems-1) {
                    submenu.add(JSeparator())
                    submenu.add(JMenuItem("... and more items"))
                }
            }
        }
        add(submenu)
    }
}
