package com.example.quinsta

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.MainThread
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.quinsta.model.AppViewModel
import com.example.quinsta.model.GridViewActivityViewModel
import com.example.quinsta.model.Value
import com.example.quinsta.ui.theme.QuinstaTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PanoromaViewActivity : ComponentActivity() {

    private lateinit var mainViewModel: GridViewActivityViewModel
    var isAds = true
    var isSave = true
    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var mInterstitialAd2: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val adRequest: AdRequest = AdRequest.Builder().build()

            InterstitialAd.load(this,
                Value.googleAds.interstitial_panaroma_view,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        if (isAds) {
                            mInterstitialAd.show(this@PanoromaViewActivity)
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
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {

                    mainViewModel = AppViewModel.getInstance(application)
                    Greeting()
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Greeting() {
        val imageChunks: ArrayList<Bitmap> = mainViewModel.image
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
                        }
                    },
                    elevation = 0.dp
                )
            },
            content = {
                Column(
                    modifier = Modifier.padding(it)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4), modifier = Modifier.padding(5.dp)
                        ) {
                            items(imageChunks.size) {
                                Card(
                                    modifier = Modifier.padding(5.dp),
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageChunks[it]),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(100.dp),
                                        contentScale = ContentScale.FillBounds
                                    )
                                }
                            }

                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xff32485F))
                            .padding(20.dp)
                            .fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(R.drawable.ic_save),
                            contentDescription = "",
                            Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clickable {
                                    val adRequest2: AdRequest = AdRequest
                                        .Builder()
                                        .build()
                                    if (isSave) {
                                        InterstitialAd.load(this@PanoromaViewActivity,
                                            "ca-app-pub-3940256099942544/1033173712",
                                            adRequest2,
                                            object : InterstitialAdLoadCallback() {
                                                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                                                    mInterstitialAd2 = interstitialAd
                                                    if (isAds) {
                                                        mInterstitialAd2.show(this@PanoromaViewActivity)
                                                        isAds = false
                                                    }
                                                    Log.i("TAG", "onAdLoaded")
                                                }

                                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                                    Log.d("TAG", loadAdError.toString())
                                                    mInterstitialAd2
                                                }
                                            })
                                        for (i in mainViewModel.image) {
                                            saveMediaToStorage(i)
                                        }
                                        isSave = false
                                    } else {
                                        for (i in mainViewModel.image) {
                                            saveMediaToStorage(i)
                                        }
                                    }
                                }
                        )
                    }
                }
            },
        )
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver?.also { resolver ->
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
            Toast.makeText(this, "save image !!", Toast.LENGTH_SHORT).show()
            isSave = true
        }
    }

    @MainThread
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
        isAds = false
    }
}





