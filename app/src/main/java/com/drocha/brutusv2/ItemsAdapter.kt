package com.drocha.brutusv2

import android.graphics.Color
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

sealed class Item(
    val name: String,
    val pattern: String,
    val color: Int,
    val title: String,
    var showProgress: Boolean = false
) {

    class PushButtonItem(name: String, pattern: String, color: Int = -1, title: String = "") :
        Item(name, pattern, color, title)

    class SwitchItem(name: String, pattern: String, color: Int = -1, title: String = "") :
        Item(name, pattern, color, title) {
        var isChecked = false
    }

    class SeekBarItem(name: String, pattern: String, color: Int = -1, title: String = "") :
        Item(name, pattern, color, title) {
        var progress = 0
        var maxProgress = 7
    }

    class OutputItem(name: String, pattern: String, color: Int = -1, title: String = "") :
        Item(name, pattern, color, title) {
        var value = ""
        var textColor: Int = Color.BLACK
    }

    class NumberInputItem(name: String, pattern: String, color: Int = -1, title: String = "") :
        Item(name, pattern, color, title) {
        var value = ""
    }
}

class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val itemTitle = view.findViewById<TextView>(R.id.itemTitle)!!
    val itemHeader = view.findViewById<TextView>(R.id.itemHeader)!!
    val cardView = view.findViewById<CardView>(R.id.cardView)!!

    val switch = view.findViewById<Switch?>(R.id.sw)
    val seekBar = view.findViewById<SeekBar?>(R.id.seekbar)
    val seekBarPercent = view.findViewById<TextView?>(R.id.seekbarPercent)
    val itemOutput = view.findViewById<TextView?>(R.id.itemOutput)
    val pushButton = view.findViewById<Button?>(R.id.itemAction)
    val progressBar = view.findViewById<ProgressBar?>(R.id.progressBar)
    val inputItemAction = view.findViewById<EditText?>(R.id.inputItemAction)
}

