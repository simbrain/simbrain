package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private object TestThemeColorPrefs : PreferenceHolder() {
    var testThemeColorValue by ThemeColorPreference(
        ThemeColor(Color(0x112233), Color(0xAABBCC), useManualDark = true)
    )
}

private fun colorDelegate(): ThemeColorPreference =
    TestThemeColorPrefs::class.declaredMemberProperties
        .filterIsInstance<KMutableProperty1<Any, *>>()
        .map { it.apply { isAccessible = true }.getDelegate(TestThemeColorPrefs) }
        .filterIsInstance<ThemeColorPreference>()
        .first()

class ThemeColorPreferenceTest {

    @Test
    fun `resolve returns the light color in light mode and the manual dark color in dark mode`() {
        val tc = ThemeColor(Color.RED, Color.BLUE, useManualDark = true)
        assertEquals(Color.RED, tc.resolve(isDark = false))
        assertEquals(Color.BLUE, tc.resolve(isDark = true))
    }

    @Test
    fun `resolve infers the dark color when not manual`() {
        val light = Color(0x3366CC)
        val tc = ThemeColor(light, useManualDark = false)
        assertEquals(light, tc.resolve(isDark = false))
        assertEquals(inferDark(light), tc.resolve(isDark = true))
    }

    @Test
    fun `inferDark darkens a light color and lightens a dark color`() {
        val light = Color(0xE6E6E6)
        val dark = Color(0x1A1A1A)
        assertTrue(light.toHSL().third > inferDark(light).toHSL().third, "a light input should infer to a darker color")
        assertTrue(dark.toHSL().third < inferDark(dark).toHSL().third, "a dark input should infer to a lighter color")
    }

    @Test
    fun `ThemeColorPreference round-trips through serialize and deserialize`() {
        val pref = ThemeColorPreference(ThemeColor(Color.GRAY))
        val tc = ThemeColor(Color(0x010203), Color(0x040506), useManualDark = true)
        assertEquals(tc, pref.deserialize(pref.serialize(tc)))
    }

    @Test
    fun `committing the default does not pin the preference`() {
        TestThemeColorPrefs.revertToDefault()
        TestThemeColorPrefs.testThemeColorValue = colorDelegate().default
        assertFalse(colorDelegate().isExplicitlySet())
        TestThemeColorPrefs.revertToDefault()
    }

    @Test
    fun `a customized theme color pins and then un-pins when returned to the default`() {
        TestThemeColorPrefs.revertToDefault()
        val custom = ThemeColor(Color(0x654321), Color(0x123456), useManualDark = true)

        TestThemeColorPrefs.testThemeColorValue = custom
        assertTrue(colorDelegate().isExplicitlySet())
        assertEquals(custom, TestThemeColorPrefs.testThemeColorValue)

        TestThemeColorPrefs.testThemeColorValue = colorDelegate().default
        assertFalse(colorDelegate().isExplicitlySet())

        TestThemeColorPrefs.revertToDefault()
    }
}
