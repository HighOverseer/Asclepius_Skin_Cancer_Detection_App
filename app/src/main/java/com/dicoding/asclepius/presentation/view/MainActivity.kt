package com.dicoding.asclepius.presentation.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.presentation.adapter.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PredictionFragment.OnBackFromResultActivityCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    private var initialPageIndex: Int = SectionsPagerAdapter.PageIndex.PREDICTION.pageIndex

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        savedInstanceState?.apply {
            initialPageIndex = getInt(KEY_PAGE_POSITION)
        }

        sectionsPagerAdapter = SectionsPagerAdapter(this)
        binding.apply {
            viewPager.adapter = sectionsPagerAdapter

            tabs.addOnTabSelectedListener(onTabSelectedListener)

            TabLayoutMediator(tabs, viewPager) { tab, position ->
                val pageIndexes = SectionsPagerAdapter.PageIndex.entries
                tab.text = getString(pageIndexes[position].titleResId)
            }.attach()

            viewPager.setCurrentItem(
                initialPageIndex,
                false
            )
        }

        supportActionBar?.elevation = 0f
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE_POSITION, binding.tabs.selectedTabPosition)
    }

    override fun onBackFromResult(isSessionSaved: Boolean) {
        if (isSessionSaved) {
            binding.viewPager.setCurrentItem(SectionsPagerAdapter.PageIndex.HISTORY.pageIndex, true)
        }
    }

    private val onTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab: Tab?) {
            setSearchBarAnimationInPredictionHistoryFragment(true, tab)
        }

        override fun onTabUnselected(tab: Tab?) {
            setSearchBarAnimationInPredictionHistoryFragment(false, tab)
        }

        private fun setSearchBarAnimationInPredictionHistoryFragment(
            isSlideIn: Boolean,
            tab: Tab?
        ) {
            val position = tab?.position ?: return

            if (position == SectionsPagerAdapter.PageIndex.HISTORY.pageIndex) {
                val fragment =
                    sectionsPagerAdapter.getFragment(SectionsPagerAdapter.PageIndex.HISTORY.pageIndex)

                fragment ?: return

                (fragment as? PredictionHistoryFragment)?.animateSlide(isSlideIn = isSlideIn)
            }
        }

        override fun onTabReselected(tab: Tab?) {}
    }


    override fun onDestroy() {
        binding.tabs.removeOnTabSelectedListener(onTabSelectedListener)
        super.onDestroy()
    }

    companion object {
        private const val KEY_PAGE_POSITION = "page_position"
    }

}