package org.simbrain.util.stats

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.mapper.Mapper
import org.apache.commons.math3.random.JDKRandomGenerator
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.distributions.*

/**
 * A probability distribution. Most wrap apache commons math classes. Some are real and some integer valued. When
 * calling sampleDouble on an integer distribution it is cast to double, and similarly from integer to double.
 */
abstract class ProbabilityDistribution(): CopyableObject {

    /**
     * Random generator for pseudo-random sequences on which a seed can be set.
     */
    @Transient
    var randomGenerator = JDKRandomGenerator()

    abstract fun sampleDouble(): Double

    abstract fun sampleDouble(n: Int): DoubleArray

    abstract fun sampleInt(): Int

    abstract fun sampleInt(n: Int): IntArray

    abstract fun deepCopy(): ProbabilityDistribution

    abstract override fun getName(): String

    override fun toString() = name

    override fun copy(): ProbabilityDistribution {
        return deepCopy()
    }

    /**
     * Use this to ensure two probability distributions return the same pseudo-random sequence of numbers.
     * See ProbabilityDistributionTest.kt for exmamples
     */
    fun setSeed(seed: Int) {
        randomGenerator.setSeed(seed)
    }

    open fun readResolve(): Any {
        randomGenerator = JDKRandomGenerator()
        return this
    }

    companion object {

        fun getXStream(): XStream {
            val xstream = Utils.getSimbrainXStream()
            xstream.registerConverter(ProbabilityDistributionConverter(xstream.mapper, xstream.reflectionProvider))
            return xstream
        }

        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(
                ExponentialDistribution::class.java,
                GammaDistribution::class.java, LogNormalDistribution::class.java,
                NormalDistribution::class.java, ParetoDistribution::class.java,
                UniformRealDistribution::class.java,
                PoissonDistribution::class.java,
                UniformIntegerDistribution::class.java
            )
        }
    }

    // TODO: Phase this out by allowing APE to construct a containing object directly
    /**
     * Utility class that encapsulates a probability distribution so that it can
     * be used in an [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor]
     * so that it's easy to create a property editor to edit a probability
     * distribution.
     */
    class Randomizer(probabilityDistribution: ProbabilityDistribution =  UniformRealDistribution()) : EditableObject {

        @UserParameter(label = "Randomizer", isObjectType = true)
        var probabilityDistribution: ProbabilityDistribution = probabilityDistribution

        // Forward to dist
        fun sampleDouble(): Double = probabilityDistribution.sampleDouble()
        fun sampleInt(): Int = probabilityDistribution.sampleInt()

        override fun getName(): String {
            return "Randomizer"
        }
    }

}

class ProbabilityDistributionConverter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
    ReflectionConverter(mapper, reflectionProvider) {
    override fun canConvert(type: Class<*>?): Boolean {
        return super.canConvert(type) && type?.superclass == ProbabilityDistribution::class.java
    }

    // TODO
    // override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
    //     val cls = Class.forName(reader.nodeName)
    //     cls.declaredConstructors
    //     return super.unmarshal(reader, context)
    // }
}
