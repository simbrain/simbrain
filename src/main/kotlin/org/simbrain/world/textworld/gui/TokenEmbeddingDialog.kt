package org.simbrain.world.textworld.gui

import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.showMessageDialog
import org.simbrain.util.table.*
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.createExtractEmbeddingAction

class TokenEmbeddingDialog(val initialTokenEmbedding: TokenEmbedding, updateTokenEmbedding: (TokenEmbedding) -> Unit): StandardDialog() {

    var trainingDocument: String? = null
        set(value) {
            field = value
            viewWordEmbeddingSourceAction.isEnabled = value != null
        }

    var viewWordEmbeddingSourceAction = createAction(
        name = "View Word Embedding Source",
        description = "View word embedding source",
        iconPath = "menu_icons/DocumentInfo.png",
    ) {
        (trainingDocument ?: initialTokenEmbedding.trainingDocument)?.let { document ->
            showMessageDialog(document, "Source Document for Embedding")
        }
    }.apply {
        // Enabled / disabled by [#trainingDocument]
        isEnabled = false
    }

    var updatedTokenEmbedding: TokenEmbedding? = null

    val tablePanel = SimbrainTablePanel(initialTokenEmbedding.createTableModel(), useDefaultToolbarAndMenu = false).apply {
        addAction(createExtractEmbeddingAction {
            (table.model as BasicDataFrame).data = it.createTableModel().data
            table.model.rowNames = it.tokens
            trainingDocument = it.trainingDocument
            table.model.fireTableStructureChanged()
            updatedTokenEmbedding = it
        })
        addAction(viewWordEmbeddingSourceAction)
        addSeparator()
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
        addAction(table.createShowMatrixPlotAction())
        addAction(table.createOpenProjectionAction(useRowLabels = true))
    }.also { contentPane = it }

    init {
        title = "Word Embedding Editor"
        trainingDocument = initialTokenEmbedding.trainingDocument
        addCommitTask {
            updateTokenEmbedding(updatedTokenEmbedding ?: initialTokenEmbedding)
        }
    }



}