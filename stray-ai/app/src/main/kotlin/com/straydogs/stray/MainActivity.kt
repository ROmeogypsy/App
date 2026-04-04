package com.straydogs.stray

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.straydogs.stray.ui.navigation.StrayNavGraph
import com.straydogs.stray.ui.theme.StrayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrayTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StrayNavGraph()
                }
            }
        }
    }
}
