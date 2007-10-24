package org.simbrain.world.dataworld;

import java.util.ArrayList;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;

public class DataModel<E> {

    /** Iteration mode. */
    private boolean iterationMode = false;
    
    /** Use last column for iteration. */
    private boolean lastColumnBasedIteration = false;
    
    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = 0;

    private int currentRow;

    /** List of consumers. */
    private ArrayList<Consumer> consumers = new ArrayList<Consumer>();

    /** List of producers. */
    private ArrayList<Producer> producers = new ArrayList<Producer>();

    public void set(int row, int column, E value) {
        // TODO
    }

    public E get(int row, int column) {
        // TODO
        return null;
    }

    public void set(int column, E value) {

    }

    public E get(int column) {
        // TODO
        return null;
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
     * @param lowerBound Value to set
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
     * @param upperBound Value to set
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
     * @param columnIteration The columnIteration to set.
     */
    public void setLastColumnBasedIteration(final boolean columnIteration) {
        lastColumnBasedIteration = columnIteration;
    }

    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }
    
    public void addNewRow()
    {
        // TODO
    }
    
    public void insertNewRow(int at)
    {
        // TODO
    }
    
    public void addNewColumn()
    {
        // TODO
    }
    
    public void insertNewColumn(int at)
    {
        // TODO
    }

    public void removeLastRow()
    {
        // TODO
    }
    
    public void removeRow(int at)
    {
        // TODO
    }
    
    public void removeLastColumn()
    {
        // TODO
    }
    
    public void removeColumn(int at)
    {
        // TODO
    }
    
    public int getColumnCount()
    {
        // TODO
        return 0;
    }
    
    public int getRowCount()
    {
        // TODO
        return 0;
    }
    
    /**
     * Fills the table with zeros.
     */
    public void zeroFill() {
        // TODO
    }

    /**
     * Same as zerofill, but only fills the last column.
     */
    public void zeroFillNew() {
//        for (int j = 0; j < model.getRowCount(); j++) {
//            model.setValueAt(new Double(0), j, model.getColumnCount() - 1);
//        }
    }
    
    public void randomize()
    {
        // TODO 
    }
    
    public void update()
    {
        // TODO
    }
    
    // /** Table Model Delegate. */
    // private DefaultTableModel model;
    //
    // /** Default initial number of rows. */
    // private static final int DEFAULT_ROW_COUNT = 5;
    //
    // /** Default initial number of columns. */
    // private static final int DEFAULT_COLUMN_COUNT = 6;
    //

    //
    // /** Persistable form of matrix data. */
    // private String[][] stringMatrixRepresentation;
    //
    //
    // /**
    // * Create a new table model for the specified data world.
    // *
    // * @param dataWorld data world
    // */
    // public TableModel() {
    //
    // model = new DefaultTableModel();
    //
    // for (int i = 1; i < DEFAULT_COLUMN_COUNT; i++) {
    // model.addColumn(Integer.toString(i));
    // }
    //
    // for (int i = 1; i < DEFAULT_ROW_COUNT; i++) {
    // model.addRow(newRow());
    // }
    // initConsumersAndProducers();
    // }
    //
    // /**
    // * {@inheritDoc}
    // */
    // public void preSaveInit() {
    // stringMatrixRepresentation = new
    // String[getModel().getRowCount()][getModel().getColumnCount()];
    // for (int i = 0; i < getModel().getRowCount(); i++) {
    // for (int j = 0; j < getModel().getColumnCount(); j++) {
    // stringMatrixRepresentation[i][j] = new String("" +
    // getModel().getValueAt(i, j));
    // }
    // }
    // }
    //
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
    // Vector<String> columnNames = new Vector<String>();
    // for (int i = 0; i < stringMatrixRepresentation[0].length; i++) {
    // columnNames.add("" + i + 1);
    // }
    // model.setColumnIdentifiers(columnNames);
    // initConsumersAndProducers();
    // }
    //
    // /**
    // * Initializes all consumers and producers.
    // */
    // public void initConsumersAndProducers() {
    // consumers = new ArrayList<Consumer>();
    // producers = new ArrayList<Producer>();
    // couplingList = new ArrayList<Coupling>();
    // for (int i = 0; i < this.getModel().getColumnCount(); i++) {
    // consumers.add(new ConsumingColumn(this, i));
    // producers.add(new ProducingColumn(this, i));
    // }
    // }
    //
    // /**
    // * Return a new vector to be used in addRow.
    // *
    // * @return a new vector to be used in addRow
    // */
    // public Vector newRow() {
    // Vector row = new Vector(model.getColumnCount());
    // for (int i = 0; i < model.getColumnCount(); i++) {
    // row.add(i, new Double(0));
    // }
    // return row;
    // }
    //
    // /**
    // * Fills the table with zeros.
    // */
    // public void zeroFill() {
    // for (int i = 1; i < model.getColumnCount(); i++) {
    // for (int j = 0; j < model.getRowCount(); j++) {
    // model.setValueAt(new Double(0), j, i);
    // }
    // }
    // }
    //
    // /**
    // * Same as zerofill, but only fills the last column.
    // */
    // public void zeroFillNew() {
    // for (int j = 0; j < model.getRowCount(); j++) {
    // model.setValueAt(new Double(0), j, model.getColumnCount() - 1);
    // }
    // }
    //
    // /**
    // * Clear the table.
    // */
    // public void removeAllRows() {
    // for (int i = model.getRowCount(); i > 0; --i) {
    // model.removeRow(i - 1);
    // }
    // }
    //
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
    // for (int j = 0; j < data[i].length; j++) {
    // row.add(j , Double.valueOf((String) data[i][j]));
    // }
    //
    // model.addRow(row);
    // }
    // }
    //
    // /**
    // * Overrides superclass to provide coupling support.
    // *
    // * @param column passed to superclass.
    // */
    // public void addColumn(final String column) {
    // model.addColumn(column);
    // consumers.add(new ConsumingColumn(this, model.getColumnCount()));
    // producers.add(new ProducingColumn(this, model.getColumnCount()));
    // model.fireTableStructureChanged();
    // model.fireTableDataChanged();
    // }
    //
    // /**
    // * Remove a column at the specified point.
    // *
    // * @param index column to remove
    // */
    // public void removeColumn(final int index) {
    // consumers.remove(index);
    // producers.remove(index);
    // for (Iterator i = model.getDataVector().iterator(); i.hasNext(); ) {
    // Vector row = (Vector) i.next();
    // row.remove(index);
    // }
    // zeroFill();
    // model.fireTableStructureChanged();
    // model.fireTableDataChanged();
    // }
    //
    // /**
    // * @param currentRow the currentRow to set
    // */
    // public void setCurrentRow(int currentRow) {
    // this.currentRow = currentRow;
    // }
    //
    // /**
    // * Randomizes the values.
    // *
    // */
    // public void randomize() {
    //
    // for (int i = 0; i < model.getColumnCount(); i++) {
    // for (int j = 0; j < model.getRowCount(); j++) {
    // model.setValueAt(randomInteger(), j, i);
    // }
    // }
    // }
    //
    // /**
    // * @return A random integer.
    // */
    // public Double randomInteger() {
    // if (upperBound >= lowerBound) {
    // double drand = Math.random();
    // drand = (drand * (upperBound - lowerBound)) + lowerBound;
    //
    // Double element = new Double(drand);
    //
    // return element;
    // }
    //
    // return new Double(0);
    // }
    //
    //
    // /**
    // * @return the tableModel
    // */
    // public DefaultTableModel getModel() {
    // return model;
    // }
}
