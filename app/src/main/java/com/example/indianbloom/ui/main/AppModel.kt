package com.example.indianbloom.ui.main

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean
)

data class RawAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)
