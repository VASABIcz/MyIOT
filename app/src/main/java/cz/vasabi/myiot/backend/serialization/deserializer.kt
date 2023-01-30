package cz.vasabi.myiot.backend.serialization

import FieldOrdering
import PrimarySerializable
import TypeField
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.typeOf

interface Deserializer {
    fun readInt(): Int?
    fun readString(): String?
    fun readBool(): Boolean?
    fun readFloat(): Float?
}

fun deserializeInternal(
    clazz: KClass<*>,
    deserializer: Deserializer,
    annotations: MutableList<Annotation> = mutableListOf()
): Any? {
    fun readList(clazz: List<KClass<*>>): List<Any>? {
        val n = deserializer.readInt() ?: return null
        val ret = mutableListOf<Any>()

        repeat(n) {
            clazz.forEach {
                val res = deserializeInternal(it, deserializer) ?: return null
                ret.add(res)
            }
        }
        return ret
    }

    annotations.addAll(clazz.findAnnotations(TypeField::class))
    if (clazz.qualifiedName?.startsWith("kotlin.") == true) {
        return when (clazz.simpleName) {
            "Float" -> deserializer.readFloat()
            "Double" -> TODO()
            "Int" -> deserializer.readInt()
            "Long" -> TODO()
            "Boolean" -> deserializer.readBool()
            "String" -> deserializer.readString()
            "Collection", "List", "Array", "Set" -> {
                if (annotations.size != 1) {
                    println("${clazz.qualifiedName} expected exactly 1 TypeField annotations got ${annotations.size}")
                    return null
                }
                val f = annotations.first() as TypeField
                val list = readList(listOf(f.type.first()))

                return when (clazz.simpleName) {
                    "Array" -> list?.toTypedArray()
                    "Set" -> list?.toSet()
                    else -> list
                }
            }

            "Map", "HashMap" -> {
                if (annotations.size != 1) {
                    println("${clazz.qualifiedName} expected exactly 1 TypeField annotations got ${annotations.size}")
                    return null
                }
                val ann = annotations.first() as TypeField
                if (ann.type.size != 2) {
                    println("${clazz.qualifiedName} map expects exactly 2 TypeField fields got ${ann.type} ${ann.type.size}")
                    return null
                }
                val list = readList(listOf(ann.type[0], ann.type[1])) ?: return null
                return hashMapOf(*list.zipWithNext().toTypedArray())
            }

            else -> {
                println("kotlin class ${clazz.qualifiedName} is not implemented")
                return null
            }
        }
    }


    val constructors = clazz.constructors.filter {
        it.hasAnnotation<PrimarySerializable>()
    }
    if (constructors.size != 1) {
        println("${clazz.qualifiedName} must have exactly 1 PrimarySerializable constructor")
        return null
    }
    val args = hashMapOf<KParameter, Any?>()
    val constructor = constructors.first()
    val ordering = clazz.findAnnotation<FieldOrdering>()
    if (ordering == null) {
        println("class ${clazz.qualifiedName} must have FieldOrdering")
        return null
    }

    constructor.parameters.sortedBy { ordering.ordering.indexOf(it.name) }.forEach {
        val cls = it.type.classifier as KClass<*>
        val res = deserializeInternal(
            cls,
            deserializer,
            it.type.findAnnotations(TypeField::class).toMutableList()
        )
        if (res == null) {
            println("failed to parse $cls")
            return null
        }
        args[it] = res
    }
    return constructor.callBy(args)
}

inline fun <reified T> Deserializer.deserialize(): T? {
    val ann = TypeField(type = typeOf<T>().arguments.map { it.type!!.classifier as KClass<*> }
        .toTypedArray())

    return deserializeInternal(T::class, this, mutableListOf(ann)) as T?
}