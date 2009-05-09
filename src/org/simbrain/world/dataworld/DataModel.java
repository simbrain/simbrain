package org.simbrain.world.dataworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Underlying data model.
 * 
 * @param <E>
 */
public class DataModel<E> {

    /** Default initial number of rows. */
    private static final int DEFAULT_ROW_COUNT = 5;

    /** Default initial number of columns. */
    private static final int DEFAULT_COLUMN_COUNT = 5;

    /** The data. */
    private List<List<E>> rowData = new ArrayList<List<E>>();

    /** Number of columns. */
    private int numColumns = DEFAULT_COLUMN_COUNT;

    /** Number of rows. */
    private int numRows = DEFAULT_ROW_COUNT;

    /** Iteration mode. */
    private boolean iterationMode = false;

    /** Use last column for iteration. */
    private boolean lastColumnBasedIteration = false;

    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = 0;

    /** Current row. */
    private int currentRow = 0;

    /** Listeners. */
    private List<Listener> listeners;

    // TODO: Document this.
    boolean initialized = false;



    /** The parent component of this model. */
    private DataWorldComponent parent;

    /**
     * Construct a dataworld model.
     * 
     * @param parent
     *            parent WorkspaceComponent.
     */
    DataModel(final DataWorldComponent parent) {
        this.parent = parent;
        for (int i = 0; i < numRows; i++) {
            rowData.add((List<E>) newRow(null));
        }
        init();
    }

    /**
     * Construct a dataworld model of a specified number of rows and columns.
     * 
     * @param parent
     *            parent WorkspaceComponent.
     * @param numColumns
     *            number of columns.
     * @param height
     *            number of rows.
     */
    DataModel(final DataWorldComponent parent, final int numRows,
            final int numColumns) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        this.parent = parent;

        for (int i = 0; i < numColumns; i++) {
            rowData.add((List<E>) newRow(null));
        }
        init();
    }

    /**
     * Initialize data model.
     */
    private void init() {


        listeners = new ArrayList<Listener>();
    }

    /**
     * Returns a properly initialized xstream object.
     * 
     * @return the XStream object
     */
    static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());

        xstream.omitField(DataModel.class, "listeners");
        xstream.omitField(DataModel.class, "parent");
        xstream.omitField(DataModel.class, "initialized");

        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        init();
        initialized = true;
        return this;
    }

    public DataWorldComponent getParent() {
        return parent;
    }

    void setParent(DataWorldComponent parent) {
        this.parent = parent;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private List<E> newRow(E value) {
        ArrayList<E> row = new ArrayList<E>();

        for (int i = 0; i < numColumns; i++) {
            row.add(value);
        }
        return row;
    }

    public void set(int row, int column, E value) {
        rowData.get(row).set(column, value);
        for (Listener listener : listeners)
            listener.itemChanged(row, column);
    }

    public E get(int row, int column) {
        return rowData.get(row).get(column);
    }

    public void set(int column, E value) {
        set(currentRow, column, value);
    }

    public E get(int column) {
        return get(currentRow, column);
    }

    /**
     * @return The lower bound.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound value.
     * 
     * @param lowerBound
     *            Value to set
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @return The upper bound value.
     */
    public int getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper bound value.
     * 
     * @param upperBound
     *            Value to set
     */
    public void setUpperBound(final int upperBound) {
        this.upperBound = upperBound;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public boolean isIterationMode() {
        return iterationMode;
    }

    public void setIterationMode(boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * @return Returns the columnIteration.
     */
    public boolean isLastColumnBasedIteration() {
        return lastColumnBasedIteration;
    }

    /**
     * @param columnIteration
     *            The columnIteration to set.
     */
    public void setLastColumnBasedIteration(final boolean columnIteration) {
        lastColumnBasedIteration = columnIteration;
    }

    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    public void addNewRow(E value) {
        numRows++;
        rowData.add(newRow(value));
        for (Listener listener : listeners)
            listener.rowAdded(numRows - 1);
        parent.setChangedSinceLastSave(true);
    }

    public void insertNewRow(final int at, E value) {
        numRows++;
        rowData.add(at, newRow(value));
        for (Listener listener : listeners)
            listener.rowAdded(at);
        parent.setChangedSinceLastSave(true);
    }

    public void addNewColumn(final E value) {
        numColumns++;
        for (List<E> row : rowData) {
            row.add(value);
        }
        for (Listener listener : listeners) {
            listener.columnAdded(numColumns - 1);
        }
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Adds rows or columns.
     * @param row number of rows to add.
     * @param col number of columns to add.
     * @param value to be added to the table.
     */
    public void addRowsColumns(final int row, final int col, final E value) {
        modifyRowsColumns(row + numRows, col + numColumns, value);
    }

    /**
     * Adds or removes rows and columns.
     * @param row Number of rows in table.
     * @param col Number of columns in table.
     * @param value to be added in the table.
     */
    public void modifyRowsColumns(final int row, final int col, final E value) {
        int currentColNum = numColumns;
        int currentRowNum = numRows;
        if (col > currentColNum) {
            for (int i = 0; i < col - currentColNum; ++i) {
                addNewColumn(value);
            }
        } else if (col < currentColNum) {
            for (int i = 0; i < currentColNum - col; ++i) {
                removeLastColumn();
            }
        }

        if (row > currentRowNum) {
            for (int i = 0; i < row - currentRowNum; ++i) {
                addNewRow(value);
            }
        } else if (row < currentRowNum) {
            for (int i = 0; i < currentRowNum - row; ++i) {
                removeLastRow();
            }
        }
    }

    public void insertNewColumn(int at, E value) {
        numColumns++;
        for (List<E> row : rowData) {
            row.add(at, value);
        }
        for (Listener listener : listeners) {
            listener.columnAdded(at);
        }
        parent.setChangedSinceLastSave(true);
    }

    public void removeLastRow() {
        numRows--;
        rowData.remove(numRows);
        for (Listener listener : listeners) {
            listener.rowRemoved(numRows);            
        }
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Remove a specified row.
     *
     * @param at index of row to remove.
     */
    public void removeRow(final int at) {
        numRows--;
        rowData.remove(at);
        for (Listener listener : listeners)
            listener.rowRemoved(at);
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Remove last column.
     */
    public void removeLastColumn() {
        numColumns--;
        for (List<E> row : rowData) {
            row.remove(numColumns);
        }
        for (Listener listener : listeners) {
            listener.columnRemoved(numColumns);
        }
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Remove column at specified index.
     * 
     * @param at index
     */
    public void removeColumn(final int at) {
        numColumns--;
        for (List<E> row : rowData) {
            row.remove(at);
        }
        for (Listener listener : listeners) {
            listener.columnRemoved(at);
        }
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Returns number of columns.
     *
     * @return number of columns.
     */
    public int getColumnCount() {
        return numColumns;
    }

    /**
     * Retursn number of rows.
     *
     * @return number of rows.
     */
    public int getRowCount() {
        return numRows;
    }

    /**
     * Initialize table with specified value.
     *
     * @param value value to initialize table with.
     */
    public void initValues(final E value) {
        if (!initialized) {
            fill(value);
            initialized = true;
        }
    }

    /**
     * Fills the table with the given value.
     */
    public void fill(final E value) {
        for (List<E> row : rowData) {
            Collections.fill(row, value);
        }
        parent.setChangedSinceLastSave(true);
    }

    /**
     * Update datamodel.
     */
    public void update() {
        if (isIterationMode()) {
            if (getCurrentRow() >= (getRowCount() - 1)) {
                setCurrentRow(0);
            } else {
                setCurrentRow(getCurrentRow() + 1);
            }
        }
    }

    public interface Listener {
        void dataChanged();

        void columnAdded(int column);

        void columnRemoved(int column);

        void rowAdded(int row);

        void rowRemoved(int row);

        void itemChanged(int row, int column);
    }

    
    // /** Persistable form of matrix data. */
    // private String[][] stringMatrixRepresentation;

    // /**
    // * {@inheritDoc}
    // */
    // public void preSaveInit() {
    // stringMatrixRepresentation = new
    // String[getModel().getRowCount()][getModel().getColumnCount()];
    //        
    // for (int i = 0; i < getModel().getRowCount(); i++) {
    // for (int j = 0; j < getModel().getColumnCount(); j++) {
    // stringMatrixRepresentation[i][j] = new String("" +
    // getModel().getValueAt(i, j));
    // }
    // }
    // }

    // /**
    // * {@inheritDoc}
    // */
    // public void postOpenInit() {
    // model = new DefaultTableModel(stringMatrixRepresentation.length,
    // stringMatrixRepresentation[0].length);
    // for (int i = 0; i < stringMatrixRepresentation.length; i++) {
    // for (int j = 0; j < stringMatrixRepresentation[0].length; j++) {
    // model.setValueAt(stringMatrixRepresentation[i][j], i, j);
    // }
    // }
    // }

    // /**
    // * Add a matrix of string data to the table, as doubles.
    // *
    // * @param data the matrix of string doubles to add
    // */
    // public void addMatrix(final String[][] data) {
    // removeAllRows();
    //        
    // int numCols = data[0].length;
    // model.addColumn("");
    //        
    // for (int i = 0; i < numCols; i++) {
    // model.addColumn(Integer.toString(i));
    // }
    //        
    // for (int i = 0; i < data.length; i++) {
    // Vector row = new Vector(data[i].length + 1);
    //            
    // for (int j = 0; j < data[i].length; j++) {
    // row.add(j , Double.valueOf((String) data[i][j]));
    // }
    //                
    // model.addRow(row);
    // }
    // }
}
