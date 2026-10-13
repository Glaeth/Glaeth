package com.clipsmile.app

/**
 * True while [com.clipsmile.app.ui.MainActivity] or
 * [com.clipsmile.app.ui.SettingsActivity] is resumed. The clipboard watcher
 * uses this to decide whether it can read the clipboard directly (Android 10+
 * only allows that for the focused app) or needs the momentary capture
 * activity.
 */
object AppVisibility {
    @Volatile
    var hasVisibleActivity: Boolean = false
}
