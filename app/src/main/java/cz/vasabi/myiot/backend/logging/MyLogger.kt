package cz.vasabi.myiot.backend.logging

import android.util.Log
import cz.vasabi.myiot.SingleState


enum class LogLevel {
    Debug,
    Info,
    Warning,
    Error;

    fun isGt(level: LogLevel): Boolean {
        return toInt() <= level.toInt()
    }

    fun toInt(): Int {
        return when (this) {
            Debug -> 1
            Info -> 2
            Warning -> 3
            Error -> 4
        }
    }
}

fun genLogMsgString(msg: String, level: LogLevel, obj: Any? = null): String {
    val x = if (obj == null) {
        ""
    } else {
        "${obj::class.qualifiedName}.${obj::class.java.enclosingMethod?.name}()"
    }
    return "${level.toString().uppercase()} : $x : $msg"
}

interface MyLogger {
    fun debug(msg: String, obj: Any? = null) {
        log(msg, LogLevel.Debug, obj)
    }

    fun info(msg: String, obj: Any? = null) {
        log(msg, LogLevel.Info, obj)
    }

    fun warning(msg: String, obj: Any? = null) {
        log(msg, LogLevel.Warning, obj)
    }

    fun error(msg: String, obj: Any? = null) {
        log(msg, LogLevel.Error, obj)
    }

    fun log(msg: String, level: LogLevel, obj: Any? = null)
}

object AppLogger : MyLogger {
    override fun log(msg: String, level: LogLevel, obj: Any?) {
        SingleState.events.add(genLogMsgString(msg, level, obj))
    }
}

object AndroidLogger : MyLogger {
    override fun log(msg: String, level: LogLevel, obj: Any?) {
        val logMsg = genLogMsgString(msg, level, obj)

        when (level) {
            LogLevel.Debug -> Log.d(null, logMsg)
            LogLevel.Info -> Log.i(null, logMsg)
            LogLevel.Warning -> Log.w(null, logMsg)
            LogLevel.Error -> Log.e(null, logMsg)
        }
    }
}

class LoggerManager : MyLogger {
    private var loggers = mutableListOf<Pair<MyLogger, LogLevel>>()

    override fun log(msg: String, level: LogLevel, obj: Any?) {
        loggers.forEach {
            if (it.second.isGt(level)) {
                it.first.log(msg, level, obj)
            }
        }
    }

    fun addLogger(logger: MyLogger, level: LogLevel) {
        loggers.add(Pair(logger, level))
    }

    fun setGlobalLogLevel(level: LogLevel) {
        loggers = loggers.map {
            Pair(it.first, level)
        }.toMutableList()
    }
}

val logger = LoggerManager().apply {
    addLogger(AndroidLogger, LogLevel.Debug)
    addLogger(AppLogger, LogLevel.Debug)
}