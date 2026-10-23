package com.clipsmile.app.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.clipsmile.app.R
import com.clipsmile.app.data.Den

/** Store, tasks, catalog and games panels swapped in under the top bar. */
object HomeSections {

    enum class Tab { HOME, STORE, TASKS, CATALOG, GAMES }

    interface Callbacks {
        fun buy(dog: DogCompanion)
        fun select(dog: DogCompanion)
        fun play(activity: Class<*>)
        fun achievements()
    }

    fun show(host: android.widget.FrameLayout, tab: Tab, den: Den, callbacks: Callbacks) {
        host.removeAllViews()
        val context = host.context
        val scroll = ScrollView(context)
        val column = column(context)
        scroll.addView(column)
        host.addView(scroll)
        when (tab) {
            Tab.HOME -> Unit
            Tab.STORE -> store(column, den, callbacks)
            Tab.TASKS -> tasks(column, den)
            Tab.CATALOG -> catalog(column, den, callbacks)
            Tab.GAMES -> games(column, callbacks)
        }
    }

    private fun store(column: LinearLayout, den: Den, callbacks: Callbacks) {
        val context = column.context
        column.addView(title(context, context.getString(R.string.store_title)))
        column.addView(body(context, context.getString(R.string.store_gems_body)))
        for (dog in DogRoster.all) {
            if (den.owns(dog.stableId())) continue
            column.addView(animalRow(context, dog, DogRoster.priceLabel(dog), context.getString(R.string.store_buy)) {
                callbacks.buy(dog)
            })
        }
        column.addView(title(context, context.getString(R.string.store_gems_title)))
        column.addView(body(context, "80 kristal"))
        column.addView(body(context, "200 kristal"))
        column.addView(body(context, "500 kristal"))
    }

    private fun tasks(column: LinearLayout, den: Den) {
        val context = column.context
        val id = den.activeId()
        val views = den.taskViews(id)
        column.addView(title(context, context.getString(R.string.tasks_daily)))
        views.filter { !it.task.weekly }.forEach { column.addView(taskRow(context, it)) }
        column.addView(title(context, context.getString(R.string.tasks_weekly)))
        views.filter { it.task.weekly }.forEach { column.addView(taskRow(context, it)) }
    }

    private fun catalog(column: LinearLayout, den: Den, callbacks: Callbacks) {
        val context = column.context
        column.addView(title(context, context.getString(R.string.tab_catalog)))
        for (dog in DogRoster.all) {
            val owned = den.owns(dog.stableId())
            val active = owned && dog.stableId() == den.activeId()
            val label = when {
                active -> context.getString(R.string.catalog_home)
                owned -> "Sv ${den.level(dog.stableId())}"
                else -> context.getString(R.string.catalog_locked)
            }
            column.addView(animalRow(context, dog, label, if (owned && !active) context.getString(R.string.catalog_home) else "") {
                if (owned) callbacks.select(dog)
            })
        }
    }

    private fun games(column: LinearLayout, callbacks: Callbacks) {
        val context = column.context
        column.addView(title(context, context.getString(R.string.tab_games)))
        column.addView(button(context, context.getString(R.string.game_drop_short)) {
            callbacks.play(FoodDropActivity::class.java)
        })
        column.addView(button(context, context.getString(R.string.game_flappy)) {
            callbacks.play(FlappyDogActivity::class.java)
        })
        column.addView(button(context, context.getString(R.string.game_run_short)) {
            callbacks.play(RunnerActivity::class.java)
        })
        column.addView(button(context, context.getString(R.string.game_sword_short)) {
            callbacks.play(SwordGateActivity::class.java)
        })
        column.addView(button(context, context.getString(R.string.achievements_title)) {
            callbacks.achievements()
        })
        column.addView(body(context, context.getString(R.string.game_best, GameScores.best(context, GameScores.BEST_DROP))))
        column.addView(body(context, context.getString(R.string.game_best, GameScores.best(context, GameScores.BEST_FLAP))))
        column.addView(body(context, context.getString(R.string.game_best, GameScores.best(context, GameScores.BEST_RUN))))
        column.addView(body(context, context.getString(R.string.game_best_named, context.getString(R.string.game_sword_short), GameScores.best(context, GameScores.BEST_SWORD))))
    }

    private fun animalRow(
        context: Context,
        dog: DogCompanion,
        meta: String,
        action: String,
        onClick: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            setBackgroundResource(R.drawable.bg_collection_card)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 8) }
        }
        row.addView(ImageView(context).apply {
            setImageResource(dog.portraitRes)
            layoutParams = LinearLayout.LayoutParams(dp(context, 52), dp(context, 52))
        })
        val text = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(context, 10)
            }
        }
        text.addView(TextView(context).apply {
            setText(dog.nameRes)
            setTextColor(0xFF3B2918.toInt())
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        })
        text.addView(TextView(context).apply {
            this.text = meta
            setTextColor(if (dog.gemPrice > 0) 0xFF6A2BD0.toInt() else 0xFFA67C00.toInt())
            textSize = 13f
        })
        row.addView(text)
        if (action.isNotEmpty()) {
            row.addView(TextView(context).apply {
                this.text = action
                setTextColor(0xFF1C4F80.toInt())
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
            })
        }
        row.setOnClickListener { onClick() }
        return row
    }

    private fun taskRow(context: Context, view: Den.TaskView): LinearLayout {
        val wrap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            setBackgroundResource(R.drawable.bg_collection_card)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 8) }
        }
        wrap.addView(TextView(context).apply {
            text = if (view.done) "✓ ${view.task.title}" else view.task.title
            setTextColor(0xFF3B2918.toInt())
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })
        wrap.addView(TextView(context).apply {
            text = "${view.progress}/${view.task.goal}   +${view.task.xp} XP   +${view.task.gold} altın"
            setTextColor(0xFF2A75BB.toInt())
            textSize = 12f
        })
        wrap.addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = view.task.goal
            progress = view.progress
            progressDrawable = context.getDrawable(R.drawable.bg_xp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 8)
            ).apply { topMargin = dp(context, 6) }
        })
        return wrap
    }

    private fun column(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 16), dp(context, 64), dp(context, 16), dp(context, 16))
    }

    private fun title(context: Context, text: String) = TextView(context).apply {
        this.text = text
        setTextColor(0xFF3B2918.toInt())
        textSize = 22f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(context, 8), 0, dp(context, 8))
    }

    private fun body(context: Context, text: String) = TextView(context).apply {
        this.text = text
        setTextColor(0xFF5C4632.toInt())
        textSize = 14f
        setPadding(0, dp(context, 4), 0, dp(context, 4))
    }

    private fun button(context: Context, text: String, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(0xFF173307.toInt())
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setBackgroundResource(R.drawable.bg_candy_green)
        setPadding(dp(context, 12), dp(context, 14), dp(context, 12), dp(context, 14))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(context, 8) }
        setOnClickListener { onClick() }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
