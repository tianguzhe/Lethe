package io.github.xiaolong125.lethe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.xiaolong125.lethe.ui.MainScreen
import io.github.xiaolong125.lethe.ui.theme.LetheTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      LetheTheme {
        // Surface provides both the background and the default content color for all text below.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainScreen() }
      }
    }
  }
}
