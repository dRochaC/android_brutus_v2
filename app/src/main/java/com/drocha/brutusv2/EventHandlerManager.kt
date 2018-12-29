package com.drocha.brutusv2

import app.akexorcist.bluetotohspp.library.BluetoothSPP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class CommandType(val type: String) {
    class Info(type: String = "INFO") : CommandType(type)
    class ActionSwitch(type: String = "ACTION_SWITCH") : CommandType(type)
    class OutputInt(type: String = "OUTPUT_INT") : CommandType(type)
}

class ModuleData(val id: String, val name: String, val commandTypeList: List<CommandType>)

class EventHandlerManager(btService: BluetoothSPP) : BluetoothSPP.OnDataReceivedListener {

    private var subscribedEvents = mutableListOf<Event>()

    private var lastMessage: String = ""

    private var firstMessage = true
    var onConnectEvent: () -> Unit = {}

    var onModule1Event: () -> Unit = {}

    init {
        btService.setOnDataReceivedListener(this)
    }

    fun subscribeToEvent(pattern: String, onCall: (String) -> Unit) {
        subscribedEvents.add(Event(pattern, onCall))
    }

    fun onModule1Event(onModule1Event: () -> Unit) {
        this.onModule1Event = onModule1Event
    }

    override fun onDataReceived(data: ByteArray?, message: String?) {
        if (firstMessage) {
            firstMessage = false
            onConnectEvent.invoke()
        }

        message?.let { _message ->
            lastMessage = _message

            subscribedEvents.forEach {
                val value = catchValueFromPattern(it.pattern)
                if (value.isNotEmpty() && value != it.lastValue) {
                    it.onCall.invoke(value)
                    it.lastValue = value
                }
            }

            checkModule1 {

            }
        }
    }

    private fun checkModule1(function: () -> Unit) {
        val obj = JSONObject(lastMessage)
        val map = jsonToMap(obj)

        if (map.containsKey(MODULE_1)) {
            val module1 = jsonToMap(obj)[MODULE_1]

            function.invoke()
        }
    }

    private fun catchValueFromPattern(pattern: String): String {
        val obj = JSONObject(lastMessage)
        val map = jsonToMap(obj)

        return map[pattern].toString()
    }

    @Throws(JSONException::class)
    fun jsonToMap(json: JSONObject): Map<String, Any> {
        var retMap: Map<String, Any> = HashMap()

        if (json !== JSONObject.NULL) {
            retMap = toMap(json)
        }
        return retMap
    }

    @Throws(JSONException::class)
    fun toMap(`object`: JSONObject): Map<String, Any> {
        val map = HashMap<String, Any>()

        val keysItr = `object`.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            var value = `object`.get(key)

            if (value is JSONArray) {
                value = toList(value)
            } else if (value is JSONObject) {
                value = toMap(value)
            }
            map[key] = value
        }
        return map
    }

    @Throws(JSONException::class)
    fun toList(array: JSONArray): List<Any> {
        val list = ArrayList<Any>()
        for (i in 0 until array.length()) {
            var value = array.get(i)
            if (value is JSONArray) {
                value = toList(value)
            } else if (value is JSONObject) {
                value = toMap(value)
            }
            list.add(value)
        }
        return list
    }

    companion object {

        private const val MODULE_1 = "module1"
    }

}