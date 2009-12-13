package org.simbrain.util.propertyeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.simbrain.util.LabelledItemPanel;

/**
 * ReflectivePropertyEditor.
 *
 * @author Jeff Yoshimi
 *
 * TODO:
 *  - Generalize array stuff
 *  - Put arrays in scrollers
 *  - Better documenation / readme
 *  - Custom naming, ordering for fields?
 *  - Deal with redundant get and is case (then two fields are added).
 */
public class ReflectivePropertyEditor extends JPanel {

    /** Getting rid of warning. */
    private static final long serialVersionUID = 1L;

    /**
     * The object whose public fields will be edited using the
     * ReflectivePropertyEditor
     */
    private Object toEdit;

    /** Main item panel. */
    private LabelledItemPanel itemPanel = new LabelledItemPanel();

    /** List of methods that can be edited. Used when committing changes. */
    private List<Method> editableMethods = new ArrayList<Method>();

    /** Map from property names for Color objects to selected selectedColor. */
    private HashMap<String, Color> selectedColor = new HashMap<String, Color>();

    /** List of methods to exclude from the dialog, listed by name. */
    private String[] excludeList = new String[]{};

    /** If true, use superclass methods. */
    private boolean useSuperclass = true;

    /**
     * Associate property names with JComponents that are used to set those
     * values.
     */
    HashMap<String, JComponent> componentMap = new HashMap<String, JComponent>();

    /**
     * Construct a property editor panel only. It's up to the user to embed it
     * in a dialog or other GUI element
     *
     * @param toEdit
     *            the object to edit
     */
    public ReflectivePropertyEditor(final Object toEdit) {
        this.toEdit = toEdit;
        this.add(itemPanel);
        initPanel();
    }

    /**
     * Use this constructor to make an editor, adjust its settings, and then
     * initialize it with an object.
     */
    public ReflectivePropertyEditor() {
    }

    /**
     * Set object to edit. For use with no argument constructor.
     *
     * @param edit the object to edit
     */
    public void setObject(final Object edit) {
        if (this.toEdit == null) {
            this.toEdit = edit;
            this.add(itemPanel);
            initPanel();
        } // else throw exception?
    }

