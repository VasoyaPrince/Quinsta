package com.example.quinsta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.example.quinsta.model.AppViewModel
import com.example.quinsta.model.GridCount
import com.example.quinsta.model.GridViewActivityViewModel
import com.example.quinsta.ui.theme.QuinstaTheme
import kotlin.math.sqrt


class PanoromaActivity : ComponentActivity() {
    private lateinit var mainViewModel: GridViewActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuinstaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    mainViewModel = AppViewModel.getInstance(application)
                    Greeting()
                }
            }
        }
    }

    @Composable
    @Preview(showBackground = true)
    fun Greeting() {
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    backgroundColor = Color(0xff32485F),
                    modifier = Modifier.padding(0.dp),
                    title = {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
//                            Image(painter = painterResource(R.drawable.ic_eye),
//                                contentDescription = "",
//                                modifier = Modifier.clickable {
//
//                                })
                        }
                    },
                    elevation = 0.dp,
                )
            },
            content = {
                val uri = Uri.parse("android.resource://com.example.quinsta/drawable/ic_image")
                Column(
                    modifier = Modifier.padding(it)
                ) {
                    val imageUri = remember { mutableStateOf<Uri?>(uri) }
                    val courseList = remember {
                        mutableStateListOf(
                            GridCount(3, true),
                            GridCount(4, false),
                            GridCount(5, false),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {

                        val launcher2 = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            imageUri.value = uri
                        }

                        Image(
                            painter = rememberImagePainter(data = imageUri.value),
                            contentDescription = "",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(300.dp)
                                .clickable {
                                    launcher2.launch("image/*")
                                },
                        )
                    }
                    CustomListView(courseList)
                    Box(
                        modifier = Modifier
                            .background(Color(0xff32485F))
                            .padding(15.dp)
                            .fillMaxWidth()
                    ) {
                        Row {
                            Image(
                                painter = painterResource(R.drawable.ic_round_close_24),
                                contentDescription = ""
                            )
                        }

                        Row {
                            Image(painter = painterResource(R.drawable.ic_check),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(300.dp, 0.dp, 0.dp, 0.dp)
                                    .clickable {
                                        splitImage2(
                                            context,
                                            courseList.first { item -> item.isSelected }.grid,
                                            imageUri.value!!
                                        )
                                    })
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(40.dp)
                            .fillMaxWidth()
                    ) {

                    }
                }
            },
        )
    }

    private fun splitImage2(context: Context, count: Int, imageUri: Uri) {
        val chunkNumbers = count * count
        val chunkHeight: Int
        val chunkWidth: Int

        val chunkedImages = arrayListOf<Bitmap>()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(
                contentResolver, imageUri
            )
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(
                contentResolver, imageUri
            )
        }
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, bitmap.width, bitmap.height, true
        )
        val cols: Int = sqrt(chunkNumbers.toDouble()).toInt()
        val rows = 1
        chunkHeight = bitmap.height / rows
        chunkWidth = bitmap.width / cols


        var ycoord = 0
        for (x in 0 until rows) {
            var xcoord = 0
            for (y in 0 until cols) {
                chunkedImages.add(
                    Bitmap.createBitmap(
                        scaledBitmap, xcoord, ycoord, chunkWidth, chunkHeight
                    )
                )
                xcoord += chunkWidth
            }
            ycoord += chunkHeight
        }


        mainViewModel.image = chunkedImages
        val intent = Intent(context, PanoromaViewActivity::class.java)
        startActivity(intent)
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomListView(courseList: SnapshotStateList<GridCount>) {

    LazyRow(
        modifier = Modifier.padding(120.dp, 0.dp, 0.dp, 0.dp)
    ) {

        itemsIndexed(courseList) { index, item ->
            Card(
                onClick = {
                    Log.d("TAG", "CustomListView: $courseList")
                    for (i in courseList.indices) {
                        courseList[i] = courseList[i].copy(isSelected = i == index)
                    }
                    Log.d(
                        "TAG",
                        "CustomListView: ${courseList.map { it.isSelected }.joinToString(", ")}"
                    )
                },
                modifier = Modifier
                    .padding(9.dp)
                    .size(30.dp),
                elevation = 6.dp,
                shape = CircleShape,
                backgroundColor = if (item.isSelected) Color(0xff32485F) else Color.Gray,
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${item.grid}",
                        color = if (item.isSelected) Color.White else Color.Black,
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

