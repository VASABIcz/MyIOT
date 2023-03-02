package cz.vasabi.myiot.backend.api

import cz.vasabi.myiot.backend.serialization.BinaryDeserializer
import cz.vasabi.myiot.backend.serialization.BinarySerializer
import cz.vasabi.myiot.backend.serialization.Deserializer
import cz.vasabi.myiot.backend.serialization.serialize
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DataMessage(val type: String, val data: Deserializer) {
    fun getBool(): Boolean? {
        if (type != "bool") {
            return null
        }
        return data.readBool()
    }

    fun getString(): String? {
        if (type != "string") {
            return null
        }
        return data.readString()
    }

    fun getInt(): Int? {
        if (type != "int") {
            return null
        }
        return data.readInt()
    }

    fun getFloat(): Float? {
        if (type != "float") {
            return null
        }
        return data.readFloat()
    }
}

fun deserializeMsg(data: InputStream): DataMessage? {
    val d = BinaryDeserializer(data)
    val n = d.readString() ?: return null
    return DataMessage(n, d)
}

fun serialize(type: String, value: Any): ByteArrayOutputStream {
    val s = BinarySerializer()
    s.writeString(type)
    s.serialize(value)
    return s.data
}