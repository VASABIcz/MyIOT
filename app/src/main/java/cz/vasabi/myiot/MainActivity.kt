package cz.vasabi.myiot

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import cz.vasabi.myiot.ui.pages.MainPage
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class CoreApplication: Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("ServiceCast", "MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            this.window.statusBarColor = Color.BLACK
            AppTheme {
                val nav = rememberNavController()
                MainPage(nav)
            }
        }
    }
}