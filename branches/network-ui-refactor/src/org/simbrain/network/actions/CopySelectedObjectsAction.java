
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.Clipboard;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

import edu.umd.cs.piccolo.PNode;

/**
 * Copy selected neurons action.
 */
public final class CopySelectedObjectsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new copy selected neurons action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public CopySelectedObjectsAction(final NetworkPanel networkPanel) {
        super("Copy");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Copy.gif"));

        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        Clipboard.clear();
        networkPanel.setNumberOfPastes(0);

        ArrayList copiedObjects = new ArrayList();

        for (Iterator i = networkPanel.getSelection().iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            if (Clipboard.canBeCopied(node, networkPanel)) {
                copiedObjects.add(node);
            }
        }

        Clipboard.add(copiedObjects);
    }

}