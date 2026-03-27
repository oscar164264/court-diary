package com.courtdiary

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.courtdiary.ui.navigation.CourtDiaryNavHost
import com.courtdiary.ui.theme.CourtDiaryTheme
import com.courtdiary.viewmodel.CaseViewModel
import com.courtdiary.viewmodel.PrecedentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CaseViewModel by viewModels()
    private val precedentViewModel: PrecedentViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewModel.seedSampleData()

        setContent {
            val darkMode by viewModel.darkModeEnabled.collectAsState()

            CourtDiaryTheme(darkTheme = darkMode) {
                CourtDiaryNavHost(
                    viewModel = viewModel,
                    precedentViewModel = precedentViewModel
                )
            }
        }
    }
}
