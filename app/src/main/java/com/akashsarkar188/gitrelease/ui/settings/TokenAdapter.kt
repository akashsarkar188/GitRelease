package com.akashsarkar188.gitrelease.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.akashsarkar188.gitrelease.R
import com.akashsarkar188.gitrelease.data.local.entity.GithubToken

class TokenAdapter(
    private val onDeleteClick: (GithubToken) -> Unit
) : ListAdapter<GithubToken, TokenAdapter.TokenViewHolder>(TokenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_token, parent, false)
        return TokenViewHolder(view)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TokenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val btnDeleteToken: ImageButton = itemView.findViewById(R.id.btnDeleteToken)

        fun bind(token: GithubToken) {
            tvUsername.text = token.username
            tvUserEmail.text = token.email ?: "No email provided"
            
            ivUserAvatar.load(token.avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_circle)
                error(R.drawable.bg_circle)
                transformations(CircleCropTransformation())
            }

            btnDeleteToken.setOnClickListener {
                onDeleteClick(token)
            }
        }
    }

    class TokenDiffCallback : DiffUtil.ItemCallback<GithubToken>() {
        override fun areItemsTheSame(oldItem: GithubToken, newItem: GithubToken): Boolean {
            return oldItem.accessToken == newItem.accessToken
        }

        override fun areContentsTheSame(oldItem: GithubToken, newItem: GithubToken): Boolean {
            return oldItem == newItem
        }
    }
}
