package org.simbrain.world.textworld.gui

import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.showMessageDialog
import org.simbrain.util.table.*
import org.simbrain.util.toMatrix
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.createTrainEmbeddingAction

class TokenEmbeddingDialog(val initialTokenEmbedding: TokenEmbedding, updateTokenEmbedding: (TokenEmbedding) -> Unit): StandardDialog() {

    var trainingDocument: String? = null

    val tablePanel = SimbrainTablePanel(initialTokenEmbedding.createTableModel(), useDefaultToolbarAndMenu = false).apply {
        addAction(
            table.importCSVAction(
                fixedColumns = false,
                skipImportOptions = true,
                defaultOptions = ImportExportOptions(includeRowNames = true),
                dataType = Double::class
            )
        )
        addAction(table.exportCsv(skipExportOptions = true, defaultOptions = ImportExportOptions(includeRowNames = true)))
        addSeparator()
        addAction(createTrainEmbeddingAction {
            (table.model as BasicDataFrame).data = it.createTableModel().data
            table.model.rowNames = it.tokens
            trainingDocument = it.trainingDocument
            table.model.fireTableStructureChanged()
        })
        addAction(createAction(
            name = "View Training Document",
            iconPath = "menu_icons/Open.png",
        ) {
            (trainingDocument ?: initialTokenEmbedding.trainingDocument)?.let { document ->
                showMessageDialog(document, "Training Document")
            }
        })
        addSeparator()
        addAction(table.createShowMatrixPlotAction())
        addAction(table.createOpenProjectionAction(useRowLabels = true))
    }.also { contentPane = it }

    init {
        title = "Token Embedding Viewer / Editor"
        addCommitTask {
            updateTokenEmbedding(
                TokenEmbedding(
                    tablePanel.table.model.rowNames as List<String>,
                    tablePanel.table.model.get2DDoubleArray().toMatrix(),
                    initialTokenEmbedding.embeddingType,
                    trainingDocument
                )
            )
        }
    }



}