package com.example.quinsta

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.example.quinsta.model.ApiResponse
import com.example.quinsta.model.ApiService
import com.example.quinsta.model.Value
import com.example.quinsta.ui.theme.QuinstaTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuinstaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    val scale = remember {
                        androidx.compose.animation.core.Animatable(0f)
                    }

                    // Animation
                    LaunchedEffect(key1 = true) {
                        scale.animateTo(
                            targetValue = 0.7f,
                            animationSpec = tween(durationMillis = 800, easing = {
                                OvershootInterpolator(4f).getInterpolation(it)
                            })
                        )
                        delay(2000)
                        getDataUsingRetrofit(this@SplashActivity)
                    }

                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = "Logo",
                            modifier = Modifier.scale(scale.value)
                        )

                    }
                }
            }
        }
    }

    private fun getDataUsingRetrofit(
        ctx: Context
    ) {
        val url = "https://appmanagement.onrender.com/"
        val retrofit =
            Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create())
                .build()
        val retrofitAPI = retrofit.create(ApiService::class.java)
        val call = retrofitAPI.getAds()

        call.enqueue(object : Callback<ApiResponse?> {
            override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
//                Toast.makeText(ctx, "Data posted to API", Toast.LENGTH_SHORT).show()
                val model = response.body()
                for (i in 0 until model!!.data.adsProvider.size) {
                    if (model.data.adsProvider[i].name == "Google Ads") {
                        for (j in 0 until model.data.adsProvider[i].fields.size) {
                            when (model.data.adsProvider[i].fields[j].name) {
                                "Application Id" -> {
                                    Value.googleAds.ApplicationId =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "banner_home" -> {
                                    Value.googleAds.banner_home =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "banner_panaroma" -> {
                                    Value.googleAds.banner_panaroma =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "banner_grids" -> {
                                    Value.googleAds.banner_grids =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "banner_stylish_font" -> {
                                    Value.googleAds.banner_stylish_font =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_panaroma" -> {
                                    Value.googleAds.interstitial_panaroma =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_panaroma_view" -> {
                                    Value.googleAds.interstitial_panaroma_view =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_nocrop" -> {
                                    Value.googleAds.interstitial_nocrop =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_grids" -> {
                                    Value.googleAds.interstitial_grids =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_grids_view" -> {
                                    Value.googleAds.interstitial_grids_view =
                                        model.data.adsProvider[i].fields[j].value
                                }
                                "interstitial_stylishFont" -> {
                                    Value.googleAds.interstitial_stylishFont =
                                        model.data.adsProvider[i].fields[j].value
                                }

                            }
                        }
                    }

                }
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()

            }

            override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                Toast.makeText(ctx, "${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
}