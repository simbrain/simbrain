/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.visionworld.filter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Customizer;
import java.beans.Introspector;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.border.EmptyBorder;

import org.dishevelled.layout.LabelFieldLayout;

import org.simbrain.world.visionworld.Filter;

/**
 * Introspective filter dialog.
 */
public final class IntrospectiveFilterDialog
    extends JDialog {

    /** Filter types combo box. */
    private JComboBox filterTypes;

    /** Placeholder container. */
    private Container placeholder;

    /** Customizer. */
    private Customizer customizer;

    /** OK action. */
    private Action ok;

    /** Cancel action. */
    private Action cancel;

    /** Help action. */
    private Action help;


    /**
     * Introspective filter dialog.
     */
    public IntrospectiveFilterDialog() {
        super();
        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {

        filterTypes = new JComboBox(new FilterTypesComboBoxModel());

        filterTypes.addActionListener(new ActionListener()
            {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent e) {
                    updateFilter();
                }
            });
        placeholder = new JPanel();
        customizer = null;

        ok = new AbstractAction("OK")
            {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    // empty
                }
            };
        cancel = new AbstractAction("Cancel")
            {
                 /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    // empty
                }
           };
        help = new AbstractAction("Help")
            {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    // empty
                }
            };
        help.setEnabled(false);
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(new EmptyBorder(11, 11, 11, 11));
        contentPane.setLayout(new BorderLayout());
        contentPane.add("Center", createMainPanel());
        contentPane.add("South", createButtonPanel());
    }

    /**
     * Create and return the main panel.
     *
     * @return the main panel
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        LabelFieldLayout l = new LabelFieldLayout();
        panel.setLayout(l);
        panel.add(new JLabel("Filter:"), l.label());
        panel.add(filterTypes);
        l.nextLine();
        placeholder.setLayout(new BorderLayout());
        panel.add(placeholder, l.wideField());
        l.nextLine();
        panel.add(Box.createGlue(), l.finalSpacing());
        return panel;
    }

    /**
     * Create and return the button panel.
     *
     * @return the button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        panel.add(Box.createHorizontalGlue());
        JButton okButton = new JButton(ok);
        getRootPane().setDefaultButton(okButton);
        panel.add(okButton);
        panel.add(Box.createHorizontalStrut(11));
        JButton cancelButton = new JButton(cancel);
        panel.add(cancelButton);
        panel.add(Box.createHorizontalStrut(11));
        JButton helpButton = new JButton(help);
        panel.add(helpButton);

        Dimension d = new Dimension(Math.max(cancelButton.getPreferredSize().width, 70),
                                    cancelButton.getPreferredSize().height);
        okButton.setPreferredSize(d);
        cancelButton.setPreferredSize(d);
        helpButton.setPreferredSize(d);

        return panel;
    }

    /**
     * Update filter.
     */
    private void updateFilter() {
        try {
            FilterType filterType = (FilterType) filterTypes.getSelectedItem();
            Class c = filterType.getImplementationClass();
            Filter filter = (Filter) c.newInstance();
            BeanInfo beanInfo = Introspector.getBeanInfo(c);
            BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
            Class customizerClass = beanDescriptor.getCustomizerClass();
            customizer = (Customizer) customizerClass.newInstance();
            customizer.setObject(filter);
            JComponent customizerComponent = (JComponent) customizer;
            placeholder.removeAll();
            placeholder.add("Center", customizerComponent);
            placeholder.invalidate();
            placeholder.getParent().validate();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Filters combo box model;
     */
    private class FilterTypesComboBoxModel
        extends AbstractListModel
        implements ComboBoxModel {

        /** Selected item. */
        private Object selectedItem;


       /** {@inheritDoc} */
        public Object getSelectedItem() {
            return selectedItem;
        }

       /** {@inheritDoc} */
        public void setSelectedItem(final Object selectedItem) {
            this.selectedItem = selectedItem;
        }

        /** {@inheritDoc} */
        public Object getElementAt(final int index) {
            return FilterType.VALUES.get(index);
        }

        /** {@inheritDoc} */
        public int getSize() {
            return FilterType.VALUES.size();
        }
    }
}
