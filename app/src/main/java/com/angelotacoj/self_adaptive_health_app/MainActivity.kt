package com.angelotacoj.self_adaptive_health_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.angelotacoj.self_adaptive_health_app.di.AppContainer
import com.angelotacoj.self_adaptive_health_app.navigation.AppNavHost
import com.angelotacoj.self_adaptive_health_app.ui.theme.Self_Adaptive_Health_AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContainer.init(applicationContext)
        setContent {
            Self_Adaptive_Health_AppTheme {
                AppNavHost()
            }
        }
    }
}
