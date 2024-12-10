package com.example.yoga.ViewModel

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.yoga.R

class ButtonAdapter(
    private val context: Context,
    private val poseNames: Array<String>,
    private val onButtonClick: (String) -> Unit
) : RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder>() {

    private var selectedIndex: Int = 0
    private val buttonReferences = mutableMapOf<Int, Button>()

    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.buttonItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.button_item, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val poseName = poseNames[position]
        holder.button.text = poseName
        holder.button.setBackgroundColor(if (position == selectedIndex) Color.rgb(10, 240, 5) else Color.BLUE)
        holder.button.setTextColor(if (position == selectedIndex) Color.rgb(60, 60, 0) else Color.WHITE)
        holder.button.setShadowLayer(if (position == selectedIndex) 15f else 0f, 5f, 5f, if (position == selectedIndex) Color.WHITE else 0)

        holder.button.setOnClickListener {
            onButtonClick(poseName)
            setSelectedIndex(position) // Update selected index on button click

        }

        // 確保按鈕引用是唯一的
        // Ensure button references are unique and correctly populated
        buttonReferences[position] = holder.button
    }

    override fun getItemCount(): Int {
        return poseNames.size
    }

    // Function 1: 根據索引獲取按鈕的參考
    fun getButtonByIndex(index: Int): Button {
        return buttonReferences[index] ?: throw IllegalArgumentException("Button not found for index $index")
    }

    // Function 2: 根據按鈕的參考獲取它的索引
    fun getIndexByButton(button: Button): Int {
        for ((index, btn) in buttonReferences) {
            if (btn == button) {
                return index
            }
        }
        throw IllegalArgumentException("Button not found in map")
    }
    
    fun setSelectedIndex(index: Int) {
        val previousIndex = selectedIndex
        selectedIndex = index
        notifyItemChanged(previousIndex)
        notifyItemChanged(selectedIndex)
    }
}