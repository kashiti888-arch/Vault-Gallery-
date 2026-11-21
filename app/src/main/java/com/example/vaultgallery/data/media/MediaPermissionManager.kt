package com.example.vaultgallery.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaAccessState {
    GRANTED,
    PARTIALLY_GRANTED,
    DENIED
}

object MediaPermissionManager {

    fun requiredMediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkMediaAccessState(context: Context): MediaAccessState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val images = isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                val videos = isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
                val selected = isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                when {
                    images && videos -> MediaAccessState.GRANTED
                    images || videos || selected -> MediaAccessState.PARTIALLY_GRANTED
                    else -> MediaAccessState.DENIED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val images = isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                val videos = isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
                when {
                    images && videos -> MediaAccessState.GRANTED
                    images || videos -> MediaAccessState.PARTIALLY_GRANTED
                    else -> MediaAccessState.DENIED
                }
            }
            else -> {
                if (isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    MediaAccessState.GRANTED
                } else {
                    MediaAccessState.DENIED
                }
            }
        }
    }

    fun canReadImages(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                isGranted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                    isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun canReadVideos(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                isGranted(context, Manifest.permission.READ_MEDIA_VIDEO) ||
                    isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
            }
            else -> isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
