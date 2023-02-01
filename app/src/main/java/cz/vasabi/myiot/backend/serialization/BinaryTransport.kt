package cz.vasabi.myiot.backend.serialization

import io.ktor.utils.io.bits.reverseByteOrder
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream

class BinarySerializer : Serializer {
    val data = ByteArrayOutputStream()
    private val array = DataOutputStream(data)
    override fun writeInt(it: Int): Boolean {
        array.writeInt(it.reverseByteOrder())
        return true
    }

    override fun writeString(it: String): Boolean {
        writeInt(it.length)
        array.writeBytes(it)
        return true
    }

    override fun writeBool(it: Boolean): Boolean {
        array.writeBoolean(it)
        return true
    }

    override fun writeFloat(it: Float): Boolean {
        array.writeFloat(it.reverseByteOrder())
        return true
    }
}

class BinaryDeserializer(stream: InputStream) : Deserializer {
    private val array = DataInputStream(stream)
    override fun readInt(): Int {
        return array.readInt().reverseByteOrder()
    }

    override fun readString(): String {
        val size = array.readInt().reverseByteOrder()
        return array.readNBytes(size).decodeToString()
    }

    override fun readBool(): Boolean {
        return array.readBoolean()
    }

    override fun readFloat(): Float {
        return array.readFloat().reverseByteOrder()
    }
}