package org.simbrain.util.table

import smile.data.DataFrame

/**
 * Wrapper for Smile DataFrame
 */
class DataFrameWrapper(val df : DataFrame): SimbrainDataModel() {

    // Can add secondary constructors to convert what we pass in

    override fun getRowCount(): Int {
        return df.nrows()
    }

    override fun getColumnCount(): Int {
        return df.ncols()
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return df[rowIndex, columnIndex]
    }

}

