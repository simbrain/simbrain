package org.simbrain.workspace.gui;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;

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

    public MultiCouplingMenu(Workspace workspace, JPopupMenu parentMenu, int numMenus) {
        this.parentMenu = parentMenu;
        menus = new ArrayList<>();
        for (int i = 0; i < numMenus; ++i) {
            menus.add(new CouplingMenu(workspace));
        }
    }

    public void setSourceModels(List<AttributeContainer> sources) {
        for (CouplingMenu menu : menus) {
            parentMenu.remove(menu);
        }
        Iterator<CouplingMenu> menuIterator = menus.iterator();
        for (AttributeContainer source : sources) {
            if (menuIterator.hasNext()) {
                CouplingMenu menu = menuIterator.next();
                menu.setSourceModel(source);
                parentMenu.add(menu);
            }
        }
    }
}
