package com.example.mmarecomp.util

import android.content.Context

/** Préférences UI légères (filtres mémorisés, dates d'export) — local uniquement. */
class UiPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var progressWindowWeeks: Int
        get() = prefs.getInt(KEY_PROGRESS_WEEKS, DEFAULT_PROGRESS_WEEKS)
        set(value) {
            prefs.edit().putInt(KEY_PROGRESS_WEEKS, value.coerceIn(4, 12)).apply()
        }

    fun markExport(exportKey: String) {
        prefs.edit().putLong(exportKey, System.currentTimeMillis()).apply()
    }

    fun lastExportMillis(exportKey: String): Long? =
        prefs.getLong(exportKey, 0L).takeIf { it > 0L }

    var importPlanDraftText: String
        get() = prefs.getString(KEY_IMPORT_DRAFT, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_IMPORT_DRAFT, value).apply()
        }

    companion object {
        private const val PREFS = "ui_prefs"
        private const val KEY_PROGRESS_WEEKS = "progress_window_weeks"
        private const val KEY_IMPORT_DRAFT = "import_plan_draft_text"
        private const val DEFAULT_PROGRESS_WEEKS = 4

        const val EXPORT_WEIGH_INS = "export_weigh_ins"
        const val EXPORT_WORKOUTS = "export_workouts"
        const val EXPORT_MEALS = "export_meals"
    }
}
