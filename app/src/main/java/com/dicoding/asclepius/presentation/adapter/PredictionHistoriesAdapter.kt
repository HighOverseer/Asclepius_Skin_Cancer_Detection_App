package com.dicoding.asclepius.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dicoding.asclepius.databinding.ItemPredictionHistoryBinding
import com.dicoding.asclepius.domain.model.PredictionHistory
import com.dicoding.asclepius.presentation.utils.formatToPercentage
import com.dicoding.asclepius.presentation.utils.loadImage

class PredictionHistoriesAdapter(
    private val onItemClick: (PredictionHistory) -> Unit
) : PagingDataAdapter<PredictionHistory, PredictionHistoriesAdapter.PredictionHistoryViewHolder>(
    DIFF_CALLBACK
) {

    class PredictionHistoryViewHolder(
        val binding: ItemPredictionHistoryBinding,
        clickedAtPosition: (Int) -> Unit
    ) : ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                clickedAtPosition(absoluteAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionHistoryViewHolder {
        val binding = ItemPredictionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PredictionHistoryViewHolder(
            binding = binding,
            clickedAtPosition = { position ->
                getItem(position)?.let {
                    onItemClick(it)
                }
            }
        )
    }

    override fun onBindViewHolder(holder: PredictionHistoryViewHolder, position: Int) {
        val predictionHistory = getItem(position) ?: return

        val uri = Uri.parse(predictionHistory.imageUri)

        holder.binding.apply {
            imageView.loadImage(uri)
            tvLabel.text = predictionHistory.modelOutput.label
            tvConfidence.text = predictionHistory.modelOutput
                .confidenceScore
                .formatToPercentage()

            tvSessionDate.text = predictionHistory.date
            tvSessionName.text = predictionHistory.sessionName
        }
    }


    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PredictionHistory>() {
            override fun areItemsTheSame(
                oldItem: PredictionHistory,
                newItem: PredictionHistory
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PredictionHistory,
                newItem: PredictionHistory
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}