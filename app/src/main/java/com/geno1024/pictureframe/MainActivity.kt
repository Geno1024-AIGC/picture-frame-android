package com.geno1024.pictureframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.geno1024.pictureframe.ui.EditorScreen
import com.geno1024.pictureframe.ui.theme.PictureFrameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PictureFrameTheme {
                EditorScreen()
            }
        }
    }
}
