package com.drocha.brutusv2

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val btService = BluetoothSPP(this)
    private val eventHandler = EventHandlerManager(btService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (btService.isBluetoothAvailable) {
            btService.setupService()
            btService.startService(BluetoothState.DEVICE_OTHER)

            btService.connect(BRUTUS_ADDRESS)

            prepareEventsHandlers()
        } else {
            Toast.makeText(this, "Bluetooth off", Toast.LENGTH_LONG).show()
        }
    }

    private fun prepareEventsHandlers() {
        eventHandler.onConnectEvent = {
            btLoading.visibility = View.GONE

            itemsRecyclerView.visibility = View.VISIBLE
            itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        }
        eventHandler.subscribeToEvent(INTERN_LED_PATTERN) {

        }

        eventHandler.subscribeToEvent(LANTERN_PATTERN) {

        }
    }

    companion object {
        private const val BRUTUS_ADDRESS = "3C:71:BF:0F:F5:32"

        // Events
        private const val INTERN_LED_PATTERN = "iled"
        private const val LANTERN_PATTERN = "lant"
    }

}
