package org.simbrain.network.llm

import org.json.JSONObject
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.TensorRole
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path

/**
 * Reads a safetensors file into off-heap parameter tensors, widening bf16 to f32
 * (bit pattern shifted into the high half of the float). The file is memory-mapped and
 * read in a single pass; tensors are keyed by their names in the file.
 *
 * Shapes map onto the 2-D [FloatTensor]: 1-D vectors become 1xN, and trailing/middle
 * singleton dims are dropped (depthwise conv kernels are stored [channels, 1, k]).
 */
object Safetensors {

    fun load(path: Path): Map<String, FloatTensor> = mapped(path) { header, data ->
        val tensors = LinkedHashMap<String, FloatTensor>()
        for (name in header.keySet()) {
            if (name == "__metadata__") continue
            val shape = header.getJSONObject(name).getJSONArray("shape").let { arr -> IntArray(arr.length()) { arr.getInt(it) } }
            val (rows, cols) = to2d(name, shape)
            val tensor = FloatTensor(rows, cols, TensorRole.PARAMETER)
            read(name, header, data, tensor)
            tensors[name] = tensor
        }
        tensors
    }

    /** Re-reads one named tensor from the file into [tensor] — restoring a hand-edited parameter. */
    fun loadInto(path: Path, name: String, tensor: FloatTensor) = mapped(path) { header, data ->
        require(header.has(name)) { "No tensor $name in $path" }
        read(name, header, data, tensor)
    }

    private fun <T> mapped(path: Path, block: (JSONObject, ByteBuffer) -> T): T {
        RandomAccessFile(path.toFile(), "r").use { file ->
            val channel = file.channel
            val headerLenBuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, 8).order(ByteOrder.LITTLE_ENDIAN)
            val headerLen = headerLenBuf.long
            require(headerLen in 1..100_000_000) { "Implausible safetensors header length $headerLen" }
            val headerBytes = ByteArray(headerLen.toInt())
            channel.map(FileChannel.MapMode.READ_ONLY, 8, headerLen).get(headerBytes)
            val header = JSONObject(String(headerBytes, Charsets.UTF_8))

            val dataStart = 8 + headerLen
            val data = channel.map(FileChannel.MapMode.READ_ONLY, dataStart, channel.size() - dataStart)
                .order(ByteOrder.LITTLE_ENDIAN)
            return block(header, data)
        }
    }

    private fun read(name: String, header: JSONObject, data: ByteBuffer, tensor: FloatTensor) {
        val entry = header.getJSONObject(name)
        val dtype = entry.getString("dtype")
        val offsets = entry.getJSONArray("data_offsets")
        val begin = offsets.getLong(0)
        val end = offsets.getLong(1)
        val count = tensor.size
        val dst = tensor.data.duplicate()
        when (dtype) {
            "BF16" -> {
                require(end - begin == count * 2L) { "$name: byte span ${end - begin} != ${count * 2}" }
                val src = data.slice(begin.toInt(), (end - begin).toInt())
                    .order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                for (i in 0 until count) {
                    dst.put(i, Float.fromBits(src.get(i).toInt() shl 16))
                }
            }
            "F32" -> {
                require(end - begin == count * 4L) { "$name: byte span ${end - begin} != ${count * 4}" }
                val src = data.slice(begin.toInt(), (end - begin).toInt())
                    .order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                dst.put(src)
            }
            else -> throw IllegalArgumentException("$name: unsupported dtype $dtype")
        }
        tensor.markMutated()
    }

    private fun to2d(name: String, shape: IntArray): Pair<Int, Int> {
        val nonSingleton = shape.filter { it != 1 }
        return when {
            nonSingleton.isEmpty() -> 1 to 1
            nonSingleton.size == 1 -> 1 to nonSingleton[0]
            nonSingleton.size == 2 -> nonSingleton[0] to nonSingleton[1]
            else -> throw IllegalArgumentException("$name: cannot map shape ${shape.toList()} to 2-D")
        }
    }
}
