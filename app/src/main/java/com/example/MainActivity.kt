package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.database.AppDatabase
import com.example.data.repository.DetoxRepository
import com.example.ui.DetoxDashboard
import com.example.ui.DetoxViewModel
import com.example.ui.DetoxViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database, DAO and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DetoxRepository(database.detoxSessionDao())

        // Feed Repository to the ViewModel using our custom VM Factory
        val viewModel: DetoxViewModel by viewModels {
            DetoxViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                DetoxDashboard(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
