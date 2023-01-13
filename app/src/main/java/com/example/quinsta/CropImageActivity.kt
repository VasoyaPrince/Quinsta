package com.example.quinsta

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.MainThread
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class CropImageActivity : ComponentActivity() {
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
                            mInterstitialAd.show(this@CropImageActivity)
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
                                                isAds = false
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

    @MainThread
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
        isAds = false
    }
}

