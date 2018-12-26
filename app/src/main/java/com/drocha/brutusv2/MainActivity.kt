package com.drocha.brutusv2

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), ItemsAdapter.ItemValueChange {

    private val btService = BluetoothSPP(this)
    private val eventHandler = EventHandlerManager(btService)

    private lateinit var itemsAdapter: ItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (btService.isBluetoothEnabled) {
            btService.setupService()
            btService.startService(BluetoothState.DEVICE_OTHER)

            btService.connect(BRUTUS_ADDRESS)

            prepareEventsHandlers()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        initRecyclerView()
    }

    private fun initRecyclerView() {
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)

        val items = mutableListOf<Item>()
        items.add(Item.SwitchItem("Luz interna", INTERN_LED_PATTERN))
        items.add(Item.SwitchItem("Lanterna", LANTERN_PATTERN))
        items.add(Item.SwitchItem("Alarme", ALARM_PATTERN))
        items.add(Item.SwitchItem("Porta USB", USB_PORT_PATTERN))
        items.add(Item.SeekBarItem("Volume (som)", SOUND_VOLUME_PATTERN))

        itemsAdapter = ItemsAdapter(items, this)
        itemsRecyclerView.adapter = itemsAdapter
    }

    private fun prepareEventsHandlers() {
        eventHandler.onConnectEvent = {
            btLoading.visibility = View.GONE

            infoGroup.visibility = View.VISIBLE
        }

        subscribeToEvent(INTERN_LED_PATTERN) {
            itemsAdapter.updateItem(
                INTERN_LED_PATTERN, it.strToBoolean()
            )
        }

        subscribeToEvent(LANTERN_PATTERN) {
            itemsAdapter.updateItem(
                LANTERN_PATTERN, it.strToBoolean()
            )
        }

        subscribeToEvent(ALARM_PATTERN) {
            itemsAdapter.updateItem(
                ALARM_PATTERN, it.strToBoolean()
            )
        }

        subscribeToEvent(USB_PORT_PATTERN) {
            itemsAdapter.updateItem(
                USB_PORT_PATTERN, it.strToBoolean()
            )
        }

        subscribeToEvent(TEMP_PATTERN) {
            tempValue.text = "$it Cº"
        }

        subscribeToEvent(SOUND_VOLUME_PATTERN) {
            itemsAdapter.updateItem(
                SOUND_VOLUME_PATTERN, it.toInt()
            )
        }
    }

    private fun String.strToBoolean(): Boolean {
        return this == "1"
    }

    private fun MainActivity.subscribeToEvent(pattern: String, callback: (String) -> Unit) {
        eventHandler.subscribeToEvent(pattern, callback)
    }

    override fun onSwitchItemValueChanged(pattern: String, isChecked: Boolean) {
        val state = if (isChecked) "1" else "0"
        val data = "$pattern$state;"
        btService.send(data, true)
    }

    override fun onSeekBarItemValueChanged(pattern: String, value: Int) {
        val data = "$pattern$value;"
        btService.send(data, true)
    }

    companion object {
        private const val BRUTUS_ADDRESS = "3C:71:BF:0F:F5:32"

        // Events
        private const val INTERN_LED_PATTERN = "iled"
        private const val LANTERN_PATTERN = "lant"
        private const val TEMP_PATTERN = "temp"
        private const val ALARM_PATTERN = "alarm"
        private const val USB_PORT_PATTERN = "usb"
        private const val SOUND_VOLUME_PATTERN = "soundvol"
    }

}
