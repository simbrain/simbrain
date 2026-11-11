/**
 * The csv import dialog over a file with column and row names: once with the names recognized, and once importing
 * into a numeric table with the names left in the data, so the unreadable cells are marked.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.util.Utils
import org.simbrain.util.table.CsvImportDialog
import org.simbrain.util.table.ImportExportOptions
import org.simbrain.util.table.planCsvImport
import java.awt.Component
import java.io.File

private fun cars() = Utils.getStringMatrix(File("simulations/tables/mtcars.csv"))

class CsvImportDialogSnapshot : UiSnapshotDef {
    override val name = "csv_import_dialog"
    override fun build(): Component {
        val raw = cars()
        return CsvImportDialog("mtcars.csv", raw, makePlan = { planCsvImport(raw, it) })
    }
}

class CsvImportDialogUnreadableSnapshot : UiSnapshotDef {
    override val name = "csv_import_dialog_unreadable"
    override fun build(): Component {
        val raw = cars()
        return CsvImportDialog(
            "mtcars.csv",
            raw,
            initialOptions = ImportExportOptions(),
            makePlan = { planCsvImport(raw, it, dataType = Double::class) }
        )
    }
}

class CsvImportDialogMismatchSnapshot : UiSnapshotDef {
    override val name = "csv_import_dialog_mismatch"
    override fun build(): Component {
        val raw = cars()
        return CsvImportDialog(
            "mtcars.csv",
            raw,
            requiredColumnCount = 6,
            initialOptions = ImportExportOptions(includeColumnNames = true),
            makePlan = { planCsvImport(raw, it, dataType = Double::class) }
        )
    }
}
