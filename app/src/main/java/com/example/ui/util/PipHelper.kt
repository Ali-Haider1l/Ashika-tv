package com.example.ui.util

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity

object PipHelper {

    tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun isSystemPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun buildSafeAspectRatio(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio < 0.42f -> Rational(100, 239)
            ratio > 2.39f -> Rational(239, 100)
            else -> Rational(width, height)
        }
    }

    fun updateSystemPipParams(
        activity: ComponentActivity,
        shouldAutoEnter: Boolean,
        aspectRatio: Rational,
        sourceRectHint: Rect?
    ) {
        if (!isSystemPipSupported(activity)) return
        runCatching {
            val params = buildParams(
                shouldAutoEnter = shouldAutoEnter,
                aspectRatio = aspectRatio,
                sourceRectHint = sourceRectHint
            )
            activity.setPictureInPictureParams(params)
        }
    }

    fun enterSystemPip(
        activity: ComponentActivity,
        aspectRatio: Rational,
        sourceRectHint: Rect?
    ): Boolean {
        if (!isSystemPipSupported(activity)) return false
        return runCatching {
            val params = buildParams(
                shouldAutoEnter = true,
                aspectRatio = aspectRatio,
                sourceRectHint = sourceRectHint
            )
            activity.enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }

    private fun buildParams(
        shouldAutoEnter: Boolean,
        aspectRatio: Rational,
        sourceRectHint: Rect?
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
        if (sourceRectHint != null && !sourceRectHint.isEmpty) {
            builder.setSourceRectHint(sourceRectHint)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(shouldAutoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }
}
