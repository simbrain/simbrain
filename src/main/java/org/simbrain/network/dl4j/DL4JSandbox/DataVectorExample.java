package org.simbrain.network.dl4j.DL4JSandbox;

import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;

import java.util.Date;

/**
 * <b>Reference: https://github.com/SkymindIO/screencasts/tree/master/datavec_spark_transform</b>
 */
public class DataVectorExample {

    public static void main(String[] args) throws Exception {
        int numLinesToSkip = 0;
        String delimiter = ",";

        String baseDir = "C:\\Users\\kenfukuyama\\Documents\\GitHub\\simbrain\\src\\main\\java\\org\\simbrain\\network\\dl4j\\DL4JSandbox\\";
        String fileName = "reports.csv";
        String inputPath = baseDir + fileName;
        String timeStamp = String.valueOf(new Date().getTime());
        String outputPath = baseDir + "reports_processed_" + timeStamp;

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

    }

}
