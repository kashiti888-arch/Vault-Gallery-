package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.VaultGalleryTheme
import com.example.vaultgallery.VaultGalleryApplication
import com.example.vaultgallery.ui.VaultGalleryApp
import com.example.vaultgallery.viewmodel.VaultGalleryViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: VaultGalleryViewModel by viewModels {
        VaultGalleryViewModel.factory(application as VaultGalleryApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            VaultGalleryTheme(
                themeMode = settings.themeMode,
                fontScale = settings.fontScale
            ) {
                VaultGalleryApp(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }
}
