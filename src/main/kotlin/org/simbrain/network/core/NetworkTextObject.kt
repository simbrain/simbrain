package org.simbrain.network.core

import org.simbrain.network.events.TextObjectEvents
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Font
import java.awt.geom.Point2D

/**
 * **NetworkTextObject** is a string of text in a neural network, typically
 * used to label elements of a neural network simulation. Contains basic text
 * properties as well. Multiple lines of text can be displayed.
 */
open class NetworkTextObject : EditableObject, LocatableModel {

    private var x = 0.0

    private var y = 0.0

    var text = ""
        set(value) {
            field = value
            events.textUpdated.fire()
        }

    /**
     * Styled ranges overlaid on [text], resolved to theme colors by the view so they track
     * light/dark mode. Kept consistent with the text by [setStyledText]; callers assigning [text]
     * directly leave old spans in place (the view clamps them to the text bounds), so styled text
     * should always be set through [setStyledText].
     */
    var spans: List<TextSpan> = listOf()
        private set

    fun setStyledText(newText: String, newSpans: List<TextSpan>) {
        spans = newSpans
        text = newText
    }

    var fontName: String = Font.SANS_SERIF

    var fontSize: Int = 12

    var isItalic: Boolean = false

    var isBold: Boolean = false

    @Transient
    override val events: TextObjectEvents = TextObjectEvents()

    @XStreamConstructor
    constructor(): super()

    constructor(initialText: String): super() {
        text = initialText
    }

    constructor(text: NetworkTextObject): super() {
        this.text = text.text
        this.x = text.x
        this.y = text.y
        this.fontSize = text.fontSize
        this.fontName = text.fontName
        this.isBold = text.isBold
        this.isItalic = text.isItalic
    }

    override fun toString(): String {
        return "(${Math.round(x)},${Math.round(y)})"
    }

    override suspend fun delete(): List<NetworkTextObject> {
        events.deleted.fire(this)
        return listOf(this)
    }

    override var location: Point2D
        get() = Point2D.Double(x, y)
        set(location) {
            x = location.x
            y = location.y
            events.locationChanged.fire()
        }

    override val name: String
        get() = "Text Object"
}

/**
 * Semantic style for a [TextSpan]; the view maps roles to theme colors and decorations
 * (see [org.simbrain.network.gui.nodes.TextNode]).
 */
enum class TextStyleRole { WARNING, ACTION, EMPHASIS }

/**
 * A styled range of a [NetworkTextObject]'s text, from [start] (inclusive) to [end] (exclusive).
 */
data class TextSpan(val start: Int, val end: Int, val role: TextStyleRole)
