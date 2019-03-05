package org.simbrain.workspace.gui;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MultiCouplingMenu creates coupling menus for multiple model objects.
 * TODO: Needs a refactor
 */
public class MultiCouplingMenu {

    private JPopupMenu parentMenu;

    private List<CouplingMenu> menus;

    private WorkspaceComponent component;

    public MultiCouplingMenu(WorkspaceComponent component, JPopupMenu parentMenu, int maxMenus) {
        this.parentMenu = parentMenu;
        this.component = component;
        menus = new ArrayList<>();
        for (int i = 0; i < maxMenus; ++i) {
            menus.add(new CouplingMenu(component));
        }
    }

    public void setSourceModels() {
        for (CouplingMenu menu : menus) {
            parentMenu.remove(menu);
        }
        Iterator<CouplingMenu> menuIterator = menus.iterator();
        for (AttributeContainer source : component.getAttributeContainers()) {
            if (menuIterator.hasNext()) {
                CouplingMenu menu = menuIterator.next();
                menu.setSourceModel(source);
                parentMenu.add(menu);
            }
        }
    }
}
