@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.example.quinsta.crop.demo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.example.quinsta.R
import com.example.quinsta.crop.ImageSelectionButton
import com.example.quinsta.crop.preferences.CropStyleSelectionMenu
import com.example.quinsta.crop.preferences.PropertySelectionSheet
import com.example.quinsta.ui.theme.QuinstaTheme
import com.smarttoolfactory.colorpicker.widget.drawChecker
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal enum class SelectionPage {
    Properties, Style
}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImageCropDemo() {
    val imageBitmapLarge = ImageBitmap.imageResource(
        LocalContext.current.resources, R.drawable.landscape5
    )

    var imageBitmap by remember { mutableStateOf(imageBitmapLarge) }

    val bottomSheetScaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
    )

    val defaultImage1 = ImageBitmap.imageResource(id = R.drawable.squircle)
    val defaultImage2 = ImageBitmap.imageResource(id = R.drawable.cloud)
    val defaultImage3 = ImageBitmap.imageResource(id = R.drawable.sun)

    val cropFrameFactory = remember {
        CropFrameFactory(
            listOf(
                defaultImage1, defaultImage2, defaultImage3
            )
        )
    }

    val handleSize: Float = LocalDensity.current.run { 20.dp.toPx() }

    var cropProperties by remember {
        mutableStateOf(
            CropDefaults.properties(
                cropOutlineProperty = CropOutlineProperty(
                    OutlineType.Rect, RectCropShape(0, "Rect")
                ), handleSize = handleSize
            )
        )
    }
    var cropStyle by remember { mutableStateOf(CropDefaults.style()) }
    val coroutineScope = rememberCoroutineScope()

    var selectionPage by remember { mutableStateOf(SelectionPage.Properties) }


    val theme by remember {
        derivedStateOf {
            cropStyle.cropTheme
        }
    }

    QuinstaTheme(
        darkTheme = when (theme) {
            CropTheme.Dark -> true
            CropTheme.Light -> false
            else -> isSystemInDarkTheme()
        }
    ) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetElevation = 16.dp,
            sheetShape = RoundedCornerShape(
                bottomStart = 0.dp, bottomEnd = 0.dp, topStart = 28.dp, topEnd = 28.dp
            ),

            sheetGesturesEnabled = true,
            sheetContent = {

                if (selectionPage == SelectionPage.Properties) {
                    PropertySelectionSheet(cropFrameFactory = cropFrameFactory,
                        cropProperties = cropProperties,
                        onCropPropertiesChange = {
                            cropProperties = it
                        })
                } else {
                    CropStyleSelectionMenu(cropType = cropProperties.cropType,
                        cropStyle = cropStyle,
                        onCropStyleChange = {
                            cropStyle = it
                        })
                }
            },

            // This is the height in collapsed state
            sheetPeekHeight = 0.dp,
        ) {
            MainContent(
                cropProperties,
                cropStyle,
                imageBitmap
            ) {
                selectionPage = it

                coroutineScope.launch {
                    if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                        bottomSheetScaffoldState.bottomSheetState.collapse()
                    } else {
                        bottomSheetScaffoldState.bottomSheetState.expand()
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    cropProperties: CropProperties,
    cropStyle: CropStyle,
    imageBitmap : ImageBitmap,
    onSelectionPageMenuClicked: (SelectionPage) -> Unit
) {

    val imageBitmapLarge = ImageBitmap.imageResource(
        LocalContext.current.resources, R.drawable.landscape5
    )

    var croppedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageBitmap by remember { mutableStateOf(imageBitmapLarge) }

    var crop by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isCropping by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            ImageCropper(modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
                imageBitmap = imageBitmap,
                contentDescription = "Image Cropper",
                cropStyle = cropStyle,
                cropProperties = cropProperties,
                crop = crop,
                onCropStart = {
                    isCropping = true
                }) {
                croppedImage = it
                isCropping = false
                crop = false
                showDialog = true
            }
        }

        BottomAppBar(
            modifier = Modifier.align(Alignment.BottomStart).height(height = 75.dp),
            content = {


                IconButton(onClick = {
                    onSelectionPageMenuClicked(SelectionPage.Properties)
                }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                    )

                }
                IconButton(onClick = {
                    onSelectionPageMenuClicked(SelectionPage.Style)
                }) {
                    Icon(Icons.Filled.Brush, contentDescription = "Style")
                }

                IconButton(onClick = { crop = true }) {
                    Icon(Icons.Filled.Crop, contentDescription = "Crop Image")
                }

                Spacer(modifier = Modifier.width(130.dp))
                ImageSelectionButton(onImageSelected = {
                        bitmap: ImageBitmap ->
                    imageBitmap = bitmap
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp))
            },
        )

        if (isCropping) {
            CircularProgressIndicator()
        }
    }

    if (showDialog) {
        croppedImage?.let {
            ShowCroppedImageDialog(imageBitmap = it) {
                showDialog = !showDialog
                croppedImage = null
            }
        }
    }


}

@Composable
private fun ShowCroppedImageDialog(imageBitmap: ImageBitmap, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismissRequest, text = {
        Image(
            modifier = Modifier
                .drawChecker(RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Fit,
            bitmap = imageBitmap,
            contentDescription = "result"
        )
    }, confirmButton = {
        TextButton(onClick = {
            onDismissRequest()
            saveMediaToStorage(context, imageBitmap.asAndroidBitmap())
        }) {
            Text("Confirm")
        }
    }, dismissButton = {
        TextButton(onClick = {
            onDismissRequest()
        }) {
            Text("Dismiss")
        }
    })

}

fun saveMediaToStorage(context: Context, bitmap: Bitmap) {
    val filename = "${System.currentTimeMillis()}.jpg"
    var fos: OutputStream? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        context.contentResolver?.also { resolver ->
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }
    } else {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image = File(imagesDir, filename)
        fos = FileOutputStream(image)
    }
    fos?.use {

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        Toast.makeText(context, "save image !!", Toast.LENGTH_SHORT).show()
    }
}