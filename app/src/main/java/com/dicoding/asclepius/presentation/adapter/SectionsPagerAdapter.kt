package com.dicoding.asclepius.presentation.adapter

import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dicoding.asclepius.R
import com.dicoding.asclepius.presentation.view.InformationFragment
import com.dicoding.asclepius.presentation.view.PredictionFragment
import com.dicoding.asclepius.presentation.view.PredictionHistoryFragment
import java.lang.ref.WeakReference

class SectionsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    private val fragmentMap = SparseArray<WeakReference<Fragment>>()

    override fun getItemCount(): Int {
        return PageIndex.entries.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment = when (position) {
            PageIndex.HISTORY.pageIndex -> PredictionHistoryFragment()
            PageIndex.PREDICTION.pageIndex -> PredictionFragment()
            else -> InformationFragment()
        }
        fragmentMap.put(position, WeakReference(fragment))

        return fragment
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        fragmentMap.clear()
    }

    fun getFragment(position: Int): Fragment? {
        return fragmentMap[position]?.get()
    }

    enum class PageIndex(val pageIndex: Int, val titleResId: Int) {
        HISTORY(0, R.string.riwayat),
        PREDICTION(1, R.string.prediksi),
        INFORMATION(2, R.string.informasi)
    }
}