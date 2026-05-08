package org.simbrain.world.soundworld

import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import org.simbrain.world.soundworld.EspeakRuntime.activeCallback
import org.simbrain.world.soundworld.EspeakRuntime.ensureInitialized
import org.simbrain.world.soundworld.EspeakRuntime.isAvailable
import org.simbrain.world.soundworld.EspeakRuntime.nativeCallback
import org.simbrain.world.soundworld.EspeakRuntime.synth
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Process-wide owner of the eSpeak-ng native library. eSpeak holds global state — voice
 * tables, dictionaries, the active synth callback — so initialization happens exactly once
 * per JVM, and synthesis calls serialize through this object's lock.
 *
 * Resolution order for the library (and its `espeak-ng-data` directory):
 *   1. `-Dsimbrain.espeak.home=<dir>` system property
 *   2. `${user.dir}/espeak-ng/`           (jpackage / shadow zip layout)
 *   3. `${user.dir}/build/espeak-ng/<platform>/`  (./gradlew buildEspeakNg dev output)
 */
internal object EspeakRuntime {

    /** True once [ensureInitialized] has loaded and initialized eSpeak. */
    @Volatile
    var isAvailable: Boolean = false
        private set

    /** Sample rate returned by `espeak_Initialize`, valid once [isAvailable]. */
    @Volatile
    var sampleRate: Int = 22050
        private set

    /**
     * Human-readable explanation of why [ensureInitialized] failed, or null if it succeeded
     * or hasn't been called. Stable for the rest of the JVM lifetime once set.
     */
    @Volatile
    var errorMessage: String? = null
        private set

    private var library: EspeakNgLibrary? = null
    private var lastVoice: String? = null
    private var lastRate: Int = -1
    private var lastVolume: Int = -1
    private var lastPitch: Int = -1

    @Volatile
    private var loadAttempted: Boolean = false

    /** Currently-active synthesis callback. Set by [synth], read by [nativeCallback]. */
    @Volatile
    private var activeCallback: ((Pointer?, Int) -> Int)? = null

    /**
     * Single persistent JNA callback that delegates to whatever Kotlin lambda is set in
     * [activeCallback]. Allocating one trampoline at init time (instead of one per synth
     * call) avoids per-utterance malloc overhead and the JNA-callback-GC footgun.
     */
    private val nativeCallback = object : EspeakNgLibrary.SynthCallback {
        override fun invoke(wav: Pointer?, numsamples: Int, events: Pointer?): Int =
            activeCallback?.invoke(wav, numsamples) ?: 0
    }

    private val lock = Any()

    fun ensureInitialized(): Boolean = synchronized(lock) {
        if (isAvailable) return true
        if (loadAttempted) return false
        loadAttempted = true
        val home = resolveEspeakHome() ?: return fail(
            """No bundled eSpeak-ng installation was found.
              |Run `./gradlew buildEspeakNg` from the project root to build it, then restart Simbrain.""".trimMargin()
        )
        val libFile = locateLibFile(home) ?: return fail(
            """Found an eSpeak-ng directory at
              |$home
              |but it is missing the libespeak-ng shared library.
              |Re-run `./gradlew buildEspeakNg` to rebuild it.""".trimMargin()
        )
        return try {
            val lib = Native.load(libFile.absolutePath, EspeakNgLibrary::class.java)
            val dataParent = File(home, "share").absolutePath
            val rate = lib.espeak_Initialize(
                EspeakNgLibrary.AUDIO_OUTPUT_SYNCHRONOUS,
                /* buflength = */ 0,
                dataParent,
                /* options = */ 0
            )
            if (rate <= 0) {
                fail("eSpeak initialization failed (espeak_Initialize returned $rate).")
            } else {
                lib.espeak_SetSynthCallback(nativeCallback)
                library = lib
                sampleRate = rate
                isAvailable = true
                true
            }
        } catch (e: Throwable) {
            fail("Failed to load libespeak-ng:\n  ${e.message}")
        }
    }

    private fun fail(message: String): Boolean {
        errorMessage = message
        System.err.println("PhonemeSynthesizer: $message")
        return false
    }

