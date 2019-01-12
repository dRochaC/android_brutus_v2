package com.drocha.brutusv2

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
        items.add(Item.OutputItem("Consumo total", BACKPACK_CONSUMPTION_PATTERN).apply {
            textColor = ContextCompat.getColor(this@MainActivity, R.color.red)
        })
        items.add(Item.OutputItem("Energia solar", SOLAR_CONSUMPTION_PATTERN).apply {
            textColor = ContextCompat.getColor(this@MainActivity, R.color.green)
        })
        items.add(Item.SwitchItem("Luz interna", INTERN_LED_PATTERN))
        items.add(Item.SwitchItem("Lanterna frontal", FRONT_LANTERN_PATTERN))
        items.add(Item.SwitchItem("Lanterna traseiro", BACK_LANTERN_PATTERN))
        items.add(Item.SeekBarItem("Volume", VOLUME_PATTERN).apply { maxProgress = 30 })
        items.add(Item.SwitchItem("Porta USB", USB_PORT_PATTERN))
        items.add(Item.SwitchItem("Descoberta de módulos", MODULES_PATTERN))
        items.add(Item.SwitchItem("Wifi", WIFI_PATTERN))
        items.add(Item.SwitchItem("OTA", OTA_PATTERN))
        items.add(Item.PushButtonItem("Modo Iron Man", IRON_MAN_MODE_PATTERN))
        items.add(Item.PushButtonItem("Parar música", STOP_MUSIC_PATTERN))
        items.add(Item.PushButtonItem("Restart", RESTART_PATTERN))

        itemsAdapter = ItemsAdapter(items, this)
        itemsRecyclerView.adapter = itemsAdapter
        itemsRecyclerView.itemAnimator = null
    }

    private fun prepareEventsHandlers() {
        eventHandler.prepare()
        eventHandler.onConnectEvent {
            btLoading.visibility = View.GONE

            tempTitle.visibility = View.VISIBLE
            tempValue.visibility = View.VISIBLE
            itemsRecyclerView.visibility = View.VISIBLE
            isPlayingTitle.visibility = View.VISIBLE
            wifiTitle.visibility = View.VISIBLE
            otaTitle.visibility = View.VISIBLE
        }

        eventHandler.onDisconnectEvent {
            disconnectLoading.visibility = View.VISIBLE
        }

        eventHandler.onReconnectEvent {
            disconnectLoading.visibility = View.GONE
        }

        eventHandler.onModule1Event { moduleData, withError ->
            if (withError) {
                itemsAdapter.removeModuleItems(moduleData)
            } else {
                itemsAdapter.updateModuleItems(moduleData)
            }
        }

        subscribeEventBooleanWithAdapter(ALARM_PATTERN)
        subscribeEventBooleanWithAdapter(INTERN_LED_PATTERN)
        subscribeEventBooleanWithAdapter(FRONT_LANTERN_PATTERN)
        subscribeEventBooleanWithAdapter(BACK_LANTERN_PATTERN)
        subscribeEvent(VOLUME_PATTERN) {
            itemsAdapter.updateSeekBarItem(
                VOLUME_PATTERN, it.toInt()
            )
        }
        subscribeEventBooleanWithAdapter(USB_PORT_PATTERN)
        subscribeEventBooleanWithAdapter(MODULES_PATTERN)
        subscribeEvent(DF_PLAYER_PATTERN) {
            setColoredText(
                !it.toBoolean(), isPlayingTitle, "is playing music", "idle"
            )
        }
        subscribeEvent(WIFI_NAME_PATTERN) {
            val isEmpty = it == " " || it.isEmpty()
            setColoredText(
                !isEmpty, wifiTitle, it, "no wifi connection"
            )
        }
        subscribeEventBooleanWithAdapter(WIFI_PATTERN)
        subscribeEvent(OTA_PATTERN) {
            setColoredText(
                it.toBoolean(), otaTitle, "OTA enabled", "OTA disabled"
            )
            itemsAdapter.updateSwitchItem(
                OTA_PATTERN, it.toBoolean()
            )
        }
        subscribeEvent(TEMP_PATTERN) {
            tempValue.text = "$it ºC"
        }
        subscribeEvent(BACKPACK_CONSUMPTION_PATTERN) {
            itemsAdapter.updateOutputItem(
                BACKPACK_CONSUMPTION_PATTERN, "$it mA"
            )
        }
        subscribeEvent(SOLAR_CONSUMPTION_PATTERN) {
            itemsAdapter.updateOutputItem(
                SOLAR_CONSUMPTION_PATTERN, "$it mA"
            )
        }
    }

    private fun setColoredText(
        status: Boolean,
        textView: TextView,
        trueText: String,
        elseText: String
    ) {
        val color = if (status) {
            textView.text = trueText
            R.color.colorPrimary
        } else {
            textView.text = elseText
            R.color.red
        }
        textView.setTextColor(ContextCompat.getColor(this@MainActivity, color))
    }

    private fun MainActivity.subscribeEvent(pattern: String, callback: (String) -> Unit) {
        eventHandler.subscribeToEvent(pattern, callback)
    }

    private fun MainActivity.subscribeEventBooleanWithAdapter(pattern: String) {
        eventHandler.subscribeToEvent(pattern) {
            itemsAdapter.updateSwitchItem(
                pattern, it.toBoolean()
            )
        }
    }

    override fun onSwitchItemValueChanged(pattern: String, isChecked: Boolean) {
        val state = if (isChecked) "1" else "0"
        val data = "$pattern$state"
        btService.send(data, true)
    }

    override fun onSeekBarItemValueChanged(pattern: String, value: Int) {
        val data = "$pattern$value"
        btService.send(data, true)
    }

    override fun onPushButtonItemClick(item: Item) {
        val data = "${item.pattern}$1"
        btService.send(data, true)
        Toast.makeText(this, "Ação: ${item.name}", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val BRUTUS_ADDRESS = "3C:71:BF:0F:F5:32"
        private const val BRUTUS_DEVICE_NAME = "BrutusV2"

        // Events
        private const val INTERN_LED_PATTERN = "internLed"
        private const val FRONT_LANTERN_PATTERN = "frontLantern"
        private const val BACK_LANTERN_PATTERN = "backLantern"
        private const val TEMP_PATTERN = "temp"
        private const val ALARM_PATTERN = "alarm"
        private const val USB_PORT_PATTERN = "usbPort"
        private const val MODULES_PATTERN = "modules"
        private const val VOLUME_PATTERN = "volume"
        private const val STOP_MUSIC_PATTERN = "stopMusic"
        private const val IRON_MAN_MODE_PATTERN = "ironManMode"
        private const val DF_PLAYER_PATTERN = "dfPlayer"
        private const val WIFI_PATTERN = "wifi"
        private const val WIFI_NAME_PATTERN = "wifiName"
        private const val OTA_PATTERN = "ota"
        private const val RESTART_PATTERN = "espRestart"
        private const val BACKPACK_CONSUMPTION_PATTERN = "backpackConsumption"
        private const val SOLAR_CONSUMPTION_PATTERN = "solarConsumption"
    }

}
