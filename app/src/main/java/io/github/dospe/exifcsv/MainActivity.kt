package io.github.dospe.exifcsv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.dospe.exifcsv.ui.ExifCsvApp
import io.github.dospe.exifcsv.ui.ExifCsvTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExifCsvTheme {
                ExifCsvApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed permissions in system settings meanwhile.
        viewModel.refreshAccess()
    }
}
