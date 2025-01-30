package org.simbrain.world.textworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.point
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class TextWorldTest {

    var world = TextWorld()

    init {
        world.text = "This is some text"
    }

    @Test
    fun `test update increments current item`() {
        runBlocking { world.update() }
        assertEquals("This", world.currentToken)
        runBlocking { world.update() }
        assertEquals("is", world.currentToken)
    }

    @Test
    fun `test wraparound`() {
        world.text = "Word1 Word2"
        runBlocking {
            world.update()
            world.update()
            world.update()
        }
        assertEquals("Word1", world.currentToken)
    }

    @Test
    fun testXML() {
        val xmlRep = getTextWorldXStream().toXML(world)
        print(xmlRep)
        val fromXml = getTextWorldXStream().fromXML(xmlRep) as TextWorld
        assertNotNull(fromXml)
        assertEquals("This is some text", fromXml.text)
    }

    @Test
    fun `test coupling with mismatched sizes`() {

        runBlocking {
            val workspace = Workspace()
            val textWorldComponent = TextWorldComponent("Test")
            val networkComponent = NetworkComponent("Network")
            val nc1 = networkComponent.network.addNeuronCollection(4)
            val nc2 = networkComponent.network.addNeuronCollection(14)
            workspace.addWorkspaceComponent(textWorldComponent)
            workspace.addWorkspaceComponent(networkComponent)

            val coupling1 = with(workspace.couplingManager) {
                createCoupling(nc1, textWorldComponent.world)
            }
            workspace.simpleIterate()
            assert(textWorldComponent.world.text.isNotEmpty())
            println(textWorldComponent.world.text)
            with(workspace.couplingManager) {
                removeCoupling(coupling1)
            }

            val coupling2 = with(workspace.couplingManager) {
                createCoupling(nc2, textWorldComponent.world)
            }
            workspace.simpleIterate()
            assert(textWorldComponent.world.text.isNotEmpty())
            println(textWorldComponent.world.text)

        }

    }

    @Test
    fun `test saving and reopening with couplings`() {

        runBlocking {
            val workspace = Workspace()
            val textWorldComponent = TextWorldComponent("Text World")
            textWorldComponent.world.text = "Was this saved?"
            val networkComponent = NetworkComponent("Network")
            val nc = networkComponent.network.addNeuronCollection(6)
            workspace.addWorkspaceComponent(textWorldComponent)
            workspace.addWorkspaceComponent(networkComponent)
            with(workspace.couplingManager) {
                createCoupling(nc, textWorldComponent.world)
            }
            val zipData = workspace.generateZipData(true)
            workspace.clearWorkspace()
            workspace.openFromZipData(zipData)
            val openedWorld = workspace.getComponent("Text World") as TextWorldComponent
            assertEquals("Was this saved?", openedWorld.world.text)
            assertEquals(2, workspace.componentList.size)
            assertEquals(1, workspace.couplings.size)
            //println(workspace)
        }
    }


}