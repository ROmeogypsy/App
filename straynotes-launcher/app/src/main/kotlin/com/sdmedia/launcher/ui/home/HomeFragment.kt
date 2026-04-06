package com.sdmedia.launcher.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.sdmedia.launcher.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        const val TAG = "HomeFragment"
    }

    private val viewModel: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: HomePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        binding.pageIndicator.attachToViewPager(binding.viewPager)

        // Set up clock widget (already inflates itself)


        observeApps()
    }

    private fun observeApps() {
        lifecycleScope.launch {
            viewModel.dockApps.collect { apps ->
                binding.dock.setApps(apps)
            }
        }
        lifecycleScope.launch {
            viewModel.predictiveApps.collect { apps ->
                binding.dock.setPredictiveApps(apps)
            }
        }
    }

    fun nextPage() {
        val current = binding.viewPager.currentItem
        val max = pagerAdapter.itemCount - 1
        if (current < max) binding.viewPager.currentItem = current + 1
    }

    fun prevPage() {
        val current = binding.viewPager.currentItem
        if (current > 0) binding.viewPager.currentItem = current - 1
    }

    fun scrollToPage(page: Int) {
        binding.viewPager.setCurrentItem(page, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
