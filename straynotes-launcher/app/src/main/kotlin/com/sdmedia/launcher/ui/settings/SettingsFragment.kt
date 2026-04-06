package com.sdmedia.launcher.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.sdmedia.launcher.R
import com.sdmedia.launcher.data.PrefsRepository
import com.sdmedia.launcher.receiver.LauncherDeviceAdmin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var prefsRepository: PrefsRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        findPreference<ListPreference>("pref_grid_columns")?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                prefsRepository.setGridColumns((newValue as String).toInt())
            }
            true
        }

        findPreference<ListPreference>("pref_icon_size")?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                prefsRepository.setIconSizeDp((newValue as String).toInt())
            }
            true
        }

        findPreference<SwitchPreferenceCompat>("pref_show_labels")?.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                prefsRepository.setShowLabels(newValue as Boolean)
            }
            true
        }

        findPreference<SwitchPreferenceCompat>("pref_double_tap_lock")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enable = newValue as Boolean
                if (enable) promptDeviceAdmin() else {
                    lifecycleScope.launch { prefsRepository.setDoubleTapLock(false) }
                }
                true
            }
        }

        // Reflect current device admin state
        val dpm = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(requireContext(), LauncherDeviceAdmin::class.java)
        findPreference<SwitchPreferenceCompat>("pref_double_tap_lock")?.isChecked =
            dpm.isAdminActive(admin)
    }

    private fun promptDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(requireContext(), LauncherDeviceAdmin::class.java))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_explanation))
        }
        startActivity(intent)
    }
}
