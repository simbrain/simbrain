package org.simbrain.world.speechsynthesizer

import org.simbrain.util.nettalk.NettalkPhonology

enum class PhonemeCodecType(private val label: String, val codec: PhonemeCodec) {
    NETTALK("Articulatory features", NettalkPhonology);

    override fun toString(): String = label
}
