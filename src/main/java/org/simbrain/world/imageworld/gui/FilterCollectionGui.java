package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.world.imageworld.dialogs.FilterDialog;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterCollection;
import org.simbrain.world.imageworld.filters.FilteredImageSource;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a toolbar for adding, deleting, and setting a current {@link Filter}
 * in a {@link FilterCollection}.
 */
public class FilterCollectionGui {

    private final FilterCollection filterCollection;

    private final JComboBox<Filter> filterComboBox = new JComboBox<>();

    public FilterCollectionGui(FilterCollection filterCollection) {
        this.filterCollection = filterCollection;
        filterCollection.getEvents().onFilterAdded(s-> updateComboBox());
        filterCollection.getEvents().onFilterRemoved(s-> updateComboBox());
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
            FilterDialog dialog = new FilterDialog(filterCollection);
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

            // If the filter is  a filtered image source, edit it too
            if (filter.getSource() instanceof FilteredImageSource) {
                FilteredImageSource imageSource = (FilteredImageSource) filter.getSource();
                AnnotatedPropertyEditor filterEditor = new AnnotatedPropertyEditor(imageSource);
                dialogPanel.add(filterEditor);
                filterEditorDialog.addClosingTask(() -> {
                    filterEditor.commitChanges();
                    // TODO
                    // Update the image based on the image source
                    // world.getImageAlbum().notifyResize();
                    // world.getImageAlbum().notifyImageUpdate();
                    filterComboBox.updateUI();
                });
            }

            // Delete filter
            JButton deleteFilter = new JButton("Delete Filter");
            deleteFilter.setToolTipText("Delete Filter");
            deleteFilter.addActionListener(e -> {
                // TODO: How to deal with this?
                // Can't remove the "Unfiltered" option
                if (filter.getName().equalsIgnoreCase("Unfiltered")) {
                    return;
                }
                int dialogResult = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete sensor panel \"" + filter.getName() + "\" ?", "Warning", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    int index = filterComboBox.getSelectedIndex();
                    filterCollection.setCurrentFilter(filterComboBox.getItemAt(index - 1));
                    filterComboBox.remove(index);
                    // TODO: Remove listener?
                    // ImageSource source = sensorMatrix.getSource();
                    // if (source instanceof FilteredImageSource) {
                    //     compositeSource.removeListener((FilteredImageSource) source);
                    // }
                    // TODO
                    // events.fireSensorMatrixRemoved(filterContainer);
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
