package org.simbrain.network.dl4j.DL4JSandbox;

import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;

/**
 * <b>Reference: https://github.com/SkymindIO/screencasts/tree/master/datavec_spark_transform</b>
 */
public class DataVectorExample {

    public static void main(String[] args) throws Exception {

        // Changing the column names based on the data type.
        Schema inputDataSchema = new Schema.Builder()
                .addColumnsString("datetime","severity","location","county","state")
                .addColumnsDouble("lat","lon")
                .addColumnsString("comment")
                .addColumnCategorical("type","TOR","WIND","HAIL")
                .build();

        TransformProcess tp = new TransformProcess.Builder(inputDataSchema)
                .removeColumns("datetime","severity","location","county","state","comment")
                .categoricalToInteger("type")
                .build();

        int numActions = tp.getActionList().size();
        for (int i = 0; i < numActions; i++){
            System.out.println("\n\n===============================");
            System.out.println("--- Schema after step " + i +
                    " (" + tp.getActionList().get(i) + ")--" );
            System.out.println(tp.getSchemaAfterStep(i));
        }

        // TODO: External libraries are needed to complete the tutorial

    }

}
