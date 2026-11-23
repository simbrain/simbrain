/**
 * The wand palette's secondary action: that it changes only when chosen, the opposite of the selected action that
 * the palette suggests for it, how it survives edits to the list of actions, and that it is saved with the palette.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
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
        assertNull(AdjustValueAction(AdjustValueAmount.Relative(0.5), AdjustValueOperation.Add).opposite())
        assertNull(ForceSpikeAction().opposite())
        assertNull(PruneWeightsAction().opposite())
    }

    @Test
    fun `a palette has no secondary action until one is chosen`() {
        val inhibit = AdjustValueAction.inhibit()
        val palette = paletteOf(AdjustValueAction.activate(), inhibit, setTo(0.0))
        assertNull(palette.secondaryAction)

        palette.secondaryIndex = 1

        assertSame(inhibit, palette.secondaryAction)
    }

    @Test
    fun `selecting another action leaves the secondary as it was`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), zero, AdjustValueAction.increment(), AdjustValueAction.decrement())
        palette.secondaryIndex = 1

        palette.selectAction(2)

        assertSame(zero, palette.secondaryAction)
    }

    @Test
    fun `the default palette draws and erases activations`() {
        val palette = WandPalette.createDefault()

        assertTrue((palette.selectedAction as AdjustValueAction).amount is AdjustValueAmount.UpperBound)
        assertSets(0.0, AdjustValueTarget.NeuronActivation, palette.secondaryAction)
        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `the opposite of the selected action is suggested when no secondary is chosen`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.synapseStrength(0.0), zero)

        assertSame(zero, palette.suggestedSecondary)

        palette.useSuggestedSecondary()

        assertSame(zero, palette.secondaryAction)
        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `the opposite of the selected action is suggested when the secondary does something else`() {
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), zero, AdjustValueAction.inhibit())
        palette.secondaryIndex = 2

        assertSame(zero, palette.suggestedSecondary)

        palette.useSuggestedSecondary()

        assertSame(zero, palette.secondaryAction)
        assertNull(palette.suggestedSecondary)
    }

    @Test
    fun `using a suggestion the palette does not have adds it to the palette`() {
        val palette = paletteOf(AdjustValueAction.activate().apply { radius = 70 }, AdjustValueAction.inhibit())
        assertSets(0.0, AdjustValueTarget.NeuronActivation, palette.suggestedSecondary)

        palette.useSuggestedSecondary()

        assertEquals(3, palette.actions.size)
        assertSame(palette.actions[2], palette.secondaryAction)
        assertSets(0.0, AdjustValueTarget.NeuronActivation, palette.secondaryAction)
        assertEquals(70, palette.secondaryAction?.radius)
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
    fun `removing an action ahead of the selected one leaves the same action selected`() {
        val inhibit = AdjustValueAction.inhibit()
        val zero = setTo(0.0)
        val palette = paletteOf(AdjustValueAction.activate(), inhibit, zero)
        palette.selectAction(1)
        palette.secondaryIndex = 2

        palette.removeAction(0)

        assertSame(inhibit, palette.selectedAction)
        assertSame(zero, palette.secondaryAction)
    }

    @Test
    fun `removing the secondary action leaves the palette without one`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit(), setTo(0.5))
        palette.secondaryIndex = 1

        palette.removeAction(1)

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
    fun `swapping does nothing when there is no secondary action`() {
        val activate = AdjustValueAction.activate()
        val palette = paletteOf(activate, setTo(0.0))

        palette.swapSelectedAndSecondary()

        assertSame(activate, palette.selectedAction)
        assertNull(palette.secondaryAction)
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
    fun `the selected action is saved with the palette`() {
        val palette = paletteOf(AdjustValueAction.activate(), setTo(0.0), AdjustValueAction.inhibit())
        palette.selectAction(2)

        val restored = getSimbrainXStream().fromXML(getSimbrainXStream().toXML(palette)) as WandPalette

        assertEquals(2, restored.selectedIndex)
    }

    @Test
    fun `a palette saved before there was a secondary has none, and suggests one`() {
        val palette = paletteOf(AdjustValueAction.activate(), AdjustValueAction.inhibit(), setTo(0.0))
        val savedWithoutSecondary = getSimbrainXStream().toXML(palette)
            .lines().filterNot { "secondaryIndex" in it }.joinToString("\n")

        val restored = getSimbrainXStream().fromXML(savedWithoutSecondary) as WandPalette

        assertEquals(3, restored.actions.size)
        assertNull(restored.secondaryAction)
        assertSame(restored.actions[2], restored.suggestedSecondary)
    }
}
