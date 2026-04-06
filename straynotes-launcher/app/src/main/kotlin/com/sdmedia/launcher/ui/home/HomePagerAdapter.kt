package com.sdmedia.launcher.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Number of home screen pages — could be made dynamic via prefs
    private var pageCount: Int = 1

    override fun getItemCount(): Int = pageCount

    override fun createFragment(position: Int): Fragment = HomePageFragment.newInstance(position)

    fun setPageCount(count: Int) {
        pageCount = count
        notifyDataSetChanged()
    }
}
