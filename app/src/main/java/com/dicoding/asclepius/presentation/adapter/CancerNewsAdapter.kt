package com.dicoding.asclepius.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.databinding.HeaderCancerNewsBinding
import com.dicoding.asclepius.databinding.ItemCancerNewsBinding
import com.dicoding.asclepius.domain.model.CancerNewsPreview
import com.dicoding.asclepius.presentation.utils.loadImage

class CancerNewsAdapter(
    private val cancerNewsList: List<CancerNewsPreview>,
    private val onItemClicked: (CancerNewsPreview) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val headerItemType = ItemType.Header
    private val contentItemType = ItemType.Content(HEADER_COUNT until cancerNewsList.count())

    class CancerNewsContentViewHolder(
        val binding: ItemCancerNewsBinding,
        clickedAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                clickedAtPosition(absoluteAdapterPosition)
            }
        }
    }


    class CancerNewsHeaderViewHolder(
        binding: HeaderCancerNewsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            headerItemType.typeId -> CancerNewsHeaderViewHolder(
                HeaderCancerNewsBinding.inflate(
                    inflater,
                    parent,
                    false
                ),

                )

            else -> CancerNewsContentViewHolder(
                ItemCancerNewsBinding.inflate(
                    inflater,
                    parent,
                    false
                ),
                clickedAtPosition = { position ->
                    onItemClicked(cancerNewsList[getRealAdapterItemPosition(position)])
                }
            )
        }
    }

    private fun getRealAdapterItemPosition(absoluteAdapterPosition: Int): Int {
        val itemPosition = absoluteAdapterPosition - HEADER_COUNT
        return if (itemPosition < 0) 0 else itemPosition
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is CancerNewsContentViewHolder -> {
                val item = cancerNewsList[getRealAdapterItemPosition(position)]

                holder.binding.apply {
                    tvTitle.text = item.title
                    tvDescription.text = item.description
                    tvAuthor.text = item.author
                    imageView.loadImage(item.imageUrl)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            in headerItemType.positions -> headerItemType.typeId
            in contentItemType.positions -> contentItemType.typeId
            else -> super.getItemViewType(position)
        }
    }


    override fun getItemCount(): Int {
        return HEADER_COUNT + cancerNewsList.count()
    }

    sealed class ItemType(val typeId: Int, val positions: IntRange) {
        data object Header : ItemType(HEADER_ITEM_ID, HEADER_POSITIONS)
        class Content(positions: IntRange) : ItemType(CONTENT_ITEM_ID, positions)
    }

    companion object {
        const val HEADER_COUNT = 1
        private val HEADER_POSITIONS = 0..0

        private const val HEADER_ITEM_ID = 100
        private const val CONTENT_ITEM_ID = 200
    }
}