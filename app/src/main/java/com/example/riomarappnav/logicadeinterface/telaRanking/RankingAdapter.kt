package com.example.riomarappnav.logicadeinterface.telaRanking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.riomarappnav.R
import com.example.riomarappnav.database.FirestoreRepository

class RankingAdapter(
    private val userList: List<FirestoreRepository.UserData>
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfilePictureRanking: ImageView = itemView.findViewById(R.id.ivProfilePictureRanking)
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
        user.trophies.toString().also { holder.tvTrophies.text = it }
        (position + 1).toString().also { holder.tvPosition.text = it }

        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl)
            .transform(CircleCrop())
            .into(holder.ivProfilePictureRanking)
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}
