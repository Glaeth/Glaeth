package com.clipsmile.app.ui

import android.content.Context
import com.clipsmile.app.data.Den

/** Hands out XP and gold when a score crosses a mark. */
object GameRewards {
    fun onScore(
        context: Context,
        previous: Int,
        score: Int,
        xpEvery: Int,
        xp: Int,
        goldEvery: Int,
        gold: Int,
        title: String
    ): String? {
        if (score <= previous) return null
        val xpHits = if (xpEvery > 0) score / xpEvery - (previous / xpEvery) else 0
        val goldHits = if (goldEvery > 0) score / goldEvery - (previous / goldEvery) else 0
        val xpTotal = xpHits * xp
        val goldTotal = goldHits * gold
        if (xpTotal <= 0 && goldTotal <= 0) return null
        Den(context).grantPlay(xpTotal, goldTotal, title)
        val bits = ArrayList<String>(2)
        if (xpTotal > 0) bits.add("+$xpTotal XP")
        if (goldTotal > 0) bits.add("+$goldTotal altın")
        return bits.joinToString("   ")
    }
}
