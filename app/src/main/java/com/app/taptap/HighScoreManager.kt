package com.app.taptap

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HighScoreManager {
    private const val PREF_NAME = "high_scores"
    private const val KEY_SCORES_NORMAL = "scores_normal"
    private const val KEY_SCORES_ZIP = "scores_zip"
    private const val MAX_SCORES = 5

    fun saveScore(context: Context, name: String, score: Int, mode: String = "NORMAL") {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (mode == "ZIP") KEY_SCORES_ZIP else KEY_SCORES_NORMAL
        val jsonString = prefs.getString(key, "[]") ?: "[]"
        val scoresArray = JSONArray(jsonString)

        // Add new score
        val newScore = JSONObject().apply {
            put("name", name)
            put("score", score)
        }
        scoresArray.put(newScore)

        // Sort and keep top 5
        val sortedScores = mutableListOf<JSONObject>()
        for (i in 0 until scoresArray.length()) {
            sortedScores.add(scoresArray.getJSONObject(i))
        }
        sortedScores.sortByDescending { it.getInt("score") }

        val trimmedScores = JSONArray()
        for (i in 0 until minOf(MAX_SCORES, sortedScores.size)) {
            trimmedScores.put(sortedScores[i])
        }

        prefs.edit().putString(key, trimmedScores.toString()).apply()
    }

    fun getScores(context: Context, mode: String = "NORMAL"): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (mode == "ZIP") KEY_SCORES_ZIP else KEY_SCORES_NORMAL
        val jsonString = prefs.getString(key, "[]") ?: "[]"
        val scoresArray = JSONArray(jsonString)

        val scoresList = mutableListOf<Pair<String, Int>>()
        for (i in 0 until scoresArray.length()) {
            val obj = scoresArray.getJSONObject(i)
            val name = obj.getString("name")
            val score = obj.getInt("score")
            scoresList.add(Pair(name, score))
        }
        return scoresList
    }

    fun clearScores(context: Context, mode: String = "NORMAL") {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (mode == "ZIP") KEY_SCORES_ZIP else KEY_SCORES_NORMAL
        prefs.edit().remove(key).apply()
    }

    fun clearAllScores(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