class ItemsAdapter(private val items: MutableList<Item>, private val listener: ItemValueChange) :
    RecyclerView.Adapter<ItemViewHolder>() {

    interface ItemValueChange {
        fun onSwitchItemValueChanged(pattern: String, isChecked: Boolean)
        fun onSeekBarItemValueChanged(pattern: String, value: Int)
        fun onEventItemClick(item: Item)
    }

    fun updateModuleItems(moduleData: ModuleData) {
        moduleData.commandTypeList.forEach {

            val newItem = createItem(it, moduleData)

            val itemFound = findItemByPattern(newItem.pattern)
            itemFound?.let {
                val position = items.indexOf(itemFound)

                when (itemFound) {
                    is Item.OutputItem -> {
                        val oldItem = items[position]
                        if (oldItem is Item.OutputItem &&
                            newItem is Item.OutputItem &&
                            oldItem.value != newItem.value
                        ) {
                            items[position] = newItem
                            notifyItemChanged(position)
                        }
                    }
                    is Item.SwitchItem -> {
                        val oldItem = items[position]
                        if (oldItem is Item.SwitchItem &&
                            newItem is Item.SwitchItem &&
                            oldItem.isChecked != newItem.isChecked
                        ) {
                            items[position] = newItem
                            notifyItemChanged(position)
                        }
                    }
                }
            } ?: run {
                items.add(newItem)
                notifyItemInserted(items.size - 1)
            }

        }
    }

    fun removeModuleItems(moduleData: ModuleData) {
        moduleData.commandTypeList.forEach {

            val newItem = createItem(it, moduleData)

            val itemFound = findItemByPattern(newItem.pattern)
            itemFound?.let {
                val position = items.indexOf(itemFound)

                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    private fun createItem(
        it: CommandType,
        moduleData: ModuleData
    ): Item {
        return when (it) {
            is CommandType.ActionSwitch -> {
                val pattern = "${moduleData.id}:${it.command}"
                val swItem = Item.SwitchItem(it.label, pattern, moduleData.color, moduleData.name)
                swItem.isChecked = it.value.toBoolean()
                swItem
            }
            is CommandType.OutputValue -> {
                val pattern = "${moduleData.id}:${it.label}"
                val outputItem =
                    Item.OutputItem(it.label, pattern, moduleData.color, moduleData.name)
                outputItem.value = it.value
                outputItem
            }
            is CommandType.ActionPushButton -> {
                val pattern = "${moduleData.id}:${it.command}"
                Item.PushButtonItem(it.label, pattern, moduleData.color, moduleData.name)
            }
        }
    }

    fun updateSwitchItem(pattern: String, isChecked: Boolean) {
        validateItem<Item.SwitchItem>(pattern) {
            it.showProgress = false
            if (it.isChecked != isChecked) {
                it.isChecked = isChecked
            }
            notifyItemChanged(items.indexOf(it))
        }
    }

    fun updateSeekBarItem(pattern: String, progress: Int) {
        validateItem<Item.SeekBarItem>(pattern) {
            it.showProgress = false
            if (it.progress != progress) {
                it.progress = progress
            }
            notifyItemChanged(items.indexOf(it))
        }
    }

    fun updateOutputItem(pattern: String, value: String) {
        validateItem<Item.OutputItem>(pattern) {
            if (it.value != value) {
                it.value = value
                notifyItemChanged(items.indexOf(it))
            }
        }
    }

    private fun findItemByPattern(pattern: String): Item? {
        return items.firstOrNull { it.pattern == pattern }
    }

    private inline fun <reified T : Item> validateItem(
        pattern: String,
        onValidate: (item: T) -> Unit
    ) {
        val item = items.find { it.pattern == pattern }
        (item as? T)?.let {
            onValidate.invoke(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val viewId = when (viewType) {
            SWITCH -> R.layout.recycler_switch_item
            SEEK_BAR -> R.layout.recycler_seekbar_item
            OUTPUT -> R.layout.recycler_output_item
            PUSH_BUTTON -> R.layout.recycler_pushbutton_item
            NUMBER_INPUT -> R.layout.recycler_number_input_item
            else -> -1
        }

        val view = LayoutInflater.from(parent.context).inflate(viewId, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemTitle.text = item.name

        holder.switch?.setOnCheckedChangeListener(null)
        holder.seekBar?.setOnSeekBarChangeListener(null)
        holder.pushButton?.setOnClickListener(null)

        holder.cardView.setCardBackgroundColor(if (item.color != -1) item.color else Color.WHITE)
        holder.itemHeader.visibility = if (item.title.isNotEmpty()) View.VISIBLE else View.GONE
        holder.itemHeader.text = item.title
        holder.progressBar?.visibility = if (item.showProgress) View.VISIBLE else View.GONE

        when (item) {
            is Item.SwitchItem -> {
                holder.switch?.isChecked = item.isChecked
                holder.switch?.setOnCheckedChangeListener { _, isChecked ->
                    listener.onSwitchItemValueChanged(item.pattern, isChecked)
                    item.showProgress = true
                    notifyItemChanged(position)
                }
            }
            is Item.SeekBarItem -> {
                holder.seekBar?.max = item.maxProgress
                holder.seekBar?.progress = item.progress
                val progress = item.progress * 100 / item.maxProgress
                holder.seekBarPercent?.text = "$progress%"

                holder.seekBar?.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        val sProgress = p0?.progress!! * 100 / item.maxProgress
                        holder.seekBarPercent?.text = "$sProgress%"
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        // unused
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        val actualProgress = p0?.progress!!
                        listener.onSeekBarItemValueChanged(item.pattern, actualProgress)
                        item.progress = actualProgress
                        item.showProgress = true
                        notifyItemChanged(position)
                    }

                })
            }
            is Item.OutputItem -> {
                holder.itemOutput?.text = item.value
                holder.itemOutput?.setTextColor(item.textColor)
            }
            is Item.PushButtonItem -> {
                holder.pushButton?.setOnClickListener { listener.onEventItemClick(item) }
            }
            is Item.NumberInputItem -> {
                holder.pushButton?.setOnClickListener {
                    item.value = holder.inputItemAction?.text.toString()
                    holder.pushButton.isFocusable = true
                    holder.pushButton.isFocusableInTouchMode = true

                    listener.onEventItemClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.SwitchItem -> SWITCH
            is Item.SeekBarItem -> SEEK_BAR
            is Item.OutputItem -> OUTPUT
            is Item.PushButtonItem -> PUSH_BUTTON
            is Item.NumberInputItem -> NUMBER_INPUT
        }
    }

    companion object {
        private const val SWITCH = 0
        private const val SEEK_BAR = 1
        private const val OUTPUT = 2
        private const val PUSH_BUTTON = 3
        private const val NUMBER_INPUT = 4
    }

}