package cz.vasabi.myiot.todo


interface CustomDataType {
    var name: String
    var description: String
    var fields: HashMap<String, DataType>
}

// Todo some constrains

class RgbDataType(
    override var name: String = "RGB",
    override var description: String = "representation of color in R G B",
    override var fields: HashMap<String, DataType> = hashMapOf(
        "r" to DataType.Base(BaseDataType.Float()),
        "g" to DataType.Base(BaseDataType.Float()),
        "b" to DataType.Base(
            BaseDataType.Float()
        )
    )
) : CustomDataType

/*
definition
{
    "name": "RGB",
    "description": "representation of color with R G B",
    "fields": {
        "r": {
            "origin": "base"
            "type": "float"
        },
        "g": {
            "origin": "base"
            "type": "float"
        },
        "b": {
            "origin": "base"
            "type": "float"
        }
    }
}
assemebled
{
    "r": 255,
    "g": 0,
    "b": 0
}

enum
{
    "type": {
        "choice": "lmao",
        "values": {
            "r": 255,
            "g": 0,
            "b": 0

        }
    }
}

"type": {
    "choice": "lmao",
    "values": {
        "r": [255, 225, 255],
        "g": [0,0,0],
        "b": [0,0,0]
    }
}

 */

sealed class DataTypeValue: DataType {
    class Base(override val value: BaseDataTypeValue): DataType.Base(value)
    class Container(override val value: BaseContainerValue): DataType.Container(value)
    class Custom(override val value: CustomDataType): DataType.Custom(value)
}

sealed interface DataType {
    open class Base(open val value: BaseDataType): DataType
    open class Container(open val value: BaseContainer): DataType
    open class Custom(open val value: CustomDataType): DataType
}

sealed class BaseDataTypeValue: BaseDataType {
    class Float(val value: kotlin.Float): BaseDataType.Float()
    class Int(val value: kotlin.Int): BaseDataType.Int()
    class Bool(val value: Boolean): BaseDataType.Bool()
    class String(val value: kotlin.String): BaseDataType.String()
}

sealed interface BaseDataType {
    open class Float : BaseDataType
    open class Int: BaseDataType
    open class Bool: BaseDataType
    open class String: BaseDataType
}

sealed class BaseContainerValue: BaseContainer {
    class Array(val values: List<DataTypeValue>, type: BaseDataType): BaseContainer.Array(type)
    class Enum(val options: List<DataTypeValue>): BaseContainer.Enum()
}

sealed interface BaseContainer {
    open class Array(val type: BaseDataType)
    open class Enum
}

sealed class DataTyp

/*
interface ReadableCompatibility: DeviceCapability {
    fun readValue(): CustomDataType {
        throw Exception()
    }
}

interface WritableCompatibility: DeviceCapability {
    // ?
    fun sendValue(data: CustomDataType) {

    }unused dependency
}

interface StreamedCompatibility: DeviceCapability {
    fun onValueReceived(data: CustomDataType) {

    }
}

interface ValueCompatibility: WritableCompatibility, ReadableCompatibility, StreamedCompatibility {

}

interface DeviceInfo {
    var name: String
    // optional description editable
    var description: String?
    // unique identifier of device
    var identifier: UUID
}

interface Device: DeviceInfo {

    // device can have multiple connections
    /*
    device will always stream data on bt
    we can optionally connect tcp to establish full-duplex com
     */
    var connections: List<Connection>
    /*
    some devices can be stream only
    other pure controllers
    FIXME its should be tied up to connection
     */
    var capabilities: List<DeviceCapability>
}

 */