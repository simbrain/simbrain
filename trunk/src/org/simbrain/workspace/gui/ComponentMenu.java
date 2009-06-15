package org.simbrain.workspace.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * For coupling a specific source component to a selected target component,
 * using one of the built in coupling methods (one to one, all to all, etc.).
 * 
 */
public class ComponentMenu extends JMenu implements WorkspaceListener {

	/** Reference to workspace. */
    Workspace workspace;
    
    /** The component to couple to. */
    WorkspaceComponent sourceComponent;
    
    /**
     * @param menuName the name of the menu
     * @param workspace the workspace
     * @param sourceComponent the source component
     */
    public ComponentMenu(final String menuName, final Workspace workspace,
            WorkspaceComponent sourceComponent) {
        super(menuName);
        this.workspace = workspace;
        this.sourceComponent = sourceComponent;
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
            final WorkspaceComponent targetComponent = component;
            JMenuItem componentMenuItem = new JMenuItem(targetComponent
                    .getName());
            componentMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    workspace.coupleOneToOne(sourceComponent
                            .getProducingAttributes(), targetComponent
                            .getConsumingAttributes());
                }
            });
            this.add(componentMenuItem);
        }
    }
}