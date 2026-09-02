package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.AfdThermoreceptorRule
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExposedTypesTest {

    @Test
    fun `exposed types survive serialization and are reset by clearing the workspace`() {
        val workspace = Workspace()
        workspace.exposeTypes(AfdThermoreceptorRule::class)
        assertEquals(setOf(AfdThermoreceptorRule::class.qualifiedName), workspace.exposedTypes)

        val serializer = WorkspaceSerializer(workspace)
        val bytes = ByteArrayOutputStream().also { serializer.serialize(it, true) }.toByteArray()

        workspace.clearWorkspace()
        assertTrue(workspace.exposedTypes.isEmpty())

        runBlocking { serializer.deserialize(ByteArrayInputStream(bytes)) }
        assertEquals(setOf(AfdThermoreceptorRule::class.qualifiedName), workspace.exposedTypes)
    }
}
