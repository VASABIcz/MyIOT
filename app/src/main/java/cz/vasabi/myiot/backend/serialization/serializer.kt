package cz.vasabi.myiot.backend.serialization

import FieldOrdering
import TypeField
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

interface Serializer {
    fun writeInt(it: Int): Boolean
    fun writeString(it: String): Boolean
    fun writeBool(it: Boolean): Boolean
    fun writeFloat(it: Float): Boolean
}

fun serializeInternal(
    value: Any,
    serializer: Serializer,
    annotations: MutableList<Annotation> = mutableListOf()
): Boolean {
    val clazz = value::class

    fun writeList(value: Collection<*>): Boolean {
        serializer.writeInt(value.size)
        value.forEach {
            if (it == null) {
                return false
            }
            serializeInternal(it, serializer)
        }
        return true
    }

    annotations.addAll(clazz.findAnnotations(TypeField::class))
    if ((clazz.qualifiedName?.startsWith("kotlin.") == true) or (clazz.qualifiedName?.startsWith("java.util.") == true)) {
        return when (clazz.simpleName) {
            "Float" -> serializer.writeFloat(value as Float)
            // "Double" -> TODO()
            "Int" -> serializer.writeInt(value as Int)
            // "Long" -> TODO()
            "Boolean" -> serializer.writeBool(value as Boolean)
            "String" -> serializer.writeString(value as String)
            "Collection", "List", "Array", "Set", "ArrayList" -> {
                if (annotations.size != 1) {
                    println("${clazz.qualifiedName} expected exactly 1 TypeField annotations got ${annotations.size}")
                    return false
                }

                val f = annotations.first() as TypeField
                if (f.type.size != 1) {
                    println("${clazz.qualifiedName} expected exactly 1 TypeField annotations got ${f.type} ${f.type.size}")
                    return false
                }

                writeList(value as Collection<*>)
            }

            "Pair" -> {
                serializeInternal((value as Pair<*, *>).first!!, serializer)
                serializeInternal((value).second!!, serializer)
            }

            "Map", "HashMap" -> {
                if (annotations.size != 1) {
                    println("${clazz.qualifiedName} expected exactly 1 TypeField annotations got ${annotations.size}")
                    return false
                }
                val ann = annotations.first() as TypeField
                if (ann.type.size != 2) {
                    println("${clazz.qualifiedName} map expects exactly 2 TypeField fields got ${ann.type}")
                    return false
                }
                writeList((value as Map<*, *>).entries.map { it.key to it.value })
                true
            }

            else -> {
                println("kotlin class ${clazz.qualifiedName} is not implemented")
                false
            }
        }
    }

    val ordering = clazz.findAnnotation<FieldOrdering>()
    if (ordering == null) {
        println("${clazz.qualifiedName} missing FieldOrdering annotation")
        return false
    }


    clazz.memberProperties.sortedBy { ordering.ordering.indexOf(it.name) }.forEach {
        val v = it.getter.call(value) ?: return false
        val ans = it.returnType.findAnnotations(TypeField::class).toMutableList()
        val res = serializeInternal(v, serializer, ans.toMutableList())
        if (!res) {
            return false
        }
    }
    return true
}

fun Serializer.serialize(value: Any): Boolean {
    return serializeInternal(value, this)
}