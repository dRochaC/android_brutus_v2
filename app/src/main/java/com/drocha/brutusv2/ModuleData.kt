package com.drocha.brutusv2

import android.graphics.Color
import android.util.Log

const val INFO = "INFO"
const val ACTION_SWITCH = "ACTION_SWITCH"
const val ACTION_PUSH_BUTTON = "ACTION_PUSH_BUTTON"
const val OUTPUT_VALUE = "OUTPUT_VALUE"

sealed class CommandType(val value: String) {
    class ActionPushButton(val label: String, val command: String) : CommandType("")
    class ActionSwitch(val label: String, val command: String, value: String) : CommandType(value)
    class OutputValue(val label: String, value: String) : CommandType(value)
}

object CommandTypeParser {

    fun tryParse(mapList: List<HashMap<String, String>>): ModuleData? {

        var moduleData: ModuleData? = null
        val commandTypeList: MutableList<CommandType> = mutableListOf()

        try {
            mapList.forEach {

                val type = it["type"]

                if (type == INFO) {
                    moduleData =
                            ModuleData(it["id"]!!, it["name"]!!, Color.parseColor(it["color"]!!))
                    return@forEach
                }

                val commandType = when (type) {
                    ACTION_SWITCH -> CommandType.ActionSwitch(
                        it["label"]!!,
                        it["command"]!!,
                        it["value"]!!
                    )
                    OUTPUT_VALUE -> CommandType.OutputValue(it["label"]!!, it["value"]!!)
                    ACTION_PUSH_BUTTON -> CommandType.ActionPushButton(
                        it["label"]!!,
                        it["command"]!!
                    )
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
    val id: String,
    val name: String,
    val color: Int,
    var commandTypeList: List<CommandType> = listOf()
)