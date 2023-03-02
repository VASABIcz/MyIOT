package cz.vasabi.myiot.backend.serialization

import io.ktor.utils.io.bits.reverseByteOrder
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.ByteOrder

class BinarySerializer : Serializer {
    val data = ByteArrayOutputStream()
    private val array = DataOutputStream(data)
    override fun writeInt(it: Int): Boolean {
        println("writing int $it")
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            array.writeInt(it.reverseByteOrder())
        }
        array.writeInt(it)
        return true
    }

    override fun writeString(it: String): Boolean {
        writeInt(it.length)
        println("writing string $it")
        array.writeBytes(it)
        return true
    }

    override fun writeBool(it: Boolean): Boolean {
        println("writing bool $it")
        array.writeBoolean(it)
        return true
    }

    override fun writeFloat(it: Float): Boolean {
        println("writing float $it")
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            array.writeFloat(it.reverseByteOrder())
        }
        array.writeFloat(it)
        return true
    }
}

class BinaryDeserializer(stream: InputStream) : Deserializer {

    val array = DataInputStream(stream)
    override fun readInt(): Int? {
        return runCatching {
            var i = array.readInt()
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                i = i.reverseByteOrder()
            }
            i
        }.getOrNull().also {
            println("read i $it")
        }
    }

    override fun readString(): String? {
        return kotlin.runCatching {
            val size = readInt() ?: return@runCatching null
            array.readNBytes(size).decodeToString()
        }.getOrNull().also {
            println("read s $it")
        }
    }

    override fun readBool(): Boolean? {
        return kotlin.runCatching {
            array.readBoolean()
        }.getOrNull().also {
            println("read b $it")
        }
    }

    override fun readFloat(): Float? {
        return kotlin.runCatching {
            var i = array.readFloat()

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                i = i.reverseByteOrder()
            }
            i
        }.getOrNull().also {
            println("read f $it")
        }
    }
}