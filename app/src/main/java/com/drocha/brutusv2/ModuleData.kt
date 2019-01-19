package com.drocha.brutusv2

import android.graphics.Color
import android.util.Log

const val INFO = "INFO"
const val SWITCH = "SWITCH"
const val SEEK_BAR = "SEEK_BAR"
const val PUSH_BUTTON = "PUSH_BUTTON"
const val OUTPUT_VALUE = "OUTPUT_VALUE"

sealed class CommandType(val label: String, val value: String) {
    class PushButton(label: String) : CommandType(label, "")
    class Switch(label: String, value: String) : CommandType(label, value)
    class OutputValue(label: String, value: String) : CommandType(label, value)
    class SeekBar(label: String, value: String) : CommandType(label, value)
}

object CommandTypeParser {

    fun tryParse(mapList: List<HashMap<String, String>>): ModuleData? {

        var moduleData: ModuleData? = null
        val commandTypeList: MutableList<CommandType> = mutableListOf()

        try {
            mapList.forEach {

                val type = it["type"]
                val label = it["label"]
                val value = it["value"]

                if (type == INFO) {
                    moduleData =
                            ModuleData(it["name"]!!, Color.parseColor(it["color"]!!))
                    return@forEach
                }

                val commandType = when (type) {
                    SWITCH -> CommandType.Switch(label!!, value!!)
                    OUTPUT_VALUE -> CommandType.OutputValue(label!!, value!!)
                    PUSH_BUTTON -> CommandType.PushButton(label!!)
                    SEEK_BAR -> CommandType.SeekBar(label!!, value!!)
                    else -> throw UnsupportedOperationException()
                }

                commandTypeList.add(commandType)
            }

            moduleData?.commandTypeList = commandTypeList

        } catch (e: Exception) {
            Log.d("Error", e.message)
        }

        return moduleData
    }
}

class ModuleData(
    val name: String,
    val color: Int,
    var commandTypeList: List<CommandType> = listOf()
)