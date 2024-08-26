package com.vistring.trace.demo.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vistring.trace.demo.test.TestDelay
import com.vistring.trace.demo.ui.theme.VSMethodTraceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VSMethodTraceTheme {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 20.dp)
                ) {
                    Button(
                        onClick = {
                            TestDelay.testMethod2()
                        }
                    ) {
                        Text(text = "耗时测试")
                    }
                }
            }
        }
    }
}