/*
 * SPDX-FileCopyrightText: 2022-2024 Andrew Gunnerson
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.bcr

import android.app.Application
import android.util.Log
import androidx.core.net.toFile
import com.chiller3.bcr.output.OutputDirUtils
import com.google.android.material.color.DynamicColors

class RecorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Material You colors
        DynamicColors.applyToActivitiesIfAvailable(this)

        Notifications(this).updateChannels()

        val oldCrashHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val redactor = OutputDirUtils.NULL_REDACTOR
                val dirUtils = OutputDirUtils(this, redactor)
                val logcatPath = listOf("crash.log")
                val logcatFile = dirUtils.createFileInDefaultDir(logcatPath, "text/plain")

                Log.e(TAG, "Saving logcat to ${redactor.redact(logcatFile.uri)} due to uncaught exception in $t", e)

                try {
                    ProcessBuilder("logcat", "-d", "*:V")
                        .redirectOutput(logcatFile.uri.toFile())
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                } finally {
                    dirUtils.tryMoveToOutputDir(logcatFile, logcatPath, "text/plain")
                }
            } finally {
                oldCrashHandler?.uncaughtException(t, e)
            }
        }

        // Move preferences to device-protected storage for direct boot support.
        Preferences.migrateToDeviceProtectedStorage(this)

        // Migrate legacy preferences.
        val prefs = Preferences(this)
        prefs.migrateSampleRate()
    }

    companion object {
        private val TAG = RecorderApplication::class.java.simpleName
    }
}
