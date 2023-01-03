package com.example.quinsta.model

import androidx.compose.ui.graphics.Color

data class GridModal(
    val featureUImage: Int,
    val featureName: String,
    val backGround: Color,
    val activity: Class<*>
)