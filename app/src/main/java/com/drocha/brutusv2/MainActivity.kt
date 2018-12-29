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

        initBT()
        initRecyclerView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            initBT()
        }
    }

    private fun initBT() {
        if (btService.isBluetoothEnabled) {
            btService.setupService()
            btService.startService(BluetoothState.DEVICE_OTHER)

            btService.autoConnect(BRUTUS_DEVICE_NAME)

            prepareEventsHandlers()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun initRecyclerView() {
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)

        val items = mutableListOf<Item>()
        items.add(Item.SwitchItem("Luz interna", INTERN_LED_PATTERN))
        items.add(Item.SwitchItem("Lanterna", LANTERN_PATTERN))
        items.add(Item.SwitchItem("Alarme", ALARM_PATTERN))
        items.add(Item.SwitchItem("Porta USB", USB_PORT_PATTERN))
        items.add(Item.SwitchItem("Descoberta de módulos", MODULES_PATTERN))

        itemsAdapter = ItemsAdapter(items, this)
        itemsRecyclerView.adapter = itemsAdapter
    }

    private fun prepareEventsHandlers() {
        eventHandler.onConnectEvent = {
            btLoading.visibility = View.GONE

            tempTitle.visibility = View.VISIBLE
            tempValue.visibility = View.VISIBLE
            itemsRecyclerView.visibility = View.VISIBLE
        }

        eventHandler.onModule1Event { moduleData, withError ->
            if (withError) {
                itemsAdapter.removeModuleItems(moduleData)
            } else {
                itemsAdapter.updateModuleItems(moduleData)
            }
        }

        subscribeEventBooleanWithAdapter(INTERN_LED_PATTERN)
        subscribeEventBooleanWithAdapter(LANTERN_PATTERN)
        subscribeEventBooleanWithAdapter(ALARM_PATTERN)
        subscribeEventBooleanWithAdapter(USB_PORT_PATTERN)
        subscribeEventBooleanWithAdapter(MODULES_PATTERN)

        subscribeEvent(TEMP_PATTERN) {
            tempValue.text = "$it Cº"
        }
    }

    private fun MainActivity.subscribeEvent(pattern: String, callback: (String) -> Unit) {
        eventHandler.subscribeToEvent(pattern, callback)
    }

    private fun MainActivity.subscribeEventBooleanWithAdapter(pattern: String) {
        eventHandler.subscribeToEvent(pattern) {
            itemsAdapter.updateItem(
                pattern, it.toBoolean()
            )
        }
    }

    override fun onSwitchItemValueChanged(pattern: String, isChecked: Boolean) {
        val state = if (isChecked) "1" else "0"
        val data = "$pattern$state"
        btService.send(data, true)
        itemsAdapter.updateItem(pattern, isChecked)
    }

    override fun onSeekBarItemValueChanged(pattern: String, value: Int) {
        val data = "$pattern$value"
        btService.send(data, true)
        itemsAdapter.updateItem(pattern, value)
    }

    companion object {
        private const val BRUTUS_ADDRESS = "3C:71:BF:0F:F5:32"
        private const val BRUTUS_DEVICE_NAME = "BrutusV2"

        // Events
        private const val INTERN_LED_PATTERN = "internLed"
        private const val LANTERN_PATTERN = "lantern"
        private const val TEMP_PATTERN = "temp"
        private const val ALARM_PATTERN = "alarm"
        private const val USB_PORT_PATTERN = "usbPort"
        private const val MODULES_PATTERN = "modules"
    }

}
