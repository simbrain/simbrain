package org.simbrain.util.propertyeditor;

import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.SwingKt;
import org.simbrain.util.UserParameter;
import smile.math.matrix.Matrix;

import java.awt.*;
import java.util.Arrays;

/**
 * For testing ReflectivePropertyEditor.
 *
 * @author Jeff Yoshimi
 */
public class AnnotatedPropertyEditorTestObject implements EditableObject {

    // Not working
    @UserParameter(label = "Non editable item", editable = false)
    private String theLabel = "I'm a label";

    @UserParameter(label = "Color")
    Color theColor = Color.red;

    @UserParameter(label = "Boolean primitive")
    boolean theBool = true;

    // @UserParameter(label = "Boolean object")
    Boolean theBooleanObject = true;

    @UserParameter(label = "String")
    String theString = "testing";

    @UserParameter(label = "Int object")
    Integer theIntObject = 1;
    @UserParameter(label = "Double object")
    Double theDoubleObject = 1.1;

    @UserParameter(label = "Float object")
    Float theFloatObject = .123213f;

    // @UserParameter(label = "Long object")
    // Long theLongObject = 12321L;
    //
    // @UserParameter(label = "Short object")
    // Short theShortObject = 20;

    // Primitive number tests
    @UserParameter(label = "Int primitive", description = "The int", minimumValue = -10, maximumValue = 10, order = 1)
    int theInt = 1;

    @UserParameter(label = "Double primitive", description = "The int", minimumValue = -10, maximumValue = 10, order
            = 1)
    double theDouble = .9092342;

    @UserParameter(label = "Float primitive")
    float theFloat = 0;

    // @UserParameter(label = "The long", description = "The long", minimumValue = -10, maximumValue = 10, defaultValue = "5", order = 1)
    // long theLong = 20L;
    // short theShort = 20; // TODO: Figure out about shorts...

    @UserParameter(label = "Double Array")
    double[] doubleArray = new double[]{.1, .2, .3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    @UserParameter(label = "Matrix")
    Matrix matrix = Matrix.eye(3);

    @UserParameter(label = "Enum")

    private TestEnum theEnum = TestEnum.FOUR;


    @UserParameter(label = "Object type", isObjectType = true)

    private Layout theLayout = new LineLayout();

    public enum TestEnum {
        ONE("One"), TWO("Two"), THREE("Three"), FOUR("Four"), FIVE("Five");

        private String name;

        private TestEnum(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public String toString() {

        return "(Test Object) \n" +
                "The Enum: " + theEnum + "\n"
                + "The Color: " + theColor + "\n"
                + "The Boolean Object: "
                + theBooleanObject + "\n" + "The Boolean: "
                + theBool + "\n" + "The String: " + theString
                + "\n" + "The Integer Object: " + theIntObject
                + "\n" + "The Double Array: " + Arrays.toString(doubleArray)
                + "\n" + "The Double Object: " + theDoubleObject + "\n"
                + "\n" + "The Matrix: " + matrix + "\n"
                + "The Float Object: " + theFloatObject + "\n" + "The Long Object: " + "\n"
                + "The int: " + theInt + "\n" + "The double: " + theDouble + "\n"
                + "The float: " + theFloat + "\n"
                + "Update rule: " + theLayout + "\n";

    }


    public Color getTheColor() {
        return theColor;
    }

    public void setTheColor(Color theColor) {
        this.theColor = theColor;
    }

    public boolean isTheBool() {
        return theBool;
    }

    public void setTheBool(boolean theBool) {
        this.theBool = theBool;
    }

    public Boolean getTheBooleanObject() {
        return theBooleanObject;
    }

    public void setTheBooleanObject(Boolean theBooleanObject) {
        this.theBooleanObject = theBooleanObject;
    }

    public String getTheString() {
        return theString;
    }

    public void setTheString(String theString) {
        this.theString = theString;
    }

    public Integer getTheIntObject() {
        return theIntObject;
    }

    public void setTheIntObject(Integer theIntObject) {
        this.theIntObject = theIntObject;
    }

    public Double getTheDoubleObject() {
        return theDoubleObject;
    }

    public void setTheDoubleObject(Double theDoubleObject) {
        this.theDoubleObject = theDoubleObject;
    }

    public Float getTheFloatObject() {
        return theFloatObject;
    }

    public void setTheFloatObject(Float theFloatObject) {
        this.theFloatObject = theFloatObject;
    }

    //    /**
    //     * @return the theLongObject
    //     */
    //    public Long getTheLongObject() {
    //        return theLongObject;
    //    }
    //
    //    /**
    //     * @param theLongObject the theLongObject to set
    //     */
    //    public void setTheLongObject(Long theLongObject) {
    //        this.theLongObject = theLongObject;
    //    }
    //
    //    /**
    //     * @return the theShortObject
    //     */
    //    public Short getTheShortObject() {
    //        return theShortObject;
    //    }
    //
    //    /**
    //     * @param theShortObject the theShortObject to set
    //     */
    //    public void setTheShortObject(Short theShortObject) {
    //        this.theShortObject = theShortObject;
    //    }

    public int getTheInt() {
        return theInt;
    }

    public void setTheInt(int theInt) {
        this.theInt = theInt;
    }

    public double getTheDouble() {
        return theDouble;
    }

    public void setTheDouble(double theDouble) {
        this.theDouble = theDouble;
    }

    public float getTheFloat() {
        return theFloat;
    }

    public void setTheFloat(float theFloat) {
        this.theFloat = theFloat;
    }

    //    /**
    //     * @return the theLong
    //     */
    //    public long getTheLong() {
    //        return theLong;
    //    }
    //
    //    /**
    //     * @param theLong the theLong to set
    //     */
    //    public void setTheLong(long theLong) {
    //        this.theLong = theLong;
    //    }

    //    /**
    //     * @return the theShort
    //     */
    //    public short getTheShort() {
    //        return theShort;
    //    }
    //
    //    /**
    //     * @param theShort the theShort to set
    //     */
    //    public void setTheShort(short theShort) {
    //        this.theShort = theShort;
    //    }

    public double[] getDoubleArray() {
        return doubleArray;
    }

    public void setDoubleArray(double[] doubleArray) {
        this.doubleArray = doubleArray;
    }

    /**
     * Simple test routine.
     */
    public static void main(String[] args) {
        AnnotatedPropertyEditorTestObject testObject = new AnnotatedPropertyEditorTestObject();
        StandardDialog dialog = SwingKt.createDialog(testObject);
        dialog.addClosingTask(() -> {
            System.out.println(testObject);
        });
        SwingKt.display(dialog);
    }


}
