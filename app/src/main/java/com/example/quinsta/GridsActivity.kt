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
import androidx.annotation.MainThread
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import com.example.quinsta.model.AppViewModel
import com.example.quinsta.model.GridCount
import com.example.quinsta.model.GridViewActivityViewModel
import com.example.quinsta.ui.theme.QuinstaTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.InputStream
import kotlin.math.sqrt


class GridsActivity : ComponentActivity() {
    private lateinit var mainViewModel: GridViewActivityViewModel
        var isAds = true
        private lateinit var mInterstitialAd: InterstitialAd
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val adRequest: AdRequest = AdRequest.Builder().build()

            InterstitialAd.load(this,
                "ca-app-pub-3940256099942544/1033173712",
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        if (isAds) {
                            mInterstitialAd.show(this@GridsActivity)
                            isAds = false
                        }
                        Log.i("TAG", "onAdLoaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error
                        Log.d("TAG", loadAdError.toString())
                        mInterstitialAd
                    }
                })

            QuinstaTheme {
                mainViewModel = AppViewModel.getInstance(application)
                Greeting()
            }
        }
    }

    @Composable
    @Preview(showBackground = true)
    fun Greeting() {
        val context = LocalContext.current
        val onBack = {

        }

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
                                        isAds = false
                                    },
                            )
                            Image(painter = painterResource(R.drawable.ic_eye),
                                contentDescription = "",
                                modifier = Modifier.clickable {

                                })
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
                    val imageUri = remember { mutableStateOf<Uri?>(uri)}
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
                                        val count =
                                            courseList.first { item -> item.isSelected }.grid
                                        splitImage(
                                            context,
                                            count,
                                            imageUri.value!!,
                                            count
                                        )
                                    })
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }
            },
        )
    }

    private fun splitImage(context: Context, count: Int, imageUri: Uri, row: Int = 1) {

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
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
        val cols: Int = sqrt(chunkNumbers.toDouble()).toInt()
        val rows: Int = row
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
        val intent = Intent(context, GridViewActivity::class.java)
        startActivity(intent)
    }
    @MainThread
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
        isAds = false
    }
}

