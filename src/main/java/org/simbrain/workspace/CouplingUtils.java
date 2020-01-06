// package org.simbrain.workspace;
//
// import java.lang.reflect.Method;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
//
// /**
//  * A utility class for coupling. Most of the methods are helpers to get {@link Consumer}s and {@link Producer}s.
//  */
// public abstract class CouplingUtils {
//
//     /**
//      * Get all methods that are annotated with {@link Consumable} in a attribute container.
//      *
//      * @param container the attribute container to search
//      * @return a list of all consumable methods
//      */
//     public static Stream<Method> getConsumableMethodsFromContainer(AttributeContainer container) {
//         return Arrays.stream(container.getClass().getMethods())
//                 .filter(CouplingUtils::isConsumable);
//     }
//
//     /**
//      * Get all methods that are annotated with {@link Producible} in a attribute container.
//      *
//      * @param container the attribute container to search
//      * @return a list of all producible methods
//      */
//     public static Stream<Method> getProducibleMethodsFromContainer(AttributeContainer container) {
//         return Arrays.stream(container.getClass().getMethods())
//                 .filter(CouplingUtils::isProducible);
//     }
//
//     /**
//      * Get all the potential consumers from an attribute container.
//      *
//      * @param container The object to check for Consumables.
//      * @return A list of consumers.
//      */
//     public static Stream<Consumer> getConsumersFromContainer(AttributeContainer container) {
//         return getConsumableMethodsFromContainer(container)
//                 .map(m -> getConsumer(container, m));
//     }
//
//     /**
//      * Get all the potential producers from an {@link AttributeContainer}.
//      *
//      * @param container The object to check for Producibles.
//      * @return A list of producers.
//      */
//     public static Stream<Producer> getProducersFromContainers(AttributeContainer container) {
//         return getProducibleMethodsFromContainer(container)
//                 .map(m -> getProducer(container, m));
//     }
//
//     /**
//      * Get a specific consumer from a specified {@link AttributeContainer}.
//      *
//      * @param container The object in which to find the consumable.
//      * @param methodName The name of the consumable method.
//      * @return The consumer
//      */
//     public static Consumer getConsumer(AttributeContainer container, String methodName) {
//         Stream<Method> stream = Arrays.stream(container.getClass().getMethods());
//         Optional<Method> method = stream.filter(m -> m.getName().equals(methodName)).findFirst();
//         if (method.isPresent()) {
//             return getConsumer(container, method.get());
//         } else {
//             throw new IllegalArgumentException(
//                     String.format(
//                             "No consumable method with name %s was found in class %s.",
//                             methodName,
//                             container.getClass().getSimpleName()
//                     )
//             );
//         }
//     }
//
//     /**
//      * Get a specific producer from the {@link AttributeContainer} object.
//      *
//      * @param container      The container in which to find the producible.
//      * @param methodName The name of the producible method.
//      * @return The producer.
//      */
//     public static Producer getProducer(AttributeContainer container, String methodName) {
//         try {
//             Method method = container.getClass().getMethod(methodName);
//             return getProducer(container, method);
//         } catch (NoSuchMethodException ex) {
//             throw new IllegalArgumentException(ex);
//         }
//     }
//
//     /**
//      * Return whether the specified method is producible.
//      */
//     public static boolean isProducible(Method method) {
//         return method.isAnnotationPresent(Producible.class);
//     }
//
//     /**
//      * Return whether the specified method is consumable.
//      */
//     public static boolean isConsumable(Method method) {
//         return method.isAnnotationPresent(Consumable.class);
//     }
//
//     /**
//      * Create a producer from the specified method on the {@link AttributeContainer}.
//      */
//     public static Producer getProducer(AttributeContainer container, Method method) {
//         Producible annotation = method.getAnnotation(Producible.class);
//         if (annotation == null) {
//             throw new IllegalArgumentException(String.format("Method %s is not producible.", method.getName()));
//         }
//
//         return Producer.builder(container, method)
//                 .description(annotation.description())
//                 .customDescription(getMethod(container, annotation.customDescriptionMethod()))
//                 .arrayDescriptionMethod(getMethod(container, annotation.arrayDescriptionMethod()))
//                 .visibility(annotation.defaultVisibility())
//                 .build();
//     }
//
//     /**
//      * Create a consumer from the specified method on the
//      * {@link AttributeContainer}.
//      */
//     public static Consumer getConsumer(AttributeContainer container, Method method) {
//         Consumable annotation = method.getAnnotation(Consumable.class);
//         if (annotation == null) {
//             throw new IllegalArgumentException(String.format("Method %s in class %s is not consumable.",
//                     method.getName(), method.getDeclaringClass().getSimpleName()));
//         }
//
//         return Consumer.builder(container, method)
//                 .customDescription(getMethod(container, annotation.customDescriptionMethod()))
//                 .description(annotation.description())
//                 .visibility(annotation.defaultVisibility())
//                 .build();
//
//     }
//
//     /**
//      * Helper to get a method object given the object and methodname
//      */
//     public static Method getMethod(AttributeContainer container, String methodName) {
//         try {
//             return container.getClass().getMethod(methodName);
//         } catch (NoSuchMethodException ex) {
//             return null;
//         }
//     }
//
//
// }
