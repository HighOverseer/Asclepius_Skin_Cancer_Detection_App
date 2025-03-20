package com.dicoding.asclepius.presentation.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.findFragment
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.presentation.adapter.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private lateinit var sectionsPagerAdapter:SectionsPagerAdapter

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

        sectionsPagerAdapter = SectionsPagerAdapter(this)
        binding.apply {
            viewPager.adapter = sectionsPagerAdapter

            tabs.addOnTabSelectedListener(onTabSelectedListener)

            TabLayoutMediator(tabs, viewPager){ tab, position ->
                val pageIndexes = SectionsPagerAdapter.PageIndex.entries
                tab.text = getString(pageIndexes[position].titleResId)
            }.attach()

            viewPager.setCurrentItem(
                SectionsPagerAdapter.PageIndex.PREDICTION.pageIndex,
                false
            )
        }

        supportActionBar?.elevation = 0f
    }

    private val onTabSelectedListener = object : OnTabSelectedListener{
        override fun onTabSelected(tab: TabLayout.Tab?) {
            setSearchBarAnimationInPredictionHistoryFragment(true, tab)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            setSearchBarAnimationInPredictionHistoryFragment(false, tab)
        }

        private fun setSearchBarAnimationInPredictionHistoryFragment(isSlideIn:Boolean, tab: Tab?) {
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

}