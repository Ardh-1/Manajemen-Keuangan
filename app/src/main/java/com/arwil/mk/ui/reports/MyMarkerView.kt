package com.arwil.mk.ui.reports // Pastikan package ini sesuai

import android.content.Context
import android.graphics.PorterDuff
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.arwil.mk.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class MyMarkerView(
    context: Context,
    layoutResource: Int,
    private val valueFormatter: (Float) -> String // Terima fungsi formatter
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val rootLayout: LinearLayout = findViewById(R.id.marker_root)
    // Ambil drawable background
    private val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.background_tooltip)!!

    init {
        // Atur warna awal (opsional, tapi bagus untuk default)
        setColor(ContextCompat.getColor(context, R.color.red))
    }

    // Dipanggil setiap kali marker digambar ulang
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return
        // Gunakan fungsi formatter yang dikirim untuk mengatur teks
        tvContent.text = valueFormatter(e.y)
        super.refreshContent(e, highlight)
    }

    // Mengatur posisi marker (di tengah atas titik)
    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }

    fun setColor(color: Int) {
        // Gunakan setColorFilter untuk mewarnai drawable
        backgroundDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        rootLayout.background = backgroundDrawable
    }
}