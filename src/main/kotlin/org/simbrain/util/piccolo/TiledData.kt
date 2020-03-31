package org.simbrain.util.piccolo

import com.Ostermiller.util.CSVParser
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter
import java.io.ByteArrayInputStream
import java.io.CharArrayReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

/**
 * Tiled `<data>` tag object representation.
 *
 * Cannot handle xml encoded data for now.
 */
@XStreamAlias("data")
@XStreamConverter(value = ToAttributedValueConverter::class, strings = ["data"])
class TiledData(width: Int, height: Int) {

    /**
     * The encoding used to encode the tile layer data. When used, it can be “base64” and “csv” at the moment.
     */
    private val encoding: String? = null

    /**
     * The compression used to compress the tile layer data. Tiled supports “gzip” and “zlib”.
     */
    private val compression: String? = null

    /**
     * Raw content of the data.
     *
     * Not handling embedded image for now.
     */
    private lateinit var data: CharArray

    @Transient
    private var _gid: MutableList<Int>? = null

    /**
     * The flat list of tile id from the raw data.
     */
    val gid: MutableList<Int>
        get() = _gid ?: decodeData().toMutableList().also { _gid = it }

    init {
        _gid = MutableList(width * height) { 0 }
    }

    /**
     * Decode the raw [data] into a list of tile ids.
     *
     * @return the list of tile id this data represents.
     */
    private fun decodeData(): List<Int> {

        fun decodeCSV() =
                CSVParser(CharArrayReader(data)).allValues
                        .flatten()
                        .filter { it.isNotEmpty() }
                        .map { it.toInt() }

        fun decodeBase64() = String(data).replace("[ \n]".toRegex(), "")
                .let { Base64.getDecoder().decode(it)!! }

        fun ByteArray.decompressGzip() = GZIPInputStream(ByteArrayInputStream(this))
                .use { it.readAllBytes()!! }

        fun ByteArray.decompressZlib() = InflaterInputStream(ByteArrayInputStream(this)).readAllBytes()!!

        fun ByteBuffer.asIntSequence() = sequence {
            while(hasRemaining()) {
                yield(getInt())
            }
        }

        return when (encoding) {
            "csv" -> decodeCSV()
            "base64" -> when(compression) {
                "gzip" -> decodeBase64().decompressGzip()
                "zlib" -> decodeBase64().decompressZlib()
                else -> decodeBase64()
            }.let { ByteBuffer.wrap(it).apply { order(ByteOrder.LITTLE_ENDIAN) } }.asIntSequence().toList()
            else -> throw IllegalStateException("Unknown encoding $encoding")
        }

    }

    @Suppress("unused")
    private fun readResolve(): Any {
        _gid = decodeData().toMutableList()
        return this
    }
}