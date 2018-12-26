package com.drocha.brutusv2

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

sealed class Item(val name: String, val pattern: String) {
    class SwitchItem(name: String, pattern: String) : Item(name, pattern) {
        var isChecked = false
    }

    class SeekBarItem(name: String, pattern: String) : Item(name, pattern) {
        var progress = 0
    }
}

class ItemsAdapter(private val items: List<Item>, private val listener: ItemValueChange) :
    RecyclerView.Adapter<ItemViewHolder>() {

    interface ItemValueChange {
        fun onSwitchItemValueChanged(pattern: String, isChecked: Boolean)
        fun onSeekBarItemValueChanged(pattern: String, value: Int)
    }

    fun updateItem(pattern: String, isChecked: Boolean) {
        val item = items.find { it.pattern == pattern }
        item?.let {
            (it as? Item.SwitchItem)?.isChecked = isChecked
            notifyDataSetChanged()
        }
    }

    fun updateItem(pattern: String, value: Int) {
        val item = items.find { it.pattern == pattern }
        item?.let {
            (it as? Item.SeekBarItem)?.progress = value
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val viewId = when (viewType) {
            SWITCH -> R.layout.recycler_switch_item
            SEEK_BAR -> R.layout.recycler_seekbar_item
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

        when (item) {
            is Item.SwitchItem -> {
                holder.switch?.isChecked = item.isChecked
                holder.switch?.setOnCheckedChangeListener { _, isChecked ->
                    listener.onSwitchItemValueChanged(item.pattern, isChecked)
                }
            }
            is Item.SeekBarItem -> {
                holder.seekBar?.progress = item.progress
                val progress = item.progress * 10
                holder.seekbarPercent?.text = "$progress%"

                holder.seekBar?.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        val progress = p0?.progress!! * 10
                        holder.seekbarPercent?.text = "$progress%"
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        // unused
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        listener.onSeekBarItemValueChanged(item.pattern, p0?.progress!!)
                    }

                })
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
        }
    }

    companion object {
        private const val SWITCH = 0
        private const val SEEK_BAR = 1
    }

}

class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val itemTitle = view.findViewById<TextView>(R.id.itemTitle)
    val switch = view.findViewById<Switch?>(R.id.sw)
    val seekBar = view.findViewById<SeekBar?>(R.id.seekbar)
    val seekbarPercent = view.findViewById<TextView?>(R.id.seekbarPercent)
}