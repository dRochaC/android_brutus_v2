package com.drocha.brutusv2

import app.akexorcist.bluetotohspp.library.BluetoothSPP

class EventHandlerManager(btService: BluetoothSPP) : BluetoothSPP.OnDataReceivedListener {

    private var subscribedEvents = mutableListOf<Event>()

    private var lastMessage: String = ""

    private var firstMessage = true
    var onConnectEvent: () -> Unit = {}

    init {
        btService.setOnDataReceivedListener(this)
    }

    fun subscribeToEvent(pattern: String, onCall: (String) -> Unit) {
        subscribedEvents.add(Event(pattern, onCall))
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
                if (value.isNotEmpty()) {
                    it.onCall.invoke(value)
                }
            }
        }
    }

    private fun catchValueFromPattern(pattern: String): String {

        val patternIndex = lastMessage.indexOf(pattern)

        if (patternIndex != -1) {

            val finalPatternIndex = lastMessage.indexOf(FINAL_PATTERN, patternIndex)

            if (finalPatternIndex != -1) {

                return lastMessage.substring(pattern.length + patternIndex, finalPatternIndex)
            }
        }

        return ""
    }

    companion object {
        private const val FINAL_PATTERN = ";"
    }

}