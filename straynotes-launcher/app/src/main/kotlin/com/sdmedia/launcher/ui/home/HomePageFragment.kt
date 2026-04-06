package com.sdmedia.launcher.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.sdmedia.launcher.databinding.FragmentHomePageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomePageFragment : Fragment() {

    companion object {
        private const val ARG_PAGE = "page"

        fun newInstance(page: Int) = HomePageFragment().apply {
            arguments = Bundle().apply { putInt(ARG_PAGE, page) }
        }
    }

    private val viewModel: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomePageBinding? = null
    private val binding get() = _binding!!
    private var page: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = arguments?.getInt(ARG_PAGE) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeGrid.page = page

        lifecycleScope.launch {
            viewModel.allApps.collect { apps ->
                val pageApps = apps.filter { /* filter by grid_page == page eventually */ true }
                // Grid populates itself from the app list
                binding.homeGrid.setApps(pageApps, page)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
