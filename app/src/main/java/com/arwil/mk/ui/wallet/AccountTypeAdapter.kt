package com.arwil.mk.ui.wallet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arwil.mk.R

class AccountTypeAdapter(
    private val types: List<AccountType>,
    private val onItemClick: (AccountType) -> Unit
) : RecyclerView.Adapter<AccountTypeAdapter.TypeViewHolder>() {
    class TypeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_account_type_icon)
        val name: TextView = view.findViewById(R.id.tv_account_type_name)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_type, parent, false)
        return TypeViewHolder(view)
    }
    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        val type = types[position]
        holder.name.text = type.name
        holder.icon.setImageResource(type.iconResId)
        holder.itemView.setOnClickListener { onItemClick(type) }
    }
    override fun getItemCount() = types.size
}