package com.arwil.mk.ui.charts

import android.graphics.PorterDuff
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R
import com.arwil.mk.ui.home.toRupiahFormat
import androidx.core.graphics.drawable.DrawableCompat

class CategorySummaryAdapter(private var summaryList: List<CategorySummary>) :
    RecyclerView.Adapter<CategorySummaryAdapter.SummaryViewHolder>() {

    class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_category_icon)
        val name: TextView = view.findViewById(R.id.tv_category_name)
        val percentage: TextView = view.findViewById(R.id.tv_category_percentage)
        val total: TextView = view.findViewById(R.id.tv_category_total)
        // val colorIndicator: View = view.findViewById(R.id.view_color_indicator)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val item = summaryList[position]
        holder.icon.setImageResource(item.iconResId)
        holder.name.text = item.categoryName
        holder.percentage.text = String.format("%.2f%%", item.percentage)
        holder.total.text = item.totalAmount.toRupiahFormat()
        holder.progressBar.progress = item.percentage.toInt() // Mengatur nilai progress

        // Mengatur warna progress bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(item.color)
        } else {
            // Untuk API level di bawah 21
            holder.progressBar.progressDrawable.setColorFilter(item.color, PorterDuff.Mode.SRC_IN)
        }
    }

    override fun getItemCount() = summaryList.size

    fun updateData(newSummaryList: List<CategorySummary>) {
        summaryList = newSummaryList
        notifyDataSetChanged()
    }
}