package org.simbrain.network.compositor

import org.simbrain.network.llm.AttendMixOp
import org.simbrain.network.llm.AttendScoresOp
import org.simbrain.network.llm.CacheWriteOp
import org.simbrain.network.llm.CausalConvOp
import org.simbrain.network.llm.EmbedLookupOp
import org.simbrain.network.llm.HeadwiseNormRopeOp
import org.simbrain.network.llm.OffsetGateOp
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.BiasOp
import org.simbrain.network.tensor.op.CausalMaskedRowSoftmaxOp
import org.simbrain.network.tensor.op.HeadMixOp
import org.simbrain.network.tensor.op.HeadScoresOp
import org.simbrain.network.tensor.op.LayerNormOp
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.MatMulLinearOp
import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.ReLUOp
import org.simbrain.network.tensor.op.RmsNormOp
import org.simbrain.network.tensor.op.SeqEmbedOp
import org.simbrain.network.tensor.op.SeqSoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SiluGateOp
import org.simbrain.network.tensor.op.SoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.TensorOp

/**
 * One stage of an op glyph: its icon, and the positions in the op's input list whose arrows
 * enter the glyph at this stage's pin.
 */
class GlyphStage(val icon: String, val inputIndices: List<Int>)

/**
 * The stage strip for [op]'s glyph. Ops that fuse several named transforms render one stage per
 * transform, in application order, each claiming the input pins the transform actually consumes —
 * so the glyph shows q and k entering the multiply before the softmax, cos/sin entering the
 * rotation after the norm, and the gate passing through silu before the elementwise product.
 * Everything else is a single stage taking all inputs; null falls back to the text pill.
 */
fun glyphStages(op: TensorOp): List<GlyphStage>? = when (op) {
    is AttendScoresOp -> listOf(
        GlyphStage("icons/op-multiply.svg", listOf(0, 1)),
        GlyphStage("icons/op-softmax.svg", emptyList()),
    )
    is HeadwiseNormRopeOp -> listOf(
        GlyphStage("icons/op-layer-norm.svg", listOf(0, 1)),
        GlyphStage("icons/op-rotate.svg", listOf(2, 3)),
    )
    is SiluGateOp -> listOf(
        GlyphStage("icons/op-silu.svg", listOf(0)),
        GlyphStage("icons/op-multiply.svg", listOf(1)),
    )
    else -> opIcon(op)?.let { listOf(GlyphStage(it, op.inputs.indices.toList())) }
}

/**
 * The stage of [op]'s glyph whose pin consumes the input named [portName] (in [alias]'s display
 * namespace), or null when the name is unknown or matches inputs in more than one stage — the
 * renderer then attaches the edge to the glyph as a whole.
 */
fun stageForInput(op: TensorOp, portName: String, alias: (String) -> String): Int? {
    val stages = glyphStages(op) ?: return null
    if (stages.size == 1) return 0
    val indices = op.inputs.indices.filter { alias(op.inputs[it].name) == portName }
    if (indices.isEmpty()) return null
    return stages.indices.filter { s -> indices.any { it in stages[s].inputIndices } }.singleOrNull()
}

/**
 * How many independent per-head passes [op] runs in one forward — 16 for the query-side
 * attention ops, 8 for the key-side norm+rope under GQA, 1 for flat ops (projections, cache
 * writes, elementwise gates). Glyphs with parallelism > 1 wear a card fan; the fan's absence
 * marks the ops that never see heads.
 */
fun opParallelism(op: TensorOp): Int = when (op) {
    is AttendScoresOp -> op.numHeads
    is AttendMixOp -> op.numHeads
    is HeadwiseNormRopeOp -> op.numHeads
    is HeadScoresOp -> op.numHeads
    is HeadMixOp -> op.numHeads
    else -> 1
}

/** The op's glyph icon in the app icon style, or null for the text-pill fallback. */
fun opIcon(op: TensorOp): String? = when (op) {
    is AddOp, is BiasOp -> "icons/op-add.svg"
    is LinearOp, is MatMulLinearOp, is HeadScoresOp, is HeadMixOp,
    is OffsetGateOp, is AttendMixOp -> "icons/op-multiply.svg"
    is LayerNormOp, is RmsNormOp -> "icons/op-layer-norm.svg"
    is CausalMaskedRowSoftmaxOp -> "icons/op-softmax.svg"
    is SoftmaxCrossEntropyOp, is SeqSoftmaxCrossEntropyOp -> "icons/op-cross-entropy.svg"
    is ReLUOp -> "icons/op-relu.svg"
    is SiluGateOp -> "icons/op-silu.svg"
    is SplitHeadsOp -> "icons/op-split.svg"
    is MergeHeadsOp -> "icons/op-merge.svg"
    is SeqEmbedOp, is EmbedLookupOp -> "icons/op-embed.svg"
    is CausalConvOp -> "icons/op-conv.svg"
    is CacheWriteOp -> "icons/op-cache-write.svg"
    else -> null
}
