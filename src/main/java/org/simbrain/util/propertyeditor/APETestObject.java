package org.simbrain.util.propertyeditor;

import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.ReflectionUtilsKt;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.SwingKt;
import org.simbrain.util.UserParameter;
import smile.math.matrix.Matrix;

import java.awt.*;

/**
 * For testing ReflectivePropertyEditor.
 *
 * @author Jeff Yoshimi
 */
public class APETestObject implements EditableObject {

    // Not working
    @UserParameter(label = "Non editable item", displayOnly = true, order = 0)
    private String theLabel = "I'm a label";

    @UserParameter(label = "Color", order = 1)
    Color theColor = Color.red;

    @UserParameter(label = "Boolean primitive", order = 2)
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

    @UserParameter(label = "Long object")
    Long theLongObject = 12321L;

    @UserParameter(label = "Short object")
    Short theShortObject = 20;

    // Primitive number tests
    @UserParameter(label = "Int primitive", description = "The int", minimumValue = -10, maximumValue = 10, order = 1)
    int theInt = 1;

    @UserParameter(label = "Double primitive", description = "The int", minimumValue = -10, maximumValue = 10, order
            = 1)
    double theDouble = .9092342;

    @UserParameter(label = "Float primitive")
    float theFloat = 0;

    @UserParameter(label = "The long", description = "The long", minimumValue = 5L, maximumValue = 40L, increment = 5L)
    long theLong = 20L;

    @UserParameter(label = "The short", description = "The short")
    short theShort = 20; // TODO: Figure out about shorts...

    @UserParameter(label = "Double Array")
    double[] doubleArray = new double[]{.1, .2, .3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    // @UserParameter(label = "Double Array")
    double[] doubleArray2 = new double[]{.1, .2, .3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    // @UserParameter(label = "Matrix")
    Matrix matrix = Matrix.eye(3);

    @UserParameter(label = "Enum")

    private TestEnum theEnum = TestEnum.FOUR;

    @UserParameter(label = "Object type", showDetails = false)
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

    /**
     * Simple test routine.
     */
    public static void main(String[] args) {
        APETestObject testObject = new APETestObject();
        StandardDialog dialog = SwingKt.createDialog2(testObject);
        dialog.addClosingTask(() -> {
            System.out.println(ReflectionUtilsKt.allPropertiesToString(testObject, "\n"));
        });
        SwingKt.display(dialog);
    }


}
