package com.clipsmile.app

/**
 * True while [com.clipsmile.app.ui.MainActivity] or
 * [com.clipsmile.app.ui.SettingsActivity] is resumed. The watcher skips its
 * background sample while one of those screens is open, because that screen
 * already reads the clipboard.
 */
object AppVisibility {
    @Volatile
    var hasVisibleActivity: Boolean = false
}
