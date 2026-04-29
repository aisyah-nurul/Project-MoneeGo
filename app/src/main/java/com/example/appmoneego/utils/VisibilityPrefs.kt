package com.example.appmoneego.utils

import android.content.Context

object VisibilityPrefs {
    private const val PREF_NAME        = "moneego_prefs"
    private const val KEY_NOMINAL_VISIBLE = "nominal_visible"

    fun isNominalVisible(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOMINAL_VISIBLE, true)

    fun setNominalVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOMINAL_VISIBLE, visible)
            .apply()
    }
}