package com.drocha.brutusv2

import app.akexorcist.bluetotohspp.library.BluetoothSPP
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class EventHandlerManager(btService: BluetoothSPP) : BluetoothSPP.OnDataReceivedListener {

    private var subscribedEvents = mutableListOf<Event>()

    private var lastMessage: String = ""
    private var lastModule1: ModuleData? = null

    private var firstMessage = true
    var onConnectEvent: () -> Unit = {}

    private var onModule1Event: (module: ModuleData, withError: Boolean) -> Unit = { _, _ -> }

    init {
        btService.setOnDataReceivedListener(this)
    }

    fun subscribeToEvent(pattern: String, onCall: (String) -> Unit) {
        subscribedEvents.add(Event(pattern, onCall))
    }

    fun onModule1Event(onModule1Event: (module: ModuleData, withError: Boolean) -> Unit) {
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

            checkModule1()
        }
    }

    private fun checkModule1() {
        val obj = JSONObject(lastMessage)
        val map = jsonToMap(obj)

        if (map.containsKey(MODULE_1)) {

            var moduleError = true

            val module1 = jsonToMap(obj)[MODULE_1] as List<HashMap<String, String>>
            CommandTypeParser.tryParse(module1)?.let {
                lastModule1 = it
                moduleError = false
            }

            lastModule1?.let {
                onModule1Event.invoke(it, moduleError)
            }
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