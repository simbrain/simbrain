package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.ImageWorldDesktopComponent;
import org.simbrain.world.imageworld.dialogs.CreateFilterDialog;
import org.simbrain.world.imageworld.filters.ImageTransformation;
import org.simbrain.world.imageworld.filters.ImageTransformationCollection;

import javax.swing.*;
import java.awt.*;

import static org.simbrain.util.SwingUtilsKt.getSwingDispatcher;

/**
 * Provides a toolbar for adding, deleting, and setting a current {@link ImageTransformation}
 * in a {@link ImageTransformationCollection}.
 */
public class FilterCollectionGui {

    private final ImageTransformationCollection imageTransformationCollection;

    private final JComboBox<ImageTransformation> filterComboBox = new JComboBox<>();

    private final ImageWorldDesktopComponent parent;

    public FilterCollectionGui(ImageWorldDesktopComponent parent, ImageTransformationCollection imageTransformationCollection) {
        this.parent = parent;
        this.imageTransformationCollection = imageTransformationCollection;
        imageTransformationCollection.getEvents().getImageTransformationAdded().on(getSwingDispatcher(), s -> updateComboBox());
        imageTransformationCollection.getEvents().getImageTransformationRemoved().on(getSwingDispatcher(), s -> updateComboBox());
        imageTransformationCollection.getEvents().getImageTransformationChanged().on(getSwingDispatcher(),
                (o, n) -> setComboBoxSelection(n));
        imageTransformationCollection.getEvents().getImageTransformationSelectionChanged().on(getSwingDispatcher(),
                imageTransformationCollection::setCurrentFilter);
    }

    public JToolBar getToolBar() {
        JToolBar filterToolbar = new JToolBar();

        filterToolbar.add(new JLabel("Filters:"));
        filterToolbar.add(filterComboBox);
        filterComboBox.setToolTipText("Which filter to view");
        updateComboBox();
        filterComboBox.setSelectedItem(imageTransformationCollection.getCurrentFilter());
        filterComboBox.setMaximumSize(new Dimension(200, 100));
        filterComboBox.addActionListener(evt -> {
            ImageTransformation selectedImageTransformation = (ImageTransformation) filterComboBox.getSelectedItem();
            if (selectedImageTransformation != null) {
                imageTransformationCollection.setCurrentFilter(selectedImageTransformation);
                imageTransformationCollection.getEvents().getImageTransformationSelectionChanged().fire(selectedImageTransformation);
            }
        });

        // Add Filter
        JButton addFilter = new JButton(ResourceManager.getSmallIcon("menu_icons/plus.png"));
        addFilter.setToolTipText("Add Filter");
        addFilter.addActionListener(evt -> {
            CreateFilterDialog dialog = new CreateFilterDialog(imageTransformationCollection);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
        filterToolbar.add(addFilter);

        // Editor Filter
        JButton editFilter = new JButton(ResourceManager.getSmallIcon("menu_icons/Tools.png"));
        editFilter.setToolTipText("Edit Filter");
        editFilter.addActionListener(evt -> {

            // Create a dialog to edit to filter
            StandardDialog filterEditorDialog = new StandardDialog();
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
            filterEditorDialog.setContentPane(dialogPanel);

            // Edit the top level filter, basically just a name
            ImageTransformation imageTransformation = imageTransformationCollection.getCurrentFilter();
            AnnotatedPropertyEditor topLevelFilterEditor = new AnnotatedPropertyEditor(imageTransformation);
            dialogPanel.add(topLevelFilterEditor);
            filterEditorDialog.addCommitTask(topLevelFilterEditor::commitChanges);

            // If the filter is a filtered image source, edit it too
            ImageSource imageSource = imageTransformation.getSource();
            filterEditorDialog.setTitle("Edit " + imageTransformation.getName());
            AnnotatedPropertyEditor filterEditor = new AnnotatedPropertyEditor((EditableObject) imageSource);
            dialogPanel.add(filterEditor);
            filterEditorDialog.addCommitTask(() -> {
                filterEditor.commitChanges();
                imageTransformation.applyFilter();
                filterComboBox.updateUI();
                parent.repaint();
            });

            // Delete filter
            JButton deleteFilter = new JButton("Delete Filter");
            deleteFilter.setToolTipText("Delete Filter");
            deleteFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
            deleteFilter.addActionListener(e -> {
                if (imageTransformation.getName().equalsIgnoreCase("Unfiltered")) {
                    JOptionPane.showMessageDialog(filterEditorDialog, "Can't remove unfiltered option");
                    return;
                }
                int dialogResult = JOptionPane.showConfirmDialog(filterEditorDialog,
                        "Are you sure you want to delete filter \"" + imageTransformation.getName() + "\" ?", "Warning",
                        JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    imageTransformationCollection.removeFilter(imageTransformation);
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

    private void setComboBoxSelection(ImageTransformation imageTransformation) {
        filterComboBox.setSelectedItem(imageTransformation);
    }

        /**
         * Reset the combo box for the filter panels.
         */
    private void updateComboBox() {
        filterComboBox.removeAllItems();
        ImageTransformation selectedImageTransformation = imageTransformationCollection.getCurrentFilter();
        for (ImageTransformation imageTransformation : imageTransformationCollection.getFilters()) {
            filterComboBox.addItem(imageTransformation);
            if (imageTransformation.equals(selectedImageTransformation)) {
                filterComboBox.setSelectedItem(imageTransformation);
            }
        }
    }

    public JComboBox<ImageTransformation> getFilterComboBox() {
        return filterComboBox;
    }
}
