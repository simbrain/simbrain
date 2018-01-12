package org.simbrain.workspace.gui;

import org.simbrain.workspace.Workspace;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void setSourceModels(List<Object> sources) {
        for (CouplingMenu menu : menus) {
            parentMenu.remove(menu);
        }
        Iterator<CouplingMenu> menuIterator = menus.iterator();
        for (Object source : sources) {
            if (menuIterator.hasNext()) {
                CouplingMenu menu = menuIterator.next();
                menu.setSourceModel(source);
                parentMenu.add(menu);
            }
        }
    }
}
