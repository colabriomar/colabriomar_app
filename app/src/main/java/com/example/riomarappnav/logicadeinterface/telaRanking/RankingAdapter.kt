package com.example.riomarappnav.logicadeinterface.telaRanking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.riomarappnav.R
import com.example.riomarappnav.database.FirestoreRepository

class RankingAdapter(
    private val userList: List<FirestoreRepository.UserData>
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ImageView removido
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        val tvTrophies: TextView = itemView.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val user = userList[position]

        holder.tvName.text = user.name
        holder.tvTrophies.text = user.trophies.toString()
        holder.tvPosition.text = (position + 1).toString()

        // Bloco do Glide removido
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}