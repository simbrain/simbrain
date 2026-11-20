/**
 * The wand palette's secondary action: the opposite of the selected action unless one is chosen, how a chosen one
 * follows edits to the list of actions, and that the choice is saved with the palette.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.getSimbrainXStream

class WandPaletteTest {

    private fun paletteOf(vararg actions: WandAction) = WandPalette().apply { actions.forEach { addAction(it) } }

    private fun setTo(value: Double) = AdjustValueAction(amount = AdjustValueAmount.Value(value))

    private fun assertSets(expected: Double, target: AdjustValueTarget, action: WandAction?) {
        action as AdjustValueAction
        assertEquals(AdjustValueOperation.Set, action.operation)
        assertEquals(target, action.target)
        assertEquals(expected, (action.amount as AdjustValueAmount.Value).value)
    }

    private fun assertAdds(expected: Double, action: WandAction?) {
        action as AdjustValueAction
        assertEquals(AdjustValueOperation.Add, action.operation)
        assertEquals(expected, (action.amount as AdjustValueAmount.Value).value)
    }

    @Test
    fun `the opposite of setting a value sets zero on the same target with the same brush size`() {
        val activate = AdjustValueAction.activate().apply { radius = 70 }

        assertSets(0.0, AdjustValueTarget.NeuronActivation, activate.opposite())
        assertEquals(70, activate.opposite()?.radius)
        assertSets(0.0, AdjustValueTarget.NeuronActivation, AdjustValueAction.randomize().opposite())
        assertSets(0.0, AdjustValueTarget.SynapseStrength, AdjustValueAction.synapseStrength(0.5).opposite())
    }

    @Test
    fun `the opposite of adding an amount adds its negative`() {
        assertAdds(-0.1, AdjustValueAction.increment(0.1).opposite())
        assertAdds(0.25, AdjustValueAction.decrement(0.25).opposite())
    }

    @Test
    fun `the opposite of setting activations to zero sets them to their upper bound`() {
        val opposite = setTo(0.0).opposite() as AdjustValueAction

        assertTrue(opposite.amount is AdjustValueAmount.UpperBound)
        assertEquals(AdjustValueTarget.NeuronActivation, opposite.target)
    }

    @Test
    fun `actions that nothing takes back have no opposite`() {
        assertNull(AdjustValueAction.synapseStrength(0.0).opposite())
        assertNull(AdjustValueAction(AdjustValueAmount.Random(), AdjustValueOperation.Add).opposite())
        assertNull(ForceSpikeAction().opposite())
        assertNull(PruneWeightsAction().opposite())
    }

    @Test
    fun `with no secondary chosen the secondary is the palette's action opposite to the selected one`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.synapseStrength(0.0), zero)

        assertTrue(palette.isSecondaryAutomatic)
        assertSame(zero, palette.secondaryAction)
    }

    @Test
    fun `with no secondary chosen and no opposite in the palette one is made`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit())

        val secondary = palette.secondaryAction

        assertSets(0.0, AdjustValueTarget.NeuronActivation, secondary)
        assertFalse(palette.actions.any { it === secondary })
    }

    @Test
    fun `an automatic secondary changes with the selected action`() {
        val decrement = AdjustValueAction.decrement()
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.increment(), decrement, setTo(0.0))

        palette.selectAction(1)

        assertSame(decrement, palette.secondaryAction)
    }

    @Test
    fun `the default palette draws and erases activations`() {
        val palette = WandPalette.createDefault()

        assertTrue((palette.selectedAction as AdjustValueAction).amount is AdjustValueAmount.UpperBound)
        assertSets(0.0, AdjustValueTarget.NeuronActivation, palette.secondaryAction)
        assertTrue(palette.actions.any { it === palette.secondaryAction })
    }

    @Test
    fun `there is no secondary when the selected action has no opposite and none is chosen`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(ForceSpikeAction(), inhibit)
        assertNull(palette.secondaryAction)

        palette.secondaryIndex = 1

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `a chosen secondary is used in place of the opposite of the selected action`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), inhibit)

        palette.secondaryIndex = 2
        palette.selectAction(1)

        assertFalse(palette.isSecondaryAutomatic)
        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `the opposite of the selected action is suggested when a secondary that does something else is chosen`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), zero, AdjustValueAction.inhibit())
        palette.secondaryIndex = 2

        assertSame(zero, palette.suggestedSecondary)

        palette.useSuggestedSecondary()

        assertSame(zero, palette.secondaryAction)
        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `nothing is suggested when the chosen secondary already does what the opposite would`() {
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), AdjustValueAction.inhibit())

        palette.secondaryIndex = 1

        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `nothing is suggested when the selected action has no opposite`() {
        val palette = paletteOf(ForceSpikeAction(), setTo(0.0))

        palette.secondaryIndex = 1

        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `a suggestion appears when the selection moves away from the action the chosen secondary suits`() {
        val decrement = AdjustValueAction.decrement()
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), AdjustValueAction.increment(), decrement)
        palette.secondaryIndex = 1

        palette.selectAction(2)

        assertSame(decrement, palette.suggestedSecondary)
    }

    @Test
    fun `removing an action ahead of the chosen secondary leaves the same action secondary`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.5), inhibit)
        palette.secondaryIndex = 2

        palette.removeAction(1)

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `removing the chosen secondary makes the secondary automatic`() {
        val palette = paletteOf(ForceSpikeAction(), AdjustValueAction.inhibit(), setTo(0.5))
        palette.secondaryIndex = 1

        palette.removeAction(1)

        assertTrue(palette.isSecondaryAutomatic)
        assertNull(palette.secondaryAction)
    }

    @Test
    fun `swapping makes the secondary action selected and the selected action secondary`() {
        val activate = AdjustValueAction.activate()
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(activate, inhibit, setTo(0.0))
        palette.secondaryIndex = 1

        palette.swapSelectedAndSecondary()

        assertSame(inhibit, palette.selectedAction)
        assertSame(activate, palette.secondaryAction)
    }

    @Test
    fun `swapping an automatic secondary leaves it automatic when the two actions are each other's opposites`() {
        val activate = AdjustValueAction.activate()
        val zero = setTo(0.0)
        val palette = paletteOf(activate, AdjustValueAction.inhibit(), zero)

        palette.swapSelectedAndSecondary()

        assertSame(zero, palette.selectedAction)
        assertSame(activate, palette.secondaryAction)
        assertTrue(palette.isSecondaryAutomatic)
    }

    @Test
    fun `swapping an automatic secondary chooses the action that was selected when it would not otherwise follow`() {
        val setHalf = setTo(0.5)
        val zero = setTo(0.0)
        val palette = paletteOf(setHalf, zero)

        palette.swapSelectedAndSecondary()

        assertSame(zero, palette.selectedAction)
        assertSame(setHalf, palette.secondaryAction)
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
    fun `a palette saved before there was a secondary has an automatic one`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit(), setTo(0.0))
        val savedWithoutSecondary = getSimbrainXStream().toXML(palette)
            .lines().filterNot { "secondaryIndex" in it }.joinToString("\n")

        val restored = getSimbrainXStream().fromXML(savedWithoutSecondary) as WandPalette

        assertEquals(3, restored.actions.size)
        assertTrue(restored.isSecondaryAutomatic)
        assertSame(restored.actions[2], restored.secondaryAction)
    }
}