    /**
     * Returns an ok / cancel dialog for this dialog.
     *
     * @return parentDialog parent dialog
     */
    public JDialog getDialog() {

        final JDialog ret = new JDialog();
        ret.setLayout(new BorderLayout());
        ret.add("Center", itemPanel);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                commit();
                ret.dispose();
            }
        });
        bottomPanel.add(okButton);
        ret.getRootPane().setDefaultButton(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ret.dispose();
            }
        });
        bottomPanel.add(cancelButton);
        ret.add("South", bottomPanel);
        return ret;
    }

    /**
     * Set up the main panel.
     */
    private void initPanel() {
        for (final Method method : toEdit.getClass().getMethods()) {

            boolean inSuperClass = (method.getDeclaringClass() != toEdit.getClass());
            // If useSuperClass is false, then if the method is in the base class,
            //  this flag should be false.
            boolean skipMethod = !useSuperclass & inSuperClass;

            if (!skipMethod && !inExcludeList(method)) {
                boolean isGetter = (method.getName().startsWith("get") || method
                        .getName().startsWith("is"));
                if (isGetter) {
                    if (hasMatchingSetter(method)) {

                        final String propertyName = getPropertyName(method);
                        final String formattedPropertyName = formatString(propertyName);

                        // Combo Boxes must be wrapped in a ComboBoxable
                        if (method.getReturnType() == ComboBoxWrapper.class) {
                            ComboBoxWrapper boxable = (ComboBoxWrapper) getGetterValue(method);
                            JComboBox comboBox = new JComboBox(boxable
                                    .getObjects());
                            componentMap.put(propertyName, comboBox);
                            comboBox
                                    .setSelectedItem(boxable.getCurrentObject());
                            itemPanel.addItem(formattedPropertyName, comboBox);
                        }

                        // Colors
                        else if (method.getReturnType() == Color.class) {
                            final JButton colorButton = new JButton("Set Color");
                            final JPanel colorIndicator = new JPanel();
                            final JPanel colorPanel = new JPanel();
                            colorPanel.add(colorButton);
                            colorIndicator.setSize(20, 20);
                            final Color currentColor = (Color) getGetterValue(method);
                            selectedColor.put(propertyName, currentColor);
                            colorIndicator.setBorder(BorderFactory
                                    .createLineBorder(Color.black));
                            colorIndicator.setBackground(currentColor);
                            colorPanel.add(colorIndicator);
                            colorButton.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent arg0) {
                                    // TODO: Alignment is wrong
                                    Color theColor = JColorChooser.showDialog(
                                            itemPanel, "Choose Color",
                                            selectedColor.get(propertyName));
                                    colorIndicator.setBackground(theColor);
                                    selectedColor.remove(propertyName);
                                    selectedColor.put(propertyName, theColor);
                                }
                            });
                            itemPanel
                                    .addItem(formattedPropertyName, colorPanel);
                        } else if (method.getReturnType() == String.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        }

                        else if (method.getReturnType() == double[].class) {
                            JTable table = new JTable();
                            table.setGridColor(Color.gray);
                            double[] value = (double[]) getGetterValue(method);
                            DefaultTableModel model = new DefaultTableModel(value.length, 1);
                            for(int i = 0; i < value.length; i++) {
                                model.setValueAt(value[i], i, 0);
                            }
                            table.setModel(model);
                            table.setBorder(BorderFactory.createLineBorder(Color.black));
                            componentMap.put(propertyName, table);
                            itemPanel.addItem(formattedPropertyName, table);
//                            JList jList = new JList();
//                            DefaultListModel model = new DefaultListModel();
//                            double[] value = (double[]) getGetterValue(method);
//                            model.setSize(value.length);
//                            for(int i = 0; i < value.length; i++) {
//                                model.set(i, value[i]);
//                            }
//                            jList.setModel(model);
//                            jList.setBorder(BorderFactory.createLineBorder(Color.black));
//                            componentMap.put(propertyName, jList);
//                            itemPanel.addItem(formattedPropertyName, jList);
                        }

                        // Booleans > Checkboxes
                        else if (method.getReturnType() == Boolean.class) {
                            JCheckBox checkBox = new JCheckBox();
                            checkBox.setSelected(Boolean.class.cast(
                                    getGetterValue(method)).booleanValue());
                            componentMap.put(propertyName, checkBox);
                            itemPanel.addItem(formattedPropertyName, checkBox);
                        } else if (method.getReturnType() == boolean.class) {
                            JCheckBox checkBox = new JCheckBox();
                            checkBox
                                    .setSelected((Boolean) getGetterValue(method));
                            componentMap.put(propertyName, checkBox);
                            itemPanel.addItem(formattedPropertyName, checkBox);
                        }

                        // Number wrappers (TODO: Find name of this)
                        else if (method.getReturnType() == Integer.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        } else if (method.getReturnType() == Double.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        } else if (method.getReturnType() == Float.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        } else if (method.getReturnType() == Long.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        } else if (method.getReturnType() == Short.class) {
                            JTextField theTextField = getInitializedTextField(method);
                            componentMap.put(propertyName, theTextField);
                            itemPanel.addItem(formattedPropertyName,
                                    theTextField);
                        }

                        // Primitive number types
                        else if (method.getReturnType() == int.class) {
                            JTextField textField = new JTextField();
                            componentMap.put(propertyName, textField);
                            textField.setText((getGetterValue(method))
                                    .toString());
                            itemPanel.addItem(formattedPropertyName, textField);
                        } else if (method.getReturnType() == double.class) {
                            JTextField textField = new JTextField();
                            componentMap.put(propertyName, textField);
                            textField.setText(((Double) getGetterValue(method))
                                    .toString());
                            itemPanel.addItem(formattedPropertyName, textField);
                        } else if (method.getReturnType() == float.class) {
                            JTextField textField = new JTextField();
                            componentMap.put(propertyName, textField);
                            textField.setText((getGetterValue(method))
                                    .toString());
                            itemPanel.addItem(formattedPropertyName, textField);
                        } else if (method.getReturnType() == long.class) {
                            JTextField textField = new JTextField();
                            componentMap.put(propertyName, textField);
                            textField.setText((getGetterValue(method))
                                    .toString());
                            itemPanel.addItem(formattedPropertyName, textField);
                        } else if (method.getReturnType() == short.class) {
                            JTextField textField = new JTextField();
                            componentMap.put(propertyName, textField);
                            textField.setText((getGetterValue(method))
                                    .toString());
                            itemPanel.addItem(formattedPropertyName, textField);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check to see if the method is in the exclude list.
     *
     * @param method the method to check
     * @return true if the method is to be excluded, false otherwse
     */
    private boolean inExcludeList(Method method) {
        for (String name : excludeList) {
            if (getPropertyName(method).equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Commit the changes in the panel to the object being edited.
     */
    public void commit() {
        for (Method m : editableMethods) {
            String propertyName = getPropertyName(m);
            Class<?> argumentType = m.getParameterTypes()[0];

            // Combo Box
            if (argumentType == ComboBoxWrapper.class) {
                final Object selectedObject = ((JComboBox) componentMap
                        .get(propertyName)).getSelectedItem();
                ComboBoxWrapper boxedObject = new ComboBoxWrapper() {
                    public Object getCurrentObject() {
                        return selectedObject;
                    }

                    public Object[] getObjects() {
                        return null;
                    }
                };
                setSetterValue(m, ComboBoxWrapper.class, boxedObject);
            }

            // Double array
            else if (argumentType == double[].class) {
                JTable table = (JTable) componentMap.get(propertyName);
                double[] data = new double[table.getRowCount()];
                for (int i = 0; i < table.getRowCount(); i++) {
                    if(table.getValueAt(i, 0).getClass() == String.class) {
                        data[i] = Double.parseDouble((String)table.getValueAt(i, 0));                        
                    } else {
                        data[i] = (Double) table.getValueAt(i, 0);
                    }
                }
                setSetterValue(m, argumentType, data);
            }

            // Colors
            else if (argumentType == Color.class) {
                // This one's easy because the colors are already stored in a
                // map
                setSetterValue(m, argumentType, selectedColor.get(propertyName));
            }

            // Booleans
            else if (argumentType == Boolean.class) {
                Boolean newVal = ((JCheckBox) componentMap.get(propertyName))
                        .isSelected();
                setSetterValue(m, argumentType, newVal);
            } else if (argumentType == boolean.class) {
                boolean newVal = ((JCheckBox) componentMap.get(propertyName))
                        .isSelected();
                setSetterValue(m, Boolean.class, new Boolean(newVal));
            }

            // Strings
            else if (argumentType == String.class) {
                String newVal = ((JTextField) componentMap.get(propertyName))
                        .getText();
                setSetterValue(m, argumentType, newVal);
            }

            // Object numbers (TODO)
            else if (argumentType == Integer.class) {
                Integer newVal = Integer.parseInt(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, argumentType, newVal);
            } else if (argumentType == Double.class) {
                Double newVal = Double.parseDouble(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, argumentType, newVal);
            } else if (argumentType == Float.class) {
                Float newVal = Float.parseFloat(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, argumentType, newVal);
            } else if (argumentType == Long.class) {
                Long newVal = Long.parseLong(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, argumentType, newVal);
            } else if (argumentType == Short.class) {
                Short newVal = Short.parseShort(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, argumentType, newVal);
            }

            // Primitive Numbers
            else if (argumentType == int.class) {
                Integer newVal = Integer.parseInt(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, Integer.class, new Integer(newVal));
            } else if (argumentType == double.class) {
                Double newVal = Double.parseDouble(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, Double.class, new Double(newVal));
            } else if (argumentType == float.class) {
                Float newVal = Float.parseFloat(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, Float.class, new Float(newVal));
            } else if (argumentType == long.class) {
                Long newVal = Long.parseLong(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, Long.class, new Long(newVal));
            } else if (argumentType == short.class) {
                Short newVal = Short.parseShort(((JTextField) componentMap
                        .get(propertyName)).getText());
                setSetterValue(m, Short.class, new Short(newVal));
            }
        }

        // TODO: Remove once unit tests are int)
        System.out.println(toEdit);
    }

    /**
     * Return a property name from a Method object, which is here taken to be
     * the substring after "is", "get", or "set".
     *
     * @param method
     *            the method to retrieve the property name from.
     * @return the property name
     */
    private String getPropertyName(Method method) {
        return method.getName().startsWith("is") ? method.getName()
                .substring(2) : method.getName().substring(3);
    }

    /**
     * Returns a text field initialized to the corresponding getter's value.
     * 
     * @param m
     * @return
     */
    private JTextField getInitializedTextField(Method m) {
        JTextField textField = new JTextField();
        textField.setText(m.getReturnType().cast(getGetterValue(m)).toString());
        return textField;
    }

    /**
     * Get the return value of getter method.
     * 
     * @param theMethod
     * @return
     */
    private Object getGetterValue(final Method theMethod) {
        try {
            return theMethod.invoke(toEdit, (Object[]) null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * True if getter has a matching setter whose one parameter matches the
     * getter's return type.
     */
    private boolean hasMatchingSetter(Method getter) {
        String propertyName = getPropertyName(getter);
        for (Method possibleSetter : toEdit.getClass().getMethods()) {
            if (possibleSetter.getName().equalsIgnoreCase("set" + propertyName)) {
                // Assume just one parameter for a setter
                // System.out.println(possibleSetter.getParameterTypes()[0].getName()
                // + " =? " + getter.getReturnType().getName());
                if (possibleSetter.getParameterTypes()[0].getName() == getter
                        .getReturnType().getName()) {
                    // At this point set the list of setter methods for use in
                    // commit
                    editableMethods.add(possibleSetter);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Format a string by inserting spaces and capitalizing. This makes the item
     * labels more appealing.
     * 
     * Example: convert "heightInPixels" to "Height In Pixels."
     * 
     * @param toFormat
     *            the String to format, retrieved from a getter or setter.
     * @return the formatted string.
     */
    private String formatString(String toFormat) {
        String ret = "";
        char[] chars = toFormat.toCharArray();
        ret += Character.toString(chars[0]).toUpperCase();
        for (int i = 1; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                ret += " " + Character.toString(chars[i]).toUpperCase();
            } else {
                ret += Character.toString(chars[i]);
            }
        }
        return ret;
    }

    /**
     * Set the value of a setter method to a specified value
     * 
     * @param theMethod
     * @param theVal
     */
    private void setSetterValue(final Method theMethod, Class<?> type,
            Object theVal) {
        try {
            theMethod.invoke(toEdit, type.cast(theVal));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the useSuperclass
     */
    public boolean isUseSuperclass() {
        return useSuperclass;
    }

    /**
     * @param useSuperclass the useSuperclass to set
     */
    public void setUseSuperclass(boolean useSuperclass) {
        this.useSuperclass = useSuperclass;
    }

    /**
     * @param strings the excludeList to set
     */
    public void setExcludeList(String[] strings) {
        this.excludeList = strings;
    }

}
