package com.example.quinsta

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.quinsta.model.GridModal
import com.example.quinsta.ui.theme.QuinstaTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuinstaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    Demo()
                }
            }
        }
    }

    @Preview
    @Composable
    fun Demo() {
        var mDisplayMenu by remember { mutableStateOf(false) }
        val mContext = LocalContext.current
        Scaffold(
            topBar = {
                TopAppBar(
                    backgroundColor = Color.Transparent,
                    title = {
                        Text(
                            text = "QuinSta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp, 10.dp, 5.dp, 5.dp),
                            textAlign = TextAlign.Start,
                            color = Color(0xff32485f),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    elevation = 0.dp,
                    actions = {
                        IconButton(onClick = { mDisplayMenu = !mDisplayMenu }) {
                            Icon(painter = painterResource(id = R.drawable.ic_more), "")
                        }
                        DropdownMenu(expanded = mDisplayMenu,
                            onDismissRequest = { mDisplayMenu = false }) {
                            DropdownMenuItem(onClick = {
                                Toast.makeText(
                                    mContext, "Settings", Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(text = "Share")
                            }
                            DropdownMenuItem(onClick = {
                                Toast.makeText(
                                    mContext, "Logout", Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(text = "Rate")
                            }
                        }
                    },
                )
            },
            content = {
                GridView(context = LocalContext.current, it)
            },
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun GridView(context: Context, paddingValues: PaddingValues) {
        val selectedModule = remember { mutableStateOf<GridModal?>(null) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                when (selectedModule.value?.featureName) {
                    "Panoroma" -> {
                        context.startActivity(Intent(context, selectedModule.value!!.activity))
                    }
                    else -> {

                    }
                }
            } else {
                Toast.makeText(
                    context, "permission denied", Toast.LENGTH_SHORT
                ).show()
            }
            selectedModule.value = null
        }
        val launcher2 = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var isGranted = true
            for (i in permissions.entries) {
                if (!i.value) {
                    isGranted = false
                    break
                }
            }
            if (isGranted) {
                when (selectedModule.value?.featureName) {
                    "Panoroma" -> {
                        context.startActivity(Intent(context, selectedModule.value!!.activity))
                    }
                    else -> {

                    }
                }
            } else {
                Toast.makeText(
                    context, "permission denied", Toast.LENGTH_SHORT
                ).show()
            }
            selectedModule.value = null

//            {
//                Manifest.permission.WRITE_EXTERNAL_STORAGE :  true,
//                Manifest.permission.READ_EXTERNAL_STORAGE : true
//            }
        }

        val openDialog = remember { mutableStateOf(false) }
        val featuresList = arrayListOf(
            GridModal(
                R.drawable.ic_panorama, "Panoroma", Color(0xffff7200), PanoromaActivity::class.java
            ),
            GridModal(
                R.drawable.ic_crop, "No Crop", Color(0xfff52187), CropImageActivity::class.java
            ),
            GridModal(
                R.drawable.ic_grid, "Grids", Color(0xff8d44ad), GridsActivity::class.java
            ),
            GridModal(
                R.drawable.ic_font,
                "Stylish Font",
                Color(0xff0278fe),
                StylishFontActivity::class.java
            ),
        )
        if (openDialog.value) {
            AlertDialog(shape = RoundedCornerShape(size = 15.dp),
                onDismissRequest = { openDialog.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }) { Text(text = "OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openDialog.value = false
                    }) { Text(text = "Cancel") }
                },
                title = { Text(text = "Please confirm") },
                text = { Text("You need to allow access to some permission") })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.padding(15.dp)
        ) {
            items(featuresList.size) {
                Card(
                    onClick = {
                        when (featuresList[it].featureName) {
                            "Panoroma" -> {
                                checkPermission1(
                                    context,
                                    openDialog,
                                    selectedModule,
                                    featuresList[it],
                                    PanoromaActivity::class.java,
                                    launcher2
                                )
                            }
                            "Grids" -> {
                                checkPermission1(
                                    context,
                                    openDialog,
                                    selectedModule,
                                    featuresList[it],
                                    GridsActivity::class.java,
                                    launcher2
                                )
                            }
                            "No Crop" -> {
                                checkPermission1(
                                    context,
                                    openDialog,
                                    selectedModule,
                                    featuresList[it],
                                    CropImageActivity::class.java,
                                    launcher2
                                )
                            }
                            "Stylish Font" -> {
                                checkPermission1(
                                    context,
                                    openDialog,
                                    selectedModule,
                                    featuresList[it],
                                    StylishFontActivity::class.java,
                                    launcher2
                                )
                            }

                            else -> {

                            }
                        }


                    },
                    modifier = Modifier.padding(15.dp),
                    elevation = 5.dp,
                    backgroundColor = featuresList[it].backGround,
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = featuresList[it].featureUImage),
                            contentDescription = featuresList[it].featureName,
                            modifier = Modifier
                                .height(50.dp)
                                .width(55.dp)
                                .padding(7.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = featuresList[it].featureName,
                            color = Color.White,
                            modifier = Modifier.padding(6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }

    private fun checkPermission(
        context: Context,
        openDialog: MutableState<Boolean>,
        selectedModule: MutableState<GridModal?>,
        grid: GridModal,
        launcher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                context.startActivity(Intent(context, PanoromaActivity::class.java))
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                openDialog.value = true
            }
            else -> {
                // Asking for permission
                selectedModule.value = grid
                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkPermission1(
        context: Context,
        openDialog: MutableState<Boolean>,
        selectedModule: MutableState<GridModal?>,
        grid: GridModal,
        activity: Class<*>,
        launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
    ) {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {

                context.startActivity(Intent(context, activity))
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) || shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                openDialog.value = true
            }
            else -> {
                // Asking for permission
                selectedModule.value = grid
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }


    @ExperimentalPermissionsApi
    @Composable
    fun GetPermission() {
        val permissionsState = rememberPermissionState(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permissionsState.status == PermissionStatus.Granted) {
            Text("READ_EXTERNAL_STORAGE permission Granted")
        } else {
            Column {
                val textToShow = if (permissionsState.status.shouldShowRationale) {
                    "The READ_EXTERNAL_STORAGE is important for this app. Please grant the permission."
                } else {
                    "READ_EXTERNAL_STORAGE not available"
                }
                Text(textToShow)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionsState.launchPermissionRequest() }) {
                    Text("Request permission")
                }
            }
        }

    }
}

