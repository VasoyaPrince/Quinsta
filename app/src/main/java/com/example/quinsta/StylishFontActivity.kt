package com.example.quinsta

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quinsta.model.Font
import com.example.quinsta.model.Fonts
import com.example.quinsta.ui.theme.QuinstaTheme
import java.util.*

class StylishFontActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuinstaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    Greeting()
                }
            }
        }
    }
@Composable
@Preview(showBackground = true)
fun Greeting() {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color(0xff32485F), modifier = Modifier.padding(0.dp), title = {
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
                    }
                }, elevation = 0.dp
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
            ) {
                var text by rememberSaveable { mutableStateOf("") }
                val fontArrayList: ArrayList<Font> = ArrayList()
                Box(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xfff5f5f5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(value = text,
                            onValueChange = {
                                text = it
                            },
                            label = { Text("Stylish Font") },
                            singleLine = true,
                            placeholder = { Text("Enter the name ") })
                    }
                }
                Box(
                    modifier = Modifier.padding(20.dp, 0.dp)
                ) {

                    val charArray = text.lowercase(Locale.getDefault()).toCharArray()
                    val strArr = arrayOfNulls<String>(44)
                    for (i in 0..43) {
                        strArr[i] = applyStyle(charArray, Fonts.strings[i])
                    }
                    fontArrayList.clear()
                    if (text.isNotEmpty()) {
                        for (i in 0..43) {
                            val font = Font()
                            font.fontText = strArr[i]!!
                            fontArrayList.add(font)
                        }

                    }
                    LazyRowItemsDemo(fontArrayList)
                }
            }
        },
    )
}
}



@Composable
fun LazyRowItemsDemo(courseList: ArrayList<Font>) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(courseList) { index, item ->
            Card(
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0xfff5f5f5),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 3.dp)
            ) {
                Row(
                    Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "${item.fontText}",
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                    )
                    AppIconButton(
                        onClick = {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText(
                                    "stylish text", "${item.fontText}"
                                )
                            )
                            Toast.makeText(context, "Text Copied", Toast.LENGTH_SHORT).show()
                        },
                        icon = R.drawable.baseline_copy,
                        Color(0xffff7200)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    AppIconButton(
                        onClick = {
                            val intent2 = Intent()
                            intent2.action = Intent.ACTION_SEND
                            intent2.type = "text/plain"
                            intent2.putExtra(Intent.EXTRA_TEXT, "${item.fontText}")
                            context.startActivity(Intent.createChooser(intent2, "Share via"))
                        },
                        icon = R.drawable.baseline_share_24,
                        Color(0xfff52187)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconButton(
    onClick: () -> Unit,
    icon: Int,
    color: Color
) {
    IconButton(
        onClick = onClick, modifier = Modifier
            .background(
                color, shape = CircleShape
            )
            .size(35.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "",
            modifier = Modifier.size(15.dp)
        )
    }
}


private fun applyStyle(charArray: CharArray, string: List<String>): String {
    val stringBuffer = StringBuffer()
    for (i in charArray.indices) {
        if (charArray[i].code - 'a'.code < 0 || charArray[i].code - 'a'.code > 25) {
            stringBuffer.append(charArray[i])
        } else {
            stringBuffer.append(string[charArray[i].code - 'a'.code])
        }
    }
    return stringBuffer.toString()
}