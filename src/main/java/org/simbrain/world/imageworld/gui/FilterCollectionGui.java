package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.dialogs.CreateFilterDialog;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterCollection;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a toolbar for adding, deleting, and setting a current {@link Filter}
 * in a {@link FilterCollection}.
 */
public class FilterCollectionGui {

    private final FilterCollection filterCollection;

    private final JComboBox<Filter> filterComboBox = new JComboBox<>();

    private final ImageWorldDesktopComponent parent;

    public FilterCollectionGui(ImageWorldDesktopComponent parent, FilterCollection filterCollection) {
        this.parent = parent;
        this.filterCollection = filterCollection;
        filterCollection.getEvents().onFilterAdded(s -> updateComboBox());
        filterCollection.getEvents().onFilterRemoved(s -> updateComboBox());
        filterCollection.getEvents().onFilterChanged(this::updateComboBox);
    }

    public JToolBar getToolBar() {
        JToolBar filterToolbar = new JToolBar();

        filterToolbar.add(new JLabel("Filters:"));
        filterToolbar.add(filterComboBox);
        filterComboBox.setToolTipText("Which filter to view");
        updateComboBox();
        filterComboBox.setSelectedItem(filterCollection.getCurrentFilter());
        filterComboBox.setMaximumSize(new Dimension(200, 100));
        filterComboBox.addActionListener(evt -> {
            Filter selectedFilter = (Filter) filterComboBox.getSelectedItem();
            if (selectedFilter != null) {
                filterCollection.setCurrentFilter(selectedFilter);
            }
        });

        // Add Filter
        JButton addFilter = new JButton(ResourceManager.getImageIcon("menu_icons/plus.png"));
        addFilter.setToolTipText("Add Filter");
        addFilter.addActionListener(evt -> {
            CreateFilterDialog dialog = new CreateFilterDialog(filterCollection);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
        filterToolbar.add(addFilter);

        // Editor Filter
        JButton editFilter = new JButton(ResourceManager.getImageIcon("menu_icons/Prefs.png"));
        editFilter.setToolTipText("Edit Filter");
        editFilter.addActionListener(evt -> {

            // Create a dialog to edit to filter
            StandardDialog filterEditorDialog = new StandardDialog();
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            filterEditorDialog.setContentPane(dialogPanel);

            // Edit the top level filter, basically just a name
            Filter filter = filterCollection.getCurrentFilter();
            AnnotatedPropertyEditor topLevelFilterEditor = new AnnotatedPropertyEditor(filter);
            dialogPanel.add(topLevelFilterEditor);
            filterEditorDialog.addClosingTask(topLevelFilterEditor::commitChanges);

            // If the filter is a filtered image source, edit it too
            ImageSource imageSource = filter.getSource();
            AnnotatedPropertyEditor filterEditor = new AnnotatedPropertyEditor(imageSource);
            dialogPanel.add(filterEditor);
            filterEditorDialog.addClosingTask(() -> {
                filterEditor.commitChanges();
                filter.refreshFilter();
                filterComboBox.updateUI();
                parent.repaint();
            });

            // Delete filter
            JButton deleteFilter = new JButton("Delete Filter");
            deleteFilter.setToolTipText("Delete Filter");
            deleteFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
            deleteFilter.addActionListener(e -> {
                if (filter.getName().equalsIgnoreCase("Unfiltered")) {
                    JOptionPane.showMessageDialog(filterEditorDialog, "Can't remove unfiltered option");
                    return;
                }
                int dialogResult = JOptionPane.showConfirmDialog(filterEditorDialog,
                        "Are you sure you want to delete filter panel \"" + filter.getName() + "\" ?", "Warning",
                        JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    filterCollection.removeFilter(filter);
                    updateComboBox();
                }
                filterEditorDialog.setVisible(false);
            });
            dialogPanel.add(deleteFilter);

            filterEditorDialog.pack();
            filterEditorDialog.setLocationRelativeTo(null);
            filterEditorDialog.setVisible(true);

        });
        filterToolbar.add(editFilter);
        return filterToolbar;
    }

    /**
     * Reset the combo box for the filter panels.
     */
    private void updateComboBox() {
        filterComboBox.removeAllItems();
        Filter selectedFilter = filterCollection.getCurrentFilter();
        for (Filter filter : filterCollection.getFilters()) {
            filterComboBox.addItem(filter);
            if (filter.equals(selectedFilter)) {
                filterComboBox.setSelectedItem(filter);
            }
        }
    }

    public JComboBox<Filter> getFilterComboBox() {
        return filterComboBox;
    }
}
