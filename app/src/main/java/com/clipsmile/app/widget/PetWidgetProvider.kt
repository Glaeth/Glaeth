package com.clipsmile.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.clipsmile.app.R
import com.clipsmile.app.data.PetProgress
import com.clipsmile.app.ui.DogRoster
import com.clipsmile.app.ui.MainActivity

/**
 * Home-screen card in the Duolingo widget shape: streak, the dog's name,
 * a short line, and the dog currently at home. Tapping opens the app.
 */
class PetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, manager, id)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PetWidgetProvider::class.java))
            for (id in ids) update(context, manager, id)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val pet = PetProgress(context)
            val dog = DogRoster.selected(context)
            val name = DogRoster.homeName(context)
            val streak = pet.streak()
            val line = when {
                streak > 0 && pet.visitedToday() -> context.getString(R.string.widget_with_you)
                streak > 0 -> context.getString(R.string.widget_missed, streak)
                else -> context.getString(R.string.widget_start)
            }
            val views = RemoteViews(context.packageName, R.layout.widget_pet)
            views.setTextViewText(R.id.widgetDays, context.getString(R.string.widget_days, streak.coerceAtLeast(0)))
            views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_title, name))
            views.setTextViewText(R.id.widgetLine, line)
            views.setImageViewResource(R.id.widgetPortrait, dog.portraitRes)
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, open)
            manager.updateAppWidget(id, views)
        }
    }
}
