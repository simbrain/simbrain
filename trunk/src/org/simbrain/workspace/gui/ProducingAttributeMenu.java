package org.simbrain.workspace.gui;

import java.util.Collection;

import javax.swing.JMenu;

import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.SingleAttributeProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * For coupling a menu-specified producing attribute to given consuming attribute.
 */
public class ProducingAttributeMenu extends JMenu implements WorkspaceListener {

	/** Reference to workspace. */
    Workspace workspace;
    
    /** The component to couple to. */
    ConsumingAttribute<?> targetConsumingAttribute;
    
    /**
     * @param menuName the name of the menu
     * @param workspace the workspace
     * @param sourceComponent the target consuming attribute.
     */
    public ProducingAttributeMenu(final String menuName, final Workspace workspace, final ConsumingAttribute<?> targetConsumingAttribute) {

		super(menuName);
		this.workspace = workspace;
		this.targetConsumingAttribute = targetConsumingAttribute;
		workspace.addListener(this);
		updateMenu();
	}

    /**
     * {@inheritDoc}
     */
	public boolean clearWorkspace() {
		return false;
	}

    /**
     * {@inheritDoc}
     */
	public void componentAdded(WorkspaceComponent component) {
		updateMenu();
	}

    /**
     * {@inheritDoc}
     */
	public void componentRemoved(WorkspaceComponent component) {
		updateMenu();
	}

    /**
     * {@inheritDoc}
     */
	public void workspaceCleared() {
		updateMenu();
	}

	/**
	 * Update the menu when components are added.
	 */
	private void updateMenu() {
		this.removeAll();
        for (WorkspaceComponent component : workspace.getComponentList()) {

            Collection<? extends Producer> producers = component.getProducers();

            if (producers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Producer producer : producers) {
                    if (producer instanceof SingleAttributeProducer) {
                        SingleCouplingMenuItem item =
                            new SingleCouplingMenuItem(workspace, 
                            		producer.getDescription(),
                            		producer.getProducingAttributes().get(0), 
                            		targetConsumingAttribute);
                        componentMenu.add(item);
                    } else {
                        JMenu producerItem = new JMenu(producer.getDescription());
                        for (ProducingAttribute<?> source : producer.getProducingAttributes()) {
                            SingleCouplingMenuItem item = 
                                new SingleCouplingMenuItem(
                                    workspace,source.getAttributeDescription(), source, targetConsumingAttribute);
                            producerItem.add(item);
                            componentMenu.add(producerItem);
                        }
                    }
                }
                this.add(componentMenu);
            }
        }
	}
}