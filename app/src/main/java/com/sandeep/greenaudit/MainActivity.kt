package com.sandeep.greenaudit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sandeep.greenaudit.ui.navigation.GreenAuditNavHost
import com.sandeep.greenaudit.ui.theme.GreenAuditTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Each screen owns its own Scaffold/edge-to-edge padding, so the
            // Nav host is the entire content - no outer Scaffold here.
            GreenAuditTheme {
                GreenAuditNavHost()
            }
        }
    }
}
