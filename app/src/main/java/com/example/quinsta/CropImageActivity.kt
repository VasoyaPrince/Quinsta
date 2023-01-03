package com.example.quinsta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.quinsta.crop.demo.ImageCropDemo
import com.example.quinsta.ui.theme.QuinstaTheme

class CropImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuinstaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background,
                ) {
                    Scaffold(topBar = {
                        TopAppBar(
                            backgroundColor = Color(0xff32485F),
                            modifier = Modifier.padding(0.dp),
                            title = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_back),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .clickable {
                                                onBackPressedDispatcher.onBackPressed()
                                            },
                                    )
                                }
                            },
                            elevation = 0.dp
                        )
                    }, content = {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxWidth()
                        ) {
                            ImageCropDemo()
                        }
                    })
                }
            }
        }
    }
}

