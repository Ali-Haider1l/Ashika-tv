package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.AshikaAuthGate
import com.example.ui.theme.AshikaLiveTvTheme

class MainActivity : ComponentActivity() {

    private var isInSystemPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInSystemPipMode = isInPictureInPictureMode
        }

        addOnPictureInPictureModeChangedListener { info ->
            isInSystemPipMode = info.isInPictureInPictureMode
        }

        setContent {
            AshikaLiveTvTheme {
                AshikaAuthGate(isInSystemPipMode = isInSystemPipMode)
            }
        }
    }
}