    fun setVoice(name: String) = synchronized(lock) {
        val lib = library ?: return
        if (name == lastVoice) return
        val rc = lib.espeak_SetVoiceByName(name)
        if (rc == EspeakNgLibrary.EE_OK) {
            lastVoice = name
        } else {
            System.err.println("eSpeak: could not set voice '$name' (rc=$rc); keeping previous")
        }
    }

    fun setRate(value: Int) = synchronized(lock) {
        val lib = library ?: return
        if (value == lastRate) return
        lib.espeak_SetParameter(EspeakNgLibrary.PARAM_RATE, value, 0)
        lastRate = value
    }

    fun setVolume(value: Int) = synchronized(lock) {
        val lib = library ?: return
        if (value == lastVolume) return
        lib.espeak_SetParameter(EspeakNgLibrary.PARAM_VOLUME, value, 0)
        lastVolume = value
    }

    fun setPitch(value: Int) = synchronized(lock) {
        val lib = library ?: return
        if (value == lastPitch) return
        lib.espeak_SetParameter(EspeakNgLibrary.PARAM_PITCH, value, 0)
        lastPitch = value
    }

    /**
     * Synchronously synthesize [text]. The provided [callback] is invoked with PCM chunks
     * as they're produced. If the callback returns non-zero, eSpeak aborts the remaining
     * synthesis. Calls are serialized — concurrent callers wait their turn.
     */
    fun synth(
        text: String,
        flags: Int = EspeakNgLibrary.ESPEAK_CHARS_UTF8,
        callback: (samples: Pointer?, count: Int) -> Int
    ): Boolean = synchronized(lock) {
        val lib = library ?: return false
        // espeak_Synth wants size INCLUDING the null terminator. Build a buffer with an
        // explicit trailing zero rather than relying on JNA marshaling — eSpeak reads up to
        // size bytes and we don't want it reading past the JNA-pinned array.
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteArray(payload.size + 1)
        System.arraycopy(payload, 0, buffer, 0, payload.size)
        activeCallback = callback
        try {
            val rc = lib.espeak_Synth(
                buffer,
                NativeLong(buffer.size.toLong()),
                /* position = */ 0,
                EspeakNgLibrary.POS_CHARACTER,
                /* endPosition = */ 0,
                flags,
                /* uniqueIdentifier = */ null,
                /* userData = */ null
            )
            return rc == EspeakNgLibrary.EE_OK
        } finally {
            activeCallback = null
        }
    }

    private fun resolveEspeakHome(): File? {
        System.getProperty("simbrain.espeak.home")?.let {
            val f = File(it)
            if (f.isDirectory) return f
        }
        val cwd = File(System.getProperty("user.dir"))
        val candidates = mutableListOf(File(cwd, "espeak-ng"))
        currentPlatform()?.let { candidates += File(cwd, "build/espeak-ng/$it") }
        return candidates.firstOrNull { File(it, "share/espeak-ng-data").isDirectory }
    }

    private fun locateLibFile(home: File): File? {
        val libDir = File(home, "lib")
        val osName = System.getProperty("os.name").lowercase()
        val candidates = when {
            "mac" in osName || "darwin" in osName -> listOf(
                File(libDir, "libespeak-ng.1.dylib"),
                File(libDir, "libespeak-ng.dylib")
            )
            "win" in osName -> listOf(
                File(home, "bin/libespeak-ng.dll"),
                File(home, "bin/espeak-ng.dll")
            )
            else -> listOf(
                File(libDir, "libespeak-ng.so.1"),
                File(libDir, "libespeak-ng.so")
            )
        }
        return candidates.firstOrNull { it.exists() }
    }

    private fun currentPlatform(): String? {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val os = when {
            "mac" in osName || "darwin" in osName -> "macos"
            "win" in osName -> "windows"
            "linux" in osName -> "linux"
            else -> return null
        }
        val arch = when {
            osArch == "aarch64" || osArch == "arm64" -> "arm64"
            osArch == "amd64" || osArch == "x86_64" -> "x64"
            else -> return null
        }
        return "$os-$arch"
    }
}
