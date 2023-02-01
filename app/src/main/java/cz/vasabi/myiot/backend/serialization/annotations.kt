import cz.vasabi.myiot.backend.connections.BaseDeviceCapability
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

@FieldOrdering(["route", "name", "description", "type"])
data class JsonDeviceCapability @PrimarySerializable constructor(
    override val route: String,
    override val name: String,
    override val description: String,
    override val type: String
) : BaseDeviceCapability