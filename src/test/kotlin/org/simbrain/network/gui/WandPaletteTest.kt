/**
 * The wand palette's secondary action: which action it is when none has been chosen, how it follows edits to the
 * list of actions, and that the choice is saved with the palette.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.simbrain.util.getSimbrainXStream

class WandPaletteTest {

    private fun paletteOf(vararg actions: WandAction) = WandPalette().apply { actions.forEach { addAction(it) } }

    private fun setTo(value: Double) = AdjustValueAction(amount = AdjustValueAmount.Value(value))

    @Test
    fun `with no secondary chosen the secondary is the action that sets activations to zero`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.synapseStrength(0.0), zero)

        assertSame(zero, palette.secondaryAction)
    }

    @Test
    fun `the default palette has a secondary action that sets activations to zero`() {
        val secondary = WandPalette.createDefault().secondaryAction as AdjustValueAction

        assertEquals(AdjustValueTarget.NeuronActivation, secondary.target)
        assertEquals(0.0, (secondary.amount as AdjustValueAmount.Value).value)
    }

    @Test
    fun `a palette with no action that sets activations to zero has no secondary until one is chosen`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), inhibit)
        assertNull(palette.secondaryAction)

        palette.secondaryIndex = 1

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `a chosen secondary is used in place of the action that sets activations to zero`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), inhibit)

        palette.secondaryIndex = 2

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `removing an action ahead of the secondary leaves the same action secondary`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.5), inhibit)
        palette.secondaryIndex = 2

        palette.removeAction(1)

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `removing the chosen secondary does not make the action after it secondary`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit(), setTo(0.5))
        palette.secondaryIndex = 1

        palette.removeAction(1)

        assertNull(palette.secondaryAction)
    }

    @Test
    fun `swapping makes the secondary action selected and the selected action secondary`() {
        val activate = AdjustValueAction.activate()
        val zero = setTo(0.0)
        val palette = paletteOf(activate, AdjustValueAction.inhibit(), zero)

        palette.swapSelectedAndSecondary()

        assertSame(zero, palette.selectedAction)
        assertSame(activate, palette.secondaryAction)
    }

    @Test
    fun `the chosen secondary is saved with the palette`() {
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), AdjustValueAction.inhibit())
        palette.secondaryIndex = 2

        val restored = getSimbrainXStream().fromXML(getSimbrainXStream().toXML(palette)) as WandPalette

        assertEquals(2, restored.secondaryIndex)
        assertEquals("I", restored.secondaryAction?.letter)
    }

    @Test
    fun `a palette saved before there was a secondary gets the action that sets activations to zero`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit(), setTo(0.0))
        val savedWithoutSecondary = getSimbrainXStream().toXML(palette)
            .lines().filterNot { "secondaryIndex" in it }.joinToString("\n")

        val restored = getSimbrainXStream().fromXML(savedWithoutSecondary) as WandPalette

        assertEquals(3, restored.actions.size)
        assertSame(restored.actions[2], restored.secondaryAction)
    }
}
