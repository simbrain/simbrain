package org.simbrain.network.gui.dialogs

import org.simbrain.network.connections.*
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.util.SwitchableChangeListener
import org.simbrain.util.SwitchablePropertyChangeListener
import org.simbrain.util.Utils
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.beans.PropertyChangeEvent
import java.text.NumberFormat
import java.util.*
import javax.swing.*
import javax.swing.event.ChangeEvent

/**
 * The **SparsityAdjustmentPanel** is a sub-panel for other connection panels
 * which adjusts the sparsity of the connection. This panel includes a text
 * field for setting the sparsity as well as a slider and a text field
 * displaying how many efferents per source neuron there would be if efferents
 * are equalized. All of these are kept in sync.
 *
 * @author Zoë Tosi
 */
class SparsePanel(
    private val connection: Sparse // should only be used in initialization
) : JPanel() {

    /**
     * A slider for setting the sparsity of the connections.
     */
    private val connectionDensitySlider = JSlider(JSlider.HORIZONTAL, 0, 100, 10)

    /**
     * A text field for setting the sparsity of the connections.
     */
    private val densityTf = JFormattedTextField(NumberFormat.getNumberInstance())

    /**
     * A text field allowing the user to specify the number of outgoing synapses
     * per source neuron.
     */
    private val synsPerSource = JFormattedTextField(NumberFormat.getNumberInstance())

    /**
     * A check box for determining if the number of outgoing synapses per source
     * neuron should be equalized.
     */
    private val equalizeEfferentsChkBx = JCheckBox()

    /**
     * A check box for determining whether or not self connections are allowed.
     */
    private val allowSelfConnectChkBx = JCheckBox()

    /**
     * Panel for self connection check box.
     */
    private var allowSelfConnectPanel = JPanel()
    private val network: Network? = null
    private val synapseGroup: SynapseGroup? = null
    private val sourceNeurons: List<Neuron>? = null
    private val targetNeurons: List<Neuron>? = null

    /**
     * A property change listener for the density text field which can be
     * switched off to prevent infinite loops.
     */
    private var densityTfListener: SwitchablePropertyChangeListener? = null

    /**
     * A property change listener for the synapses per source neuron text field
     * which can be switched off to prevent infinite loops.
     */
    private var synsPerSourceListener: SwitchablePropertyChangeListener? = null

    /**
     * A change listener for the slider which can be switched off to prevent
     * infinite loops.
     */
    private var sliderListener: SwitchableChangeListener? = null

    /**
     * The number of target neurons being connected to. Used for determining the
     * number of efferents per source neuron when efferents are equalized.
     */
    private var numTargs = 0

    /**
     * Whether to allow self connections.
     */
    private var allowSelfConnect = false

    /**
     * Whether to allow recurrent connections.
     */
    var isRecurrentConnection = false
        private set

    /**
     * Constructs a gui panel for adjusting the sparsity of a sparse connect
     * neurons object, and initializes all appropriate listeners.
     *
     * @param connection the connection object this panel will act on
     */
    init {
        initPanelValues()
        initializeSparseSlider()
        addChangeListeners()
        addActionListeners()
        initializeLayout()
    }

    /**
     * Initializes the panel's layout
     */
    private fun initializeLayout() {
        densityTfListener!!.disable()
        synsPerSourceListener!!.disable()
        sliderListener!!.disable()
        // Create label/text field combo panels for:
        // Connection Density
        // Equalize Efferents
        // Allow Self-Connection
        val sparseTextPanel = JPanel(FlowLayout())
        sparseTextPanel.add(JLabel("Density: "))
        var sSize = densityTf.preferredSize
        sSize.width = 80
        densityTf.preferredSize = sSize
        sparseTextPanel.add(densityTf)
        val equalizerPanel = JPanel(FlowLayout())
        equalizerPanel.add(JLabel("Equalize Efferents:"))
        equalizerPanel.add(equalizeEfferentsChkBx)
        sSize = synsPerSource.preferredSize
        sSize.width = 80
        synsPerSource.preferredSize = sSize
        equalizerPanel.add(synsPerSource)
        synsPerSource.isVisible = numTargs > 0
        allowSelfConnectPanel = JPanel(FlowLayout())
        allowSelfConnectPanel.add(JLabel("Self Connections: "))
        allowSelfConnectPanel.add(allowSelfConnectChkBx)

        // Begin laying out components
        this.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.insets = Insets(5, 5, 5, 5)

        // Connection density label/textfield
        // (0, 0) w 2
        gbc.fill = GridBagConstraints.NONE
        gbc.gridwidth = 2
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        this.add(sparseTextPanel, gbc)

        // Connection density slider
        // (0, 1) w 4
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridwidth = 4
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        this.add(connectionDensitySlider, gbc)

        // Equalize Efferents label/TextField
        // (0, 2) w 2
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.gridwidth = 2
        this.add(equalizerPanel, gbc)
        gbc.gridx = 2
        gbc.anchor = GridBagConstraints.NORTHEAST
        if (isRecurrentConnection) {
            this.add(allowSelfConnectPanel, gbc)
        } else {
            this.add(Box.createRigidArea(allowSelfConnectPanel.preferredSize), gbc)
        }
        densityTfListener!!.enable()
        synsPerSourceListener!!.enable()
        sliderListener!!.enable()
    }

    /**
     * Initializes the sparse slider.
     */
    private fun initializeSparseSlider() {
        connectionDensitySlider.majorTickSpacing = 10
        connectionDensitySlider.minorTickSpacing = 2
        connectionDensitySlider.paintTicks = true
        val labelTable2 = Hashtable<Int, JLabel>()
        labelTable2[0] = JLabel("0%")
        labelTable2[100] = JLabel("100%")
        connectionDensitySlider.labelTable = labelTable2
        connectionDensitySlider.paintLabels = true
    }

    /**
     * Adds change listeners specific to sparse panel: Sparsity slider, sparsity
     * text field, and syns/source field.
     *
     *
     * Since every field listens to every field, loops are prevented through the
     * use of so-called "switchable listeners", which have fields allowing them
     * to be turned off, and focus listeners. Any field not being interacted
     * with by the user has its listener disabled, and any field being
     * interacted with (which is "in focus") is enabled. This allows all fields
     * to be changed accordingly in response to user input without changes to
     * the out of focus fields themselves firing off events.
     */
    private fun addChangeListeners() {

        // *********************************************************************
        // Slider
        sliderListener = object : SwitchableChangeListener() {
            override fun stateChanged(e: ChangeEvent) {
                val source = e.source as JSlider
                if (source === connectionDensitySlider && isEnabled) {
                    densityTfListener!!.disable()
                    synsPerSourceListener!!.disable()
                    val nt = if (allowSelfConnect || !isRecurrentConnection) numTargs else numTargs - 1
                    val `val` = connectionDensitySlider.value.toDouble() / 100
                    densityTf.value = `val`
                    if (numTargs > 0) {
                        val sps = (`val` * nt).toInt()
                        synsPerSource.value = sps
                    } else {
                        synsPerSource.text = "N/A"
                    }
                }
            }
        }
        connectionDensitySlider.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                // Enables the listener associated with the density slider
                // field while it is in focus.
                sliderListener?.enable()
            }

            override fun focusLost(e: FocusEvent) {
                // Disables the listener associated with the density slider
                // field when it is not in focus.
                sliderListener?.disable()
            }
        })
        connectionDensitySlider.addChangeListener(sliderListener)

        // Equalized efferent number (Synapses per source)
        synsPerSourceListener = object : SwitchablePropertyChangeListener() {
            override fun propertyChange(evt: PropertyChangeEvent) {
                if (isEnabled && evt.source === synsPerSource) {
                    densityTfListener!!.disable()
                    sliderListener?.disable()
                    val nt = if (allowSelfConnect || !isRecurrentConnection) numTargs else numTargs - 1
                    val sps = Utils.parseInteger(synsPerSource)
                    if (sps != null) {
                        if (numTargs > 0) {
                            densityTf.value = sps.toDouble() / nt
                        } else {
                            densityTf.text = "N/A"
                        }
                        val sVal = ((densityTf.value as Number).toDouble() * 100).toInt()
                        connectionDensitySlider.value = sVal
                    }
                }
            }
        }
        synsPerSource.addFocusListener(object : FocusListener {
            override fun focusGained(arg0: FocusEvent) {
                // Enables the listener associated with the synapse per source
                // field while it is in focus.
                synsPerSourceListener?.enable()
            }

            override fun focusLost(arg0: FocusEvent) {
                // Disables the listener associated with the synapse per source
                // field while it is not in focus.
                synsPerSourceListener?.disable()
            }
        })
        synsPerSource.addPropertyChangeListener(synsPerSourceListener)

        // Overall density
        densityTfListener = object : SwitchablePropertyChangeListener() {
            override fun propertyChange(evt: PropertyChangeEvent) {
                if (evt.source === densityTf && isEnabled) {
                    sliderListener?.disable()
                    synsPerSourceListener?.disable()
                    val sps: Int
                    val nt = if (allowSelfConnect) numTargs else numTargs - 1
                    if (densityTf.value != null) {
                        if (numTargs > 0) {
                            sps = ((densityTf.value as Number).toDouble() * nt).toInt()
                            synsPerSource.value = sps
                        } else {
                            synsPerSource.text = "N/A"
                        }
                        val sVal = ((densityTf.value as Number).toDouble() * 100).toInt()
                        connectionDensitySlider.value = sVal
                    }
                }
            }
        }
        densityTf.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                // Enables the listener associated with the density
                // field while it is in focus.
                densityTfListener?.enable()
            }

            override fun focusLost(e: FocusEvent) {
                // Disables the listener associated with the density
                // field while it is not in focus.
                densityTfListener?.disable()
            }
        })
        densityTf.addPropertyChangeListener(densityTfListener)
    }

    /**
     * Adds action listeners specific to sparse panel: sparse specific check
     * box.
     */
    private fun addActionListeners() {
        equalizeEfferentsChkBx.addActionListener { arg0 ->
            if (arg0.source === equalizeEfferentsChkBx) {
                if (equalizeEfferentsChkBx.isSelected) {
                    synsPerSource.isEnabled = true
                } else {
                    synsPerSource.isEnabled = false
                }
            }
        }
        allowSelfConnectChkBx.addActionListener(object : ActionListener {
            override fun actionPerformed(arg0: ActionEvent) {
                allowSelfConnect = allowSelfConnectChkBx.isSelected
                densityTfListener!!.disable()
                sliderListener!!.disable()
                synsPerSourceListener!!.disable()
                val nt = if (allowSelfConnect || !isRecurrentConnection) numTargs else numTargs - 1
                val sparsity = densityTf.text.toDouble()
                synsPerSource.value = (sparsity * nt).toInt()
            }
        })
    }

    fun initPanelValues() {
        val connectivity = connection.connectionDensity
        equalizeEfferentsChkBx.isSelected = connection.equalizeEfferents
        allowSelfConnectChkBx.isSelected = connection.allowSelfConnection
        synsPerSource.value = (connectivity * numTargs).toInt()
        connectionDensitySlider.value = (connectivity * 100).toInt()
        densityTf.value = connectivity
        //        synsPerSource.setEnabled(!editing);
//        equalizeEfferentsChkBx.setEnabled(!editing);
//        allowSelfConnectChkBx.setEnabled(!editing);
    }

    fun applyChanges(sparse: Sparse): Boolean {
        if (equalizeEfferentsChkBx.isEnabled) {
            // Should always be disabled if the connection is AllToAll
            sparse.equalizeEfferents = equalizeEfferentsChkBx.isSelected
        }
        if (allowSelfConnectChkBx.isEnabled) {
            sparse.allowSelfConnection = allowSelfConnectChkBx.isSelected
        }
        val connectivity = Utils.doubleParsable(densityTf)
        if (!java.lang.Double.isNaN(connectivity)) {
            sparse.connectionDensity = connectivity
        }
        return true
    }

    fun applyConnection(source: List<Neuron>, target: List<Neuron>): List<Synapse>? {
        val density = Utils.doubleParsable(densityTf)
        return if (!java.lang.Double.isNaN(density)) {
            if (density == 1.0) {
                connectAllToAll(source, target, allowSelfConnect)
            } else {
                val result =
                    connectSparse(source, target, density, allowSelfConnect, equalizeEfferentsChkBx.isSelected)
                if (result is ConnectionsResult.Add) {
                    result.connectionsToAdd
                } else {
                    java.util.List.of()
                }
            }
        } else null
    }

    fun isAllowSelfConnect(): Boolean {
        return allowSelfConnect
    }

    fun setAllowSelfConnect(allowSelfConnect: Boolean) {
        this.allowSelfConnect = allowSelfConnect
        val nt = if (allowSelfConnect) numTargs else numTargs - 1
        val sps = ((densityTf.value as Number).toDouble() * nt).toInt()
        synsPerSource.value = sps
    }

    fun setRecurrent(recurrent: Boolean) {
        isRecurrentConnection = recurrent
        if (densityTfListener != null && synsPerSourceListener != null && sliderListener != null) {
            refresh()
        }
    }

    fun getNumTargs(): Int {
        return numTargs
    }

    fun setNumTargs(numTargs: Int) {
        this.numTargs = numTargs
        val nt = if (allowSelfConnect) numTargs else numTargs - 1
        val sps = ((densityTf.value as Number).toDouble() * nt).toInt()
        synsPerSource.value = sps
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        connectionDensitySlider.isEnabled = enabled
        densityTf.isEnabled = enabled
        synsPerSource.isEnabled = enabled
        equalizeEfferentsChkBx.isEnabled = enabled
        allowSelfConnectChkBx.isEnabled = enabled
    }

    fun setDensity(density: Double) {
        densityTf.text = java.lang.Double.toString(density)
        sliderListener!!.enable()
        connectionDensitySlider.value = (density * 100).toInt()
        sliderListener!!.disable()
    }

    /**
     * Refreshes the panel's view.
     */
    fun refresh() {
        densityTfListener!!.disable()
        val density = Utils.doubleParsable(densityTf)
        densityTf.text = java.lang.Double.toString(density)
        densityTfListener!!.enable()
        this.removeAll()
        initializeLayout()
        this.repaint()
    }

}
