package org.simbrain.world.dataworld;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;


/**
 * The <b>ButtonEditor</b> class provides and editor for buttons that allows you to change their names.
 *
 * @author RJB
 */
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor, KeyListener {
    private JTextField text = new JTextField();

    public ButtonEditor(DataWorld world) {
        text.addKeyListener(this);
    }

    public Object getCellEditorValue() {
        DataWorld.editButtons = false;

        return new JButton(text.getText());
    }

    public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
        text.setText(((JButton) arg1).getText());

        return text;
    }

    public void keyTyped(KeyEvent arg0) {
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
            this.fireEditingStopped();
        } else if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
            this.fireEditingCanceled();
        }
    }

    public void keyPressed(KeyEvent arg0) {
    }

    public void keyReleased(KeyEvent arg0) {
    }
}
