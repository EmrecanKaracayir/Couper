package model

import algorithm.utils.isHomeTeam

data class Fixture(
    val fixtureID: String,
    val fixtureTimeStamp: Int,
    val fixtureStatus: String,
    val fixtureDate: String,

    val leagueID: String,
    val leagueSeason: Int,
    val leagueRound: Int,

    val homeTeamID: String,
    val homeTeamName: String,
    val awayTeamID: String,
    val awayTeamName: String,

    val homeTeamScore: Int?,
    val awayTeamScore: Int?,
) {
    var homeTeamAAG: Double? = null
    var homeTeamAYG: Double? = null
    var awayTeamAAG: Double? = null
    var awayTeamAYG: Double? = null

    var weight: Double = 1.0

    fun isFixtureAnalyzedForTeamID(refTeamID: String): Boolean {
        return if (isHomeTeam(this, refTeamID)) homeTeamAAG != null && homeTeamAYG != null
        else awayTeamAAG != null && awayTeamAYG != null
    }

    fun resetFixture() {
        homeTeamAAG = null
        homeTeamAYG = null
        awayTeamAAG = null
        awayTeamAYG = null

        weight = 1.0
    }
}