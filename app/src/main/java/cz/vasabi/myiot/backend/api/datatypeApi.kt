package cz.vasabi.myiot.backend.api

import com.fasterxml.jackson.annotation.JsonFormat
sealed interface Data {
    class B(val b: Boolean): Data
    class F(val f: Float): Data
    class I(val i: Int): Data
    class S(val s: String): Data

    val type: String
        get() = when (this) {
            is B -> "bool"
            is F -> "float"
            is I -> "int"
            is S -> "string"
        }

    val value: String
        get() = when (this) {
            is B -> this.b.toString()
            is F -> this.f.toString()
            is I -> this.i.toString()
            is S -> {
                // FIXME proper string escaping
                val res = this.s
                    .replace("\n", "\\n")
                "\"$res\""
            }
        }

    val jsonBody: String
        get() = "{\"type\": \"${type}\", \"value\": ${value}}"
}

interface GenericResponse {
    val value: String
    val type: String

    fun toData(): Data {
        return when (type) {
            "bool" -> Data.B(value.toBoolean())
            "int" -> Data.I(value.toInt())
            "float" -> Data.F(value.toFloat())
            "string" -> Data.S(value)
            else -> {
                throw Exception("unknown data $this")
            }
        }
    }
}

data class GenericHttpResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) override val value: String,
    override val type: String
) : GenericResponse