package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * A list of components used by the combo box.
 * @author jyoshimi
 */
class ComponentList extends AbstractListModel implements ComboBoxModel {

    private static final long serialVersionUID = 1L;

    /** List of components within the workspace. */
    private List<WorkspaceComponent> componentList;

    /** Workspace component that is selected. */
    private WorkspaceComponent selected;

    /**
     * Constructs a list of components.
     * @param components to be set.
     */
    public ComponentList(final List<? extends WorkspaceComponent> components) {
        super();
        this.componentList = new ArrayList<WorkspaceComponent>(components);
    }

    /**
     * Returns the element at the specified location.
     * @param index of element.
     * @return object at location.
     */
    public WorkspaceComponent getElementAt(final int index) {
        return componentList.get(index);
    }

    /**
     * Returns the size of the component list.
     * @return size of component list
     */
    public int getSize() {
        return componentList.size();
    }

    /**
     * @return the componentList
     */
//    public List<Component> getComponentList() {
//        return componentList;
//    }

    /**
     * @param componentList the componentList to set
     */
//    public void setComponentList(final List<Component> componentList) {
//        this.componentList = componentList;
//    }

    /**
     * Returns the selected item.
     * @return selected item.
     */
    public WorkspaceComponent getSelectedItem() {
        return selected;
    }

    /**
     * Sets the selected item(s).
     * @param arg0 items to be set as selected.
     * //TODO: Check this stuff...
     */
    public void setSelectedItem(final Object arg0) {
        for (WorkspaceComponent component : componentList) {
            if (component == arg0) {
                selected = component;
            }
        }
    }
}