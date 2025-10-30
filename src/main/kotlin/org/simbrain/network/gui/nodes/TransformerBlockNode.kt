package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.TransformerBlock
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.piccolo.*
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import smile.math.matrix.Matrix
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.*


class TransformerBlockNode(networkPanel: NetworkPanel, val transformerBlock: TransformerBlock) :
    ArrayLayerNode(networkPanel, transformerBlock) {

    val selfAttentionImage = PImage().apply {
        mainNode.addChild(this)
    }

    val selfAttentionLabel = mainNode.addLabel("Attention Scores (${transformerBlock.selfAttention.shapeString})")

    val sequenceGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val qSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val qSequenceLabel = sequenceGroup.addLabel("q (${transformerBlock.qStack.shapeString})")

    val kSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val kSequenceLabel = sequenceGroup.addLabel("k (${transformerBlock.qStack.shapeString})")

    val vSequenceImage = PImage().apply {
        sequenceGroup.addChild(this)
    }

    val vSequenceLabel = sequenceGroup.addLabel("v (${transformerBlock.qStack.shapeString})")

    val matrixGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val qMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val qMatrixLabel = matrixGroup.addLabel("Q (${transformerBlock.Q.shapeString})")

    val kMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val kMatrixLabel = matrixGroup.addLabel("K (${transformerBlock.K.shapeString})")

    val vMatrixImage = PImage().apply {
        matrixGroup.addChild(this)
    }

    val vMatrixLabel = matrixGroup.addLabel("V (${transformerBlock.V.shapeString})")

    val feedForwardGroup = PNode().apply {
        mainNode.addChild(this)
    }

    val feedForwardInputImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardInputLabel = feedForwardGroup.addLabel("FF Input (${transformerBlock.feedForwardInput.shapeString})")

    val feedForwardHiddenImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardHiddenLabel = feedForwardGroup.addLabel("FF Hidden (${transformerBlock.feedForwardHidden.shapeString})")

    val feedForwardOutputImage = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardOutputLabel = feedForwardGroup.addLabel("FF Output (${transformerBlock.activations.shapeString})")

    // Track current arrows so they can be removed when updating
    private val currentArrows = mutableListOf<Arrow>()

    val feedForwardW1Image = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardW1Label = feedForwardGroup.addLabel("Input -> Hidden (${transformerBlock.W1.shapeString})")

    val feedForwardW2Image = PImage().apply {
        feedForwardGroup.addChild(this)
    }

    val feedForwardW2Label = feedForwardGroup.addLabel("Hidden -> Output (${transformerBlock.W2.shapeString})")


    val blockInputLabel = mainNode.addLabel("Input")
    val blockOutputLabel = mainNode.addLabel("Output")



    override val margin = 10.0

    /**
     * Get the shape string for a matrix, accounting for the weight matrix preference.
     * If it's a weight matrix and the preference is set to source-target format (not target-source),
     * return the transposed dimensions.
     */
    private fun Matrix.getShapeString(isWeightMatrix: Boolean = false): String {
        return if (isWeightMatrix && !NetworkPreferences.weightMatrixTargetSource) {
            "${ncol()} x ${nrow()}"
        } else {
            "${nrow()} x ${ncol()}"
        }
    }

    /**
     * Create a new neuron array node.
     *
     * @param np Reference to NetworkPanel
     * @param na reference to model neuron array
     */
    init {

        val events = transformerBlock.events



        events.updated.on {
            events.updateGraphics.fire()
        }

        events.updateGraphics.on(Dispatchers.Swing) {
            updateImages()
        }

        updateImages()
        updateBorder()

        // call once to make sure all the actions are registered
        contextMenu

    }

    private fun renderImage(
        image: PImage,
        matrix: Matrix,
        width: kotlin.Double,
        height: kotlin.Double,
        strokeWidth: kotlin.Float = 1f,
        isWeightMatrix: Boolean = false,
        offset: (PImage) -> Unit = { }
    ) {
        image.removeAllChildren()
        val img = matrix.flatten().toSimbrainColorImage(matrix.ncol(), matrix.nrow())
            .let { if (isWeightMatrix && !NetworkPreferences.weightMatrixTargetSource) it.transposed() else it }
        image.image = img
        image.setBounds(
            image.x, image.y,
            width, height
        )
        offset(image)
        image.addBorder(strokeWidth)
        updateTextLabels()
    }

    private fun updateImages() {


        renderImage(selfAttentionImage, transformerBlock.selfAttention, 100.0, 100.0)

        if (indexOfChild(matrixGroup) == -1) {
            mainNode.addChild(matrixGroup)
        }
        renderImage(qMatrixImage, transformerBlock.Q, 60.0, 60.0, strokeWidth = 2f, isWeightMatrix = true)
        renderImage(kMatrixImage, transformerBlock.K, 60.0, 60.0, strokeWidth = 2f, isWeightMatrix = true) {
            it.anchorCenterLeft().alignTo(qMatrixImage.anchorCenterRight(), offsetX = 20.0)
        }
        renderImage(vMatrixImage, transformerBlock.V, 60.0, 60.0, strokeWidth = 2f, isWeightMatrix = true) {
            it.anchorCenterLeft().alignTo(kMatrixImage.anchorCenterRight(), offsetX = 60.0)
        }
        matrixGroup.anchorRelative(0.3, 0.0).alignTo(selfAttentionImage.anchorCenterBottom(), offsetY = 100.0)

        if (indexOfChild(sequenceGroup) == -1) {
            mainNode.addChild(sequenceGroup)
        }
        renderImage(qSequenceImage, transformerBlock.qStack, 40.0, 40.0) {
            it.anchorCenterBottom().alignTo(qMatrixImage.anchorCenterTop(), offsetY = -20.0)
        }
        renderImage(kSequenceImage, transformerBlock.kStack, 40.0, 40.0) {
            it.anchorCenterBottom().alignTo(kMatrixImage.anchorCenterTop(), offsetY = -20.0)
        }
        renderImage(vSequenceImage, transformerBlock.vStack, 40.0, 40.0) {
            it.anchorCenterBottom().alignTo(vMatrixImage.anchorCenterTop(), offsetY = -20.0)
        }

        if (indexOfChild(feedForwardGroup) == -1) {
            mainNode.addChild(feedForwardGroup)
        }
        renderImage(feedForwardInputImage, transformerBlock.feedForwardInput, 40.0, 40.0)
        renderImage(feedForwardHiddenImage, transformerBlock.feedForwardHidden, 40.0, 40.0) {
            it.anchorCenterBottom().alignTo(feedForwardInputImage.anchorCenterTop(), offsetY = -16.0)
        }
        renderImage(feedForwardOutputImage, transformerBlock.activations, 40.0, 40.0) {
            it.anchorCenterBottom().alignTo(feedForwardHiddenImage.anchorCenterTop(), offsetY = -16.0)
        }
        renderImage(feedForwardW1Image, transformerBlock.W1, 40.0, 40.0, strokeWidth = 2f, isWeightMatrix = true) {
            it.anchorCenterLeft().alignTo(feedForwardInputImage.anchorTopRight(), offsetX = 32.0, offsetY = -8.0)
        }
        renderImage(feedForwardW2Image, transformerBlock.W2, 40.0, 40.0, strokeWidth = 2f, isWeightMatrix = true) {
            it.anchorCenterLeft().alignTo(feedForwardHiddenImage.anchorTopRight(), offsetX = 32.0, offsetY = -8.0)
        }
        feedForwardGroup.anchorCenterBottom().alignTo(selfAttentionImage.anchorTopRight(), offsetY = -20.0)

        // Remove existing arrows before adding new ones
        currentArrows.forEach { arrow ->
            arrow.removeFromParent()
        }
        currentArrows.clear()

        val arrows = mutableListOf<Arrow>()

        val busX = selfAttentionImage.anchorCenterLeft().withOffset(offsetX = -40.0)
        
        arrows.addAll(listOf(

            createArrowPath()
                .startAt(qMatrixImage.anchorCenterTop())
                .lineTo(qMatrixImage.anchorCenterTop().withOffset(offsetY = -70.0))
                .lineToX(selfAttentionImage.anchorCenterBottom())
                .lineTo(selfAttentionImage.anchorCenterBottom())
                .build(),
            createArrowPath()
                .startAt(kMatrixImage.anchorCenterTop())
                .lineTo(kMatrixImage.anchorCenterTop().withOffset(offsetY = -70.0))
                .lineToX(selfAttentionImage.anchorCenterBottom())
                .lineTo(selfAttentionImage.anchorCenterBottom())
                .buildWithoutArrowhead(),
            createArrowPath()
                .startAt(vMatrixImage.anchorCenterTop())
                .lineToXY(
                    vMatrixImage.anchorCenterTop(),
                    selfAttentionImage.anchorCenterTop().withOffset(offsetY = -10.0)
                )
                .lineToX(busX)
                .buildWithJunction(),
            createArrowPath()
                .startAt(selfAttentionImage.anchorCenterRight())
                .lineToX(vMatrixImage.anchorCenterTop().withOffset(offsetX = -40.0))
                .lineToY(selfAttentionImage.anchorCenterTop().withOffset(offsetY = -10.0))
                .build(),
            createArrowPath()
                .startAt(feedForwardInputImage.anchorRelative(1.0, 0.25))
                .lineToX(feedForwardW1Image.anchorTopLeft())
                .build(),
            createArrowPath()
                .startAtXY(feedForwardHiddenImage.anchorCenterRight(), feedForwardHiddenImage.anchorRelative(1.0, 0.25))
                .lineToX(feedForwardW1Image.anchorTopLeft())
                .build(),
            createArrowPath()
                .startAt(feedForwardW1Image.anchorTopLeft())
                .lineToX(feedForwardHiddenImage.anchorRelative(1.0, 0.75))
                .build(),
            createArrowPath()
                .startAtXY(feedForwardW2Image.anchorTopLeft(), feedForwardOutputImage.anchorRelative(1.0, 0.75))
                .lineToX(feedForwardOutputImage.anchorCenterRight())
                .build(),

            // vertical line from inputs
            createArrowPath()
                .startAtXY(this@TransformerBlockNode.anchorCenterBottom(), qMatrixImage.anchorCenterBottom().withOffset(offsetY = 50.0))
                .lineToY(qMatrixImage.anchorCenterBottom().withOffset(offsetY = 30.0))
                .buildWithoutArrowhead(),

            // First skip connection: inputs -> bus (around attention)
            // This represents the residual connection around the self-attention mechanism
            createArrowPath()
                .startAtXY(this@TransformerBlockNode.anchorCenterBottom(), qMatrixImage.anchorCenterBottom().withOffset(offsetY = 40.0))
                .lineToX(busX)
                .buildWithoutArrowhead(),
            // Second skip connection: feedForwardInput -> bus -> feedForwardOutput (around MLP)
            // This represents the residual connection around the feed-forward network
            createArrowPath()
                .startAtXY(busX, feedForwardInputImage.anchorCenterBottom().withOffset(offsetY = 10.0))
                .lineToX(feedForwardInputImage.anchorCenterBottom())
                .lineToY(feedForwardInputImage.anchorCenterBottom())
                .build(),
            createArrowPath()
                .startAt(feedForwardOutputImage.anchorCenterTop())
                .lineToY(feedForwardOutputImage.anchorCenterTop().withOffset(offsetY = -10.0))
                .lineToX(busX)
                .buildWithJunction(),

            createArrowPath()
                .startAt(qMatrixImage.anchorCenterBottom().withOffset(offsetY = 30.0))
                .lineToX(vMatrixImage.anchorCenterBottom())
                .buildWithoutArrowhead(),
            createArrowPath()
                .startAt(qMatrixImage.anchorCenterBottom().withOffset(offsetY = 30.0))
                .lineToY(qMatrixImage.anchorCenterBottom())
                .build(),
            createArrowPath()
                .startAt(kMatrixImage.anchorCenterBottom().withOffset(offsetY = 30.0))
                .lineToY(kMatrixImage.anchorCenterBottom())
                .build(),
            createArrowPath()
                .startAt(vMatrixImage.anchorCenterBottom().withOffset(offsetY = 30.0))
                .lineToY(vMatrixImage.anchorCenterBottom())
                .build(),

            // main bus
            createArrowPath()
                .startAtXY(busX, qMatrixImage.anchorCenterBottom().withOffset(offsetY = 40.0))
                .lineToY(feedForwardOutputImage.anchorCenterTop().withOffset(offsetY = -30.0))
                .lineToX(this@TransformerBlockNode.anchorCenterTop())
                .lineToRelative(0.0, -20.0)
                .build(),
        ))

        arrows.forEach {
            mainNode.addChild(it)
            it.lowerToBottom()
            currentArrows.add(it)
        }

        updateTextLabels()
    }

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()

            // Edit Menu
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.add(networkPanel.networkActions.duplicateAction)
            contextMenu.addSeparator()
            val selectedBlocks = networkPanel.selectionManager.filterSelectedModels<TransformerBlock>()
            val count = selectedBlocks.size
            val editArray = networkPanel.createAction(
                name = "Edit $count transformer ${if (count == 1) "block" else "blocks"}..."
            ) {
                propertyDialog?.display()
            }
            contextMenu.add(editArray)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.networkActions.connectSelectedModels)
            contextMenu.addSeparator()

            val applyInputs: Action = networkPanel.networkActions.createTestInputPanelAction(transformerBlock)
            contextMenu.add(applyInputs)
            val addActivationToInput = networkPanel.networkActions.createAddActivationToInputAction(transformerBlock)
            contextMenu.add(addActivationToInput)
            contextMenu.addSeparator()

            // Randomize Action
            val randomizeAction = networkPanel.networkActions.randomizeObjectsAction

            contextMenu.add(randomizeAction)

            contextMenu.addSeparator()
            val editComponents: Action = object : AbstractAction("Edit components...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog = StandardDialog()
                    val arrayData = MatrixDataFrame(transformerBlock.selfAttention)
                    dialog.contentPane = JTabbedPane().apply {
                        add("Self Attention", SimbrainTablePanel(arrayData))
                        add("K", SimbrainTablePanel(MatrixDataFrame(transformerBlock.K)))
                        add("Q", SimbrainTablePanel(MatrixDataFrame(transformerBlock.Q)))
                        add("V", SimbrainTablePanel(MatrixDataFrame(transformerBlock.V)))
                        add("k", SimbrainTablePanel(MatrixDataFrame(transformerBlock.kStack)))
                        add("q", SimbrainTablePanel(MatrixDataFrame(transformerBlock.qStack)))
                        add("v", SimbrainTablePanel(MatrixDataFrame(transformerBlock.vStack)))
                    }
                    dialog.addCommitTask {
                        with(networkPanel.network) {
                            transformerBlock.update()
                        }
                    }
                    dialog.pack()
                    dialog.setLocationRelativeTo(null)
                    dialog.isVisible = true
                }
            }
            contextMenu.add(editComponents)

            contextMenu.addSeparator()
            contextMenu.add(networkPanel.alignMenu)
            contextMenu.add(networkPanel.spaceMenu)

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(transformerBlock)
            contextMenu.add(couplingMenu)
            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        val editingObjects = networkPanel.filterSelectedModelByClass<TransformerBlock>()
        if (editingObjects.isEmpty()) return null
        return editingObjects.createEditorDialog(
            titleName = if (editingObjects.size == 1)
                "Edit ${editingObjects.first().displayName}"
            else
                "Edit ${editingObjects.size} Transformer Blocks",
        )
    }

    override val propertyDialog: StandardDialog?
        get() = createEditDialog()

    override val model: TransformerBlock
        get() = transformerBlock

    /**
     * Update the text labels for components.
     */
    fun updateTextLabels() {
        fun PText.centerBelow(image: PImage, padding: kotlin.Double = 1.0) {
            anchorCenterTop().alignTo(image.anchorCenterBottom(), offsetY = padding)
        }

        // Update label text for sequences (not weight matrices)
        qSequenceLabel.text = "q (${transformerBlock.qStack.getShapeString()})"
        qSequenceLabel.centerBelow(qSequenceImage)
        
        kSequenceLabel.text = "k (${transformerBlock.kStack.getShapeString()})"
        kSequenceLabel.centerBelow(kSequenceImage)
        
        vSequenceLabel.text = "v (${transformerBlock.vStack.getShapeString()})"
        vSequenceLabel.centerBelow(vSequenceImage)

        // Update label text for weight matrices with preference consideration
        qMatrixLabel.text = "Q (${transformerBlock.Q.getShapeString(isWeightMatrix = true)})"
        qMatrixLabel.centerBelow(qMatrixImage)
        
        kMatrixLabel.text = "K (${transformerBlock.K.getShapeString(isWeightMatrix = true)})"
        kMatrixLabel.centerBelow(kMatrixImage)
        
        vMatrixLabel.text = "V (${transformerBlock.V.getShapeString(isWeightMatrix = true)})"
        vMatrixLabel.centerBelow(vMatrixImage)

        selfAttentionLabel.text = "Attention Scores (${transformerBlock.selfAttention.getShapeString()})"
        selfAttentionLabel.centerBelow(selfAttentionImage)

        feedForwardInputLabel.text = "FF Input (${transformerBlock.feedForwardInput.getShapeString()})"
        feedForwardInputLabel.centerBelow(feedForwardInputImage)
        
        feedForwardHiddenLabel.text = "FF Hidden (${transformerBlock.feedForwardHidden.getShapeString()})"
        feedForwardHiddenLabel.centerBelow(feedForwardHiddenImage)
        
        feedForwardOutputLabel.text = "FF Output (${transformerBlock.activations.getShapeString()})"
        feedForwardOutputLabel.centerBelow(feedForwardOutputImage)
        
        feedForwardW1Label.text = "Input -> Hidden (${transformerBlock.W1.getShapeString(isWeightMatrix = true)})"
        feedForwardW1Label.centerBelow(feedForwardW1Image)
        
        feedForwardW2Label.text = "Hidden -> Output (${transformerBlock.W2.getShapeString(isWeightMatrix = true)})"
        feedForwardW2Label.centerBelow(feedForwardW2Image)

        blockInputLabel.anchorCenterBottom().alignTo(this@TransformerBlockNode.anchorCenterBottom().withOffset(offsetY = -5.0))
        blockOutputLabel.anchorCenterBottom().alignXTo(this@TransformerBlockNode.anchorCenterTop())
        blockOutputLabel.anchorCenterBottom().alignYTo(feedForwardOutputImage.anchorCenterTop().withOffset(offsetY = -55.0))
    }

    fun PNode.addLabel(text: String): PText {
        val label = PText().apply {
            this.text = text
            font = INFO_FONT
        }
        addChild(label)
        return label
    }

    /**
     * Helper class to build arrow paths using anchor points with offsets
     */
    class ArrowPathBuilder(private val parentNode: PNode) {
        private val points = mutableListOf<Point2D>()
        
        fun startAt(anchor: PNodeAnchor): ArrowPathBuilder {
            val globalPoint = anchor.globalPoint
            val localPoint = parentNode.globalToLocal(globalPoint)
            points.add(localPoint)
            return this
        }
        
        fun startAtXY(xAnchor: PNodeAnchor, yAnchor: PNodeAnchor): ArrowPathBuilder {
            val xGlobalPoint = xAnchor.globalPoint
            val yGlobalPoint = yAnchor.globalPoint
            val xLocalPoint = parentNode.globalToLocal(xGlobalPoint)
            val yLocalPoint = parentNode.globalToLocal(yGlobalPoint)
            points.add(point(xLocalPoint.x, yLocalPoint.y))
            return this
        }
        
        fun lineTo(anchor: PNodeAnchor): ArrowPathBuilder {
            val globalPoint = anchor.globalPoint
            val localPoint = parentNode.globalToLocal(globalPoint)
            points.add(localPoint)
            return this
        }
        
        fun lineTo(x: kotlin.Double, y: kotlin.Double): ArrowPathBuilder {
            points.add(point(x, y))
            return this
        }
        
        fun lineToRelative(offsetX: kotlin.Double, offsetY: kotlin.Double): ArrowPathBuilder {
            if (points.isNotEmpty()) {
                val lastPoint = points.last()
                points.add(Point2D.Double(lastPoint.x + offsetX, lastPoint.y + offsetY))
            }
            return this
        }
        
        fun lineToXY(xAnchor: PNodeAnchor, yAnchor: PNodeAnchor): ArrowPathBuilder {
            val xGlobalPoint = xAnchor.globalPoint
            val yGlobalPoint = yAnchor.globalPoint
            val xLocalPoint = parentNode.globalToLocal(xGlobalPoint)
            val yLocalPoint = parentNode.globalToLocal(yGlobalPoint)
            points.add(point(xLocalPoint.x, yLocalPoint.y))
            return this
        }
        
        fun lineToX(xAnchor: PNodeAnchor): ArrowPathBuilder {
            if (points.isEmpty()) return this
            val xGlobalPoint = xAnchor.globalPoint
            val xLocalPoint = parentNode.globalToLocal(xGlobalPoint)
            val currentY = points.last().y
            points.add(point(xLocalPoint.x, currentY))
            return this
        }
        
        fun lineToY(yAnchor: PNodeAnchor): ArrowPathBuilder {
            if (points.isEmpty()) return this
            val yGlobalPoint = yAnchor.globalPoint
            val yLocalPoint = parentNode.globalToLocal(yGlobalPoint)
            val currentX = points.last().x
            points.add(point(currentX, yLocalPoint.y))
            return this
        }
        
        fun build(strokeWidth: kotlin.Float = 1.0f, strokeColor: Color = Color.GRAY): Arrow {
            return Arrow(points.toList(), ArrowType.ARROWHEAD, strokeWidth = strokeWidth, strokeColor = strokeColor)
        }
        
        fun buildWithoutArrowhead(strokeWidth: kotlin.Float = 1.0f, strokeColor: Color = Color.GRAY): Arrow {
            return Arrow(points.toList(), ArrowType.NONE, strokeWidth = strokeWidth, strokeColor = strokeColor)
        }
        
        fun buildWithJunction(strokeWidth: kotlin.Float = 1.0f, strokeColor: Color = Color.GRAY): Arrow {
            return Arrow(points.toList(), ArrowType.JUNCTION, strokeWidth = strokeWidth, strokeColor = strokeColor)
        }
    }
    
    /**
     * Extension function to create an arrow path builder
     */
    fun PNode.createArrowPath(): ArrowPathBuilder {
        return ArrowPathBuilder(this)
    }
    
    /**
     * Extension function to add offsets to a PNodeAnchor
     */
    fun PNodeAnchor.withOffset(offsetX: kotlin.Double = 0.0, offsetY: kotlin.Double = 0.0): PNodeAnchor {
        return PNodeAnchor(this.node, this.offsetX + offsetX, this.offsetY + offsetY)
    }

    enum class ArrowType {
        ARROWHEAD,
        NONE,
        JUNCTION
    }

    class Arrow(
        private val path: List<Point2D>, 
        private val arrowType: ArrowType = ArrowType.ARROWHEAD,
        private val strokeWidth: kotlin.Float = 1.0f,
        private val strokeColor: Color = Color.GRAY
    ): PNode() {
        
        private val arrowHeadSize = 5.0
        private val junctionRadius = 4.0
        
        init {
            if (path.size >= 2) {
                createArrowPath()
            }
        }
        
        private fun createArrowPath() {
            // Just set the bounds based on the path
            val bounds = Rectangle2D.Double()
            path.forEach { point ->
                bounds.add(point)
            }
            // Expand bounds slightly for arrowhead or junction
            val expandSize = when (arrowType) {
                ArrowType.JUNCTION -> junctionRadius
                else -> arrowHeadSize
            }
            bounds.setRect(
                bounds.x - expandSize, 
                bounds.y - expandSize,
                bounds.width + 2 * expandSize, 
                bounds.height + 2 * expandSize
            )
            setBounds(bounds)
        }
        
        override fun paint(paintContext: org.piccolo2d.util.PPaintContext) {
            if (path.size < 2) return
            
            val g2 = paintContext.graphics as Graphics2D
            val originalStroke = g2.stroke
            val originalColor = g2.color
            
            // Set drawing properties
            g2.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.color = strokeColor
            
            // Draw the main line
            val xArray = IntArray(path.size) { i -> path[i].x.toInt() }
            val yArray = IntArray(path.size) { i -> path[i].y.toInt() }
            g2.drawPolyline(xArray, yArray, path.size)
            
            // Draw the ending based on arrow type
            when (arrowType) {
                ArrowType.ARROWHEAD -> {
                    if (path.size >= 2) {
                        val lastPoint = path.last()
                        val secondLastPoint = path[path.size - 2]

                        val dx = lastPoint.x - secondLastPoint.x
                        val dy = lastPoint.y - secondLastPoint.y
                        val angle = kotlin.math.atan2(dy, dx)

                        val arrowAngle = kotlin.math.PI / 6 // 30 degrees

                        val x1 = lastPoint.x - arrowHeadSize * kotlin.math.cos(angle - arrowAngle)
                        val y1 = lastPoint.y - arrowHeadSize * kotlin.math.sin(angle - arrowAngle)

                        val x2 = lastPoint.x - arrowHeadSize * kotlin.math.cos(angle + arrowAngle)
                        val y2 = lastPoint.y - arrowHeadSize * kotlin.math.sin(angle + arrowAngle)

                        g2.drawLine(lastPoint.x.toInt(), lastPoint.y.toInt(), x1.toInt(), y1.toInt())
                        g2.drawLine(lastPoint.x.toInt(), lastPoint.y.toInt(), x2.toInt(), y2.toInt())
                    }
                }
                ArrowType.JUNCTION -> {
                    val lastPoint = path.last()
                    
                    // Draw filled white circle
                    val originalFillColor = g2.color
                    g2.color = Color.WHITE
                    g2.fillOval(
                        (lastPoint.x - junctionRadius).toInt(),
                        (lastPoint.y - junctionRadius).toInt(),
                        (2 * junctionRadius).toInt(),
                        (2 * junctionRadius).toInt()
                    )
                    
                    // Draw circle border
                    g2.color = strokeColor
                    g2.drawOval(
                        (lastPoint.x - junctionRadius).toInt(),
                        (lastPoint.y - junctionRadius).toInt(),
                        (2 * junctionRadius).toInt(),
                        (2 * junctionRadius).toInt()
                    )
                    
                    // Draw plus sign
                    val plusSize = junctionRadius * 0.5
                    // Horizontal line
                    g2.drawLine(
                        (lastPoint.x - plusSize).toInt(),
                        lastPoint.y.toInt(),
                        (lastPoint.x + plusSize).toInt(),
                        lastPoint.y.toInt()
                    )
                    // Vertical line
                    g2.drawLine(
                        lastPoint.x.toInt(),
                        (lastPoint.y - plusSize).toInt(),
                        lastPoint.x.toInt(),
                        (lastPoint.y + plusSize).toInt()
                    )
                }
                ArrowType.NONE -> {
                    // No ending decoration
                }
            }
            
            // Restore original graphics state
            g2.stroke = originalStroke
            g2.color = originalColor
        }
    }
}