package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.ScriptRepository
import com.example.ui.FountainStudioScreen
import com.example.ui.FountainViewModel
import com.example.ui.theme.FountainStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ScriptRepository(
            scriptDao = database.scriptDao(),
            appContext = applicationContext
        )

        setContent {
            val fountainViewModel: FountainViewModel = viewModel(
                factory = FountainViewModel.Factory(repository)
            )
            val uiState by fountainViewModel.uiState.collectAsStateWithLifecycle()

            FountainStudioTheme(darkTheme = uiState.isDarkMode) {
                FountainStudioScreen(viewModel = fountainViewModel)
            }
        }
    }
}
