package org.simbrain.util.propertyeditor;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * For testing ReflectivePropertyEditor.
 * 
 * @author Jeff Yoshimi
 */
public class TestObject {

    // Color test
    Color theColor = Color.red;

    // Boolean tests
    boolean theBool = true;
    Boolean theBooleanObject = true;

    // String Test
    String theString = "testing";

    // Object number tests (TODO Name)
    Integer theIntObject = 1;
    Double theDoubleObject = 1.1;
    Float theFloatObject = .123213f;
    Long theLongObject = 12321L;
    Short theShortObject = 20;

    // Primitive number tests
    int theInt = 1;
    double theDouble = .9092342;
    float theFloat = 0;
    long theLong = 20L;
    short theShort = 20; // TODO: Figure out about shorts...

    // Array test(s)
    double[] doubleArray = new double[]{.1,.2,.3,4};

    // Enum / Combo Box test
    private TestEnum theEnum = TestEnum.FOUR;
    ComboBoxable boxable;

    public enum TestEnum {
        ONE("One"), TWO("Two"), THREE("Three"), FOUR("Four"), FIVE("Five");

        private String name;

        private TestEnum(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    };

    public String toString() {

        return "(Test Object) \n" +

        "The Enum: " + theEnum + "\n" + "The Color: " + theColor + "\n"
                + "The Boolean Object: " + theBooleanObject + "\n"
                + "The Boolean: " + theBool + "\n" + "The String: " + theString
                + "\n" + "The Integer Object: " + theIntObject + "\n"
                + "The Double Array: " + Arrays.toString(doubleArray) + "\n"
                + "The Double Object: " + theDoubleObject + "\n"
                + "The Float Object: " + theFloatObject + "\n"
                + "The Long Object: " + theLongObject + "\n"
                + "The Short Object: " + theShortObject + "\n" + "The int: "
                + theInt + "\n" + "The double: " + theDouble + "\n"
                + "The float: " + theFloat + "\n" + "The long: " + theLong
                + "\n" + "The short: " + theShort + "\n";
    }

    /*
     * Wrap the enum object in a ComboBoxable wrapper
     * 
     * @return the boxable
     */
    public ComboBoxable getEnumeration() {
        return new ComboBoxable() {

            public Object getCurrentObject() {
                return theEnum;
            }

            public Object[] getObjects() {
                return TestEnum.values();
            }

        };
    }

    /**
     * Sets the enumeration based on a boxable object.
     * 
     * @param boxable
     *            the boxable to set
     */
    public void setEnumeration(ComboBoxable object) {
        theEnum = (TestEnum) object.getCurrentObject();
    }

