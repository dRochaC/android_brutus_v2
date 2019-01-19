package com.drocha.brutusv2

import android.os.Handler
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import org.json.JSONArray
import org.json.JSONObject

class EventHandlerManager(btService: BluetoothSPP) : BluetoothSPP.OnDataReceivedListener {

    private var subscribedEvents = mutableListOf<Event>()

    private var isConnected = true

    private var lastMessage: String = ""
    private var lastModule1: ModuleData? = null

    private var lastMessageTime = System.currentTimeMillis()

    private var firstMessage = true
    private var onConnectEvent: () -> Unit = {}
    private var onDisconnectEvent: () -> Unit = {}
    private var onReconnectEvent: () -> Unit = {}

    private var onModule1Event: (module: ModuleData, withError: Boolean) -> Unit = { _, _ -> }

    init {
        btService.setOnDataReceivedListener(this)
    }

    fun prepare() {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val actualTime = System.currentTimeMillis()
                if (actualTime - lastMessageTime > IS_CONNECTED_TIME) {
                    isConnected = false
                    onDisconnectEvent.invoke()
                } else if (!isConnected) {
                    isConnected = true
                    onReconnectEvent.invoke()
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    fun onConnectEvent(onConnectEvent: () -> Unit) {
        this.onConnectEvent = onConnectEvent
    }

    fun onDisconnectEvent(onDisconnectEvent: () -> Unit) {
        this.onDisconnectEvent = onDisconnectEvent
    }

    fun onReconnectEvent(onReconnectEvent: () -> Unit) {
        this.onReconnectEvent = onReconnectEvent
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
            lastMessageTime = System.currentTimeMillis()
            lastMessage = _message

            subscribedEvents.forEach {
                val value = catchValueFromPattern(it.pattern)
                if (value.isNotEmpty() && value != "null" && value != it.lastValue) {
                    it.onCall.invoke(value)
                    it.lastValue = value
                }
            }

            checkModule1()
        }
    }

    private fun checkModule1() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun catchValueFromPattern(pattern: String): String {
        return try {
            val obj = JSONObject(lastMessage)
            val map = jsonToMap(obj)

            map[pattern].toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        var retMap: Map<String, Any> = HashMap()

        if (json !== JSONObject.NULL) {
            retMap = toMap(json)
        }
        return retMap
    }

    private fun toMap(`object`: JSONObject): Map<String, Any> {
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

    private fun toList(array: JSONArray): List<Any> {
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
        private const val IS_CONNECTED_TIME = 3000
    }

}