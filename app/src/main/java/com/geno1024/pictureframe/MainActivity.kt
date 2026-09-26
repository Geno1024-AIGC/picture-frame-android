package com.geno1024.pictureframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.geno1024.pictureframe.ui.theme.PictureFrameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PictureFrameTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("相框")
                }
            }
        }
    }
}
