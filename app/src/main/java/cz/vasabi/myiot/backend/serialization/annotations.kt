import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class PrimarySerializable

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class TypeField(val type: Array<KClass<*>>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class FieldOrdering(val ordering: Array<String>)