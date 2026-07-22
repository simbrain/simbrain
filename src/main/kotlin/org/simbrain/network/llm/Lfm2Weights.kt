package org.simbrain.network.llm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.util.PreferenceHolder
import org.simbrain.util.StringPreference
import org.simbrain.util.fetchFileWithCache
import org.simbrain.util.getSystemCacheDirectory
import org.simbrain.util.showDirectorySelectionDialog
import org.simbrain.util.showOptionDialog
import org.simbrain.util.showWarningConfirmDialog
import org.simbrain.util.showWarningDialog
import java.io.File
import java.nio.file.Path
import javax.swing.JOptionPane
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

object LlmPreferences : PreferenceHolder() {

    /** Last weights directory the user pointed a language model at. */
    var weightsDirectory by StringPreference("")
}

/**
 * Locates or fetches the LFM2.5-230M weights. Simbrain never bundles or mirrors them: the
 * download is user-initiated, straight from Hugging Face, into the local Simbrain cache.
 */
object Lfm2Weights {

    const val MODEL_NAME = "LFM2.5-230M"
    const val LICENSE_URL = "https://www.liquid.ai/lfm-license"
    private const val RESOLVE_BASE = "https://huggingface.co/LiquidAI/LFM2.5-230M/resolve/main"
    private const val SAFETENSORS_SHA256 = "f630da86651136c9aee893b04b7542007e90fdd718355358e57e7ecc31517cfd"
    private const val TOKENIZER_SHA256 = "df1d8d5ec5d091b460562ffd545e4a5e91d17d4a0db7ebe733be34ed374377bd"
    private const val CACHE_SUBDIRECTORY = "lfm2.5-230m"

    fun isValidWeightsDirectory(dir: Path) =
        dir.resolve("model.safetensors").exists() && dir.resolve("tokenizer.json").exists()

    /**
     * Finds an existing weights directory: the remembered preference, then the Simbrain cache,
     * then the Hugging Face hub cache.
     */
    fun findWeightsDirectory(): Path? {
        if (LlmPreferences.weightsDirectory.isNotEmpty()) {
            Path.of(LlmPreferences.weightsDirectory).takeIf { isValidWeightsDirectory(it) }?.let { return it }
        }
        File(getSystemCacheDirectory(), CACHE_SUBDIRECTORY).toPath()
            .takeIf { isValidWeightsDirectory(it) }?.let { return it }
        val hub = Path.of(
            System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots"
        )
        return (if (hub.exists()) hub.listDirectoryEntries() else emptyList())
            .firstOrNull { isValidWeightsDirectory(it) }
    }

    /**
     * Downloads `model.safetensors` (~460 MB) and `tokenizer.json` from Hugging Face into the
     * Simbrain cache with SHA-256 verification and progress windows. Blocking; call off the EDT.
     *
     * @return the weights directory, or null on failure.
     */
    fun download(): Path? {
        val safetensors = fetchFileWithCache(
            "$RESOLVE_BASE/model.safetensors", CACHE_SUBDIRECTORY, SAFETENSORS_SHA256
        ) ?: return null
        fetchFileWithCache(
            "$RESOLVE_BASE/tokenizer.json", CACHE_SUBDIRECTORY, TOKENIZER_SHA256
        ) ?: return null
        return safetensors.parentFile.toPath()
    }

    val downloadNotice: String
        get() = """
            The weights for $MODEL_NAME by Liquid AI, Inc. (~460 MB) will be downloaded from
            Hugging Face into ${File(getSystemCacheDirectory(), CACHE_SUBDIRECTORY)}.

            They are licensed under the LFM Open License v1.0 ($LICENSE_URL),
            whose terms bind you as the downloader. Commercial use is restricted for
            organizations above ${'$'}10M annual revenue.

            Download now?
        """.trimIndent()
}

/**
 * Walks the user through attaching weights to this model. An existing installation is adopted
 * immediately, ready for the canvas node's background load; otherwise the user chooses between
 * downloading (license notice first; fetched on [scope] off the EDT and loaded when done) and
 * locating a folder. Returns false when the user cancels, leaving the model unloaded — its
 * canvas node still offers locate and download by right-click. Call on the EDT.
 */
fun LanguageModel.obtainWeightsInteractive(scope: CoroutineScope): Boolean {
    Lfm2Weights.findWeightsDirectory()?.let {
        weightsDirectory = it.toString()
        return true
    }
    return when (showOptionDialog(
        "No ${Lfm2Weights.MODEL_NAME} weights were found on this machine.",
        "Language Model Weights",
        arrayOf("Download…", "Locate folder…", "Cancel"),
        defaultOption = 0,
    )) {
        0 -> {
            if (showWarningConfirmDialog(Lfm2Weights.downloadNotice) != JOptionPane.OK_OPTION) return false
            scope.launch(Dispatchers.Default) {
                val dir = Lfm2Weights.download() ?: return@launch
                LlmPreferences.weightsDirectory = dir.toString()
                weightsDirectory = dir.toString()
                if (!isLoaded) runCatching { loadWeights() }
            }
            true
        }
        1 -> {
            val dir = showDirectorySelectionDialog() ?: return false
            if (!Lfm2Weights.isValidWeightsDirectory(Path.of(dir))) {
                showWarningDialog("No model.safetensors and tokenizer.json in $dir")
                return false
            }
            LlmPreferences.weightsDirectory = dir
            weightsDirectory = dir
            true
        }
        else -> false
    }
}
