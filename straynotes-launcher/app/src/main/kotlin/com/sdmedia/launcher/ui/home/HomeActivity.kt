package com.sdmedia.launcher.ui.home

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import com.sdmedia.launcher.R
import com.sdmedia.launcher.databinding.ActivityHomeBinding
import com.sdmedia.launcher.receiver.LauncherDeviceAdmin
import com.sdmedia.launcher.ui.drawer.AppDrawerFragment
import com.sdmedia.launcher.ui.common.DragController
import com.sdmedia.launcher.util.Gesture
import com.sdmedia.launcher.util.GestureHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityHomeBinding
    private lateinit var gestureHelper: GestureHelper
    lateinit var dragController: DragController

    private var drawerFragment: AppDrawerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge — launcher owns the full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gestureHelper = GestureHelper(this, ::onGesture)
        dragController = DragController(this)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.home_container, HomeFragment(), HomeFragment.TAG)
            }
        }

        observeState()
    }

    private fun observeState() {
        viewModel.syncApps()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureHelper.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun onGesture(gesture: Gesture) {
        when (gesture) {
            Gesture.SWIPE_UP    -> openDrawer()
            Gesture.SWIPE_DOWN  -> expandNotifications()
            Gesture.DOUBLE_TAP  -> lockScreen()
            Gesture.SWIPE_LEFT  -> nextPage()
            Gesture.SWIPE_RIGHT -> prevPage()
        }
    }

    fun openDrawer() {
        val existing = supportFragmentManager.findFragmentByTag(AppDrawerFragment.TAG) as? AppDrawerFragment
        if (existing != null) {
            existing.open()
            return
        }
        val fragment = AppDrawerFragment()
        drawerFragment = fragment
        supportFragmentManager.commit {
            add(R.id.drawer_container, fragment, AppDrawerFragment.TAG)
        }
        // open() called from fragment's onViewCreated
    }

    fun closeDrawer() {
        val fragment = supportFragmentManager.findFragmentByTag(AppDrawerFragment.TAG) as? AppDrawerFragment
        fragment?.close()
    }

    fun isDrawerOpen(): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(AppDrawerFragment.TAG) as? AppDrawerFragment
        return fragment?.isOpen() == true
    }

    private fun nextPage() {
        val home = supportFragmentManager.findFragmentByTag(HomeFragment.TAG) as? HomeFragment
        home?.nextPage()
    }

    private fun prevPage() {
        val home = supportFragmentManager.findFragmentByTag(HomeFragment.TAG) as? HomeFragment
        home?.prevPage()
    }

    /** Pull notification shade via StatusBarManager reflection. */
    private fun expandNotifications() {
        try {
            val service = getSystemService("statusbar")
            val cls = Class.forName("android.app.StatusBarManager")
            val method = cls.getMethod("expandNotificationsPanel")
            method.invoke(service)
        } catch (e: Exception) {
            // Fallback: do nothing — do NOT open settings
        }
    }

    /** Lock via DevicePolicyManager if active, else no-op. */
    private fun lockScreen() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, LauncherDeviceAdmin::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
        }
        // Without admin active, guide user via settings — no crash
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer()
        }
        // Suppress default back on home screen — do not exit
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Home button pressed while already on home — close drawer, go to page 0
        if (intent?.action == Intent.ACTION_MAIN) {
            closeDrawer()
            val home = supportFragmentManager.findFragmentByTag(HomeFragment.TAG) as? HomeFragment
            home?.scrollToPage(0)
        }
    }
}
