package com.ayub.statusbardate

import android.content.Context
import android.graphics.Color

object Prefs {
    val FORMAT_LABELS = arrayOf("Sun Nov 8", "8 Nov", "Sun, 8 Nov", "8 November", "৮ নভেম্বর (Bangla)")
    val FORMAT_PATTERNS = arrayOf("EEE MMM d", "d MMM", "EEE, d MMM", "d MMMM", "d MMMM")
    val COLOR_NAMES = arrayOf("White", "Black", "Yellow", "Cyan", "Green")
    val COLOR_VALUES = intArrayOf(Color.WHITE, Color.BLACK, Color.YELLOW, Color.CYAN, Color.GREEN)

    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun format(c: Context) = sp(c).getInt("format", 0)
    fun setFormat(c: Context, v: Int) = sp(c).edit().putInt("format", v).apply()

    fun sizeSp(c: Context) = sp(c).getInt("size", 12)
    fun setSizeSp(c: Context, v: Int) = sp(c).edit().putInt("size", v).apply()

    // distance from the right screen edge, in dp
    fun xDp(c: Context) = sp(c).getInt("x", 110)
    fun setXDp(c: Context, v: Int) = sp(c).edit().putInt("x", v).apply()

    // vertical nudge, in dp (-20..20)
    fun yDp(c: Context) = sp(c).getInt("y", 0)
    fun setYDp(c: Context, v: Int) = sp(c).edit().putInt("y", v).apply()

    fun color(c: Context) = sp(c).getInt("color", 0)
    fun setColor(c: Context, v: Int) = sp(c).edit().putInt("color", v).apply()

    fun bold(c: Context) = sp(c).getBoolean("bold", true)
    fun setBold(c: Context, v: Boolean) = sp(c).edit().putBoolean("bold", v).apply()
}
