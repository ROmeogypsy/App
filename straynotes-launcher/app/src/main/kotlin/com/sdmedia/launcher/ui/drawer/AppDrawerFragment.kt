package com.sdmedia.launcher.ui.drawer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.sdmedia.launcher.data.AppRepository
import com.sdmedia.launcher.databinding.FragmentDrawerBinding
import com.sdmedia.launcher.ui.shortcut.ShortcutMenu
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppDrawerFragment : Fragment() {

    companion object {
        const val TAG = "AppDrawerFragment"
    }

    @Inject lateinit var appRepository: AppRepository

    private val viewModel: AppDrawerViewModel by activityViewModels()
    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppDrawerAdapter
    private var shortcutMenu: ShortcutMenu? = null

    private var _isOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppDrawerAdapter(
            onAppClick = { app ->
                val launchIntent = requireContext().packageManager
                    .getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    lifecycleScope.launch { appRepository.recordLaunch(app.packageName) }
                }
            },
            onAppLongPress = { app, anchorView ->
                shortcutMenu = ShortcutMenu(requireContext())
                shortcutMenu?.show(anchorView, app)
            }
        )

        val gridManager = GridLayoutManager(context, 4).also {
            it.initialPrefetchItemCount = 12
        }
        binding.recyclerView.apply {
            layoutManager = gridManager
            adapter = this@AppDrawerFragment.adapter
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(0, 20)
        }

        lifecycleScope.launch {
            viewModel.filteredApps.collect { apps ->
                adapter.submitList(apps)
            }
        }

        lifecycleScope.launch {
            binding.searchView.queryFlow.collect { query ->
                viewModel.searchQuery.value = query
            }
        }

        // Start below screen, then open
        view.translationY = view.resources.displayMetrics.heightPixels.toFloat()
        open()
    }

    /** Drawer slides up with spring — not linear. */
    fun open() {
        _isOpen = true
        val anim = SpringAnimation(requireView(), DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        }
        anim.start()
    }

    /** Drawer slides down, then fragment is removed. */
    fun close() {
        _isOpen = false
        binding.searchView.clear()
        val targetY = requireView().height.toFloat()
        val anim = SpringAnimation(requireView(), DynamicAnimation.TRANSLATION_Y, targetY).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        }
        anim.addEndListener { _, _, _, _ ->
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        anim.start()
    }

    fun isOpen(): Boolean = _isOpen

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