    /**
     * Simple test routine.
     * 
     * @param args
     *            not used.
     */
    public static void main(String[] args) {

        // Test ReflectivePropertyEditor as a panel with ok / cancel
        TestObject testObject = new TestObject();
        ReflectivePropertyEditor editor = new ReflectivePropertyEditor(testObject);
        JDialog dialog = editor.getDialog();
        dialog.pack();
        dialog.setVisible(true);

        // Test ReflectivePropertyEditor as a panel
        JDialog dialog2 = new JDialog();
        TestObject testObject2 = new TestObject();
        TestObject testObject3 = new TestObject();
        final ReflectivePropertyEditor editor1 = new ReflectivePropertyEditor(
                testObject2);
        final ReflectivePropertyEditor editor2 = new ReflectivePropertyEditor(
                testObject3);
        JPanel panel = new JPanel();
        panel.add(editor1);
        panel.add(editor2);
        dialog2.setContentPane(panel);
        dialog2.pack();
        dialog2.setLocation(new Point(500, 0));
        dialog2.setVisible(true);
        dialog2.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg) {
                editor1.commit();
                System.out.println("-----");
                editor2.commit();
            }
        });

    }

    /**
     * @return the theColor
     */
    public Color getTheColor() {
        return theColor;
    }

    /**
     * @param theColor
     *            the theColor to set
     */
    public void setTheColor(Color theColor) {
        this.theColor = theColor;
    }

    /**
     * @return the theBool
     */
    public boolean isTheBool() {
        return theBool;
    }

    /**
     * @param theBool
     *            the theBool to set
     */
    public void setTheBool(boolean theBool) {
        this.theBool = theBool;
    }

    /**
     * @return the theBooleanObject
     */
    public Boolean getTheBooleanObject() {
        return theBooleanObject;
    }

    /**
     * @param theBooleanObject
     *            the theBooleanObject to set
     */
    public void setTheBooleanObject(Boolean theBooleanObject) {
        this.theBooleanObject = theBooleanObject;
    }

    /**
     * @return the theString
     */
    public String getTheString() {
        return theString;
    }

    /**
     * @param theString
     *            the theString to set
     */
    public void setTheString(String theString) {
        this.theString = theString;
    }

    /**
     * @return the theIntObject
     */
    public Integer getTheIntObject() {
        return theIntObject;
    }

    /**
     * @param theIntObject
     *            the theIntObject to set
     */
    public void setTheIntObject(Integer theIntObject) {
        this.theIntObject = theIntObject;
    }

    /**
     * @return the theDoubleObject
     */
    public Double getTheDoubleObject() {
        return theDoubleObject;
    }

    /**
     * @param theDoubleObject
     *            the theDoubleObject to set
     */
    public void setTheDoubleObject(Double theDoubleObject) {
        this.theDoubleObject = theDoubleObject;
    }

    /**
     * @return the theFloatObject
     */
    public Float getTheFloatObject() {
        return theFloatObject;
    }

    /**
     * @param theFloatObject
     *            the theFloatObject to set
     */
    public void setTheFloatObject(Float theFloatObject) {
        this.theFloatObject = theFloatObject;
    }

    /**
     * @return the theLongObject
     */
    public Long getTheLongObject() {
        return theLongObject;
    }

    /**
     * @param theLongObject
     *            the theLongObject to set
     */
    public void setTheLongObject(Long theLongObject) {
        this.theLongObject = theLongObject;
    }

    /**
     * @return the theShortObject
     */
    public Short getTheShortObject() {
        return theShortObject;
    }

    /**
     * @param theShortObject
     *            the theShortObject to set
     */
    public void setTheShortObject(Short theShortObject) {
        this.theShortObject = theShortObject;
    }

    /**
     * @return the theInt
     */
    public int getTheInt() {
        return theInt;
    }

    /**
     * @param theInt
     *            the theInt to set
     */
    public void setTheInt(int theInt) {
        this.theInt = theInt;
    }

    /**
     * @return the theDouble
     */
    public double getTheDouble() {
        return theDouble;
    }

    /**
     * @param theDouble
     *            the theDouble to set
     */
    public void setTheDouble(double theDouble) {
        this.theDouble = theDouble;
    }

    /**
     * @return the theFloat
     */
    public float getTheFloat() {
        return theFloat;
    }

    /**
     * @param theFloat
     *            the theFloat to set
     */
    public void setTheFloat(float theFloat) {
        this.theFloat = theFloat;
    }

    /**
     * @return the theLong
     */
    public long getTheLong() {
        return theLong;
    }

    /**
     * @param theLong
     *            the theLong to set
     */
    public void setTheLong(long theLong) {
        this.theLong = theLong;
    }

    /**
     * @return the theShort
     */
    public short getTheShort() {
        return theShort;
    }

    /**
     * @param theShort
     *            the theShort to set
     */
    public void setTheShort(short theShort) {
        this.theShort = theShort;
    }

    /**
     * @return the doubleArray
     */
    public double[] getDoubleArray() {
        return doubleArray;
    }

    /**
     * @param doubleArray the doubleArray to set
     */
    public void setDoubleArray(double[] doubleArray) {
        this.doubleArray = doubleArray;
    }
}
