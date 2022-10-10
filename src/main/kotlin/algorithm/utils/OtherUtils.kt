package algorithm.utils

import algorithm.model.SidedFixture
import model.Fixture

fun isInSameSeason(fixture: Fixture, reference: Fixture): Boolean =
    fixture.leagueSeason == reference.leagueSeason

fun isHomeTeam(fixture: Fixture, teamID: String): Boolean = fixture.homeTeamID == teamID

fun isSideInCorrectPosition(fixture: Fixture, sidedFixture: SidedFixture): Boolean {
    return if (sidedFixture.isSideHome()) fixture.homeTeamID == sidedFixture.sideTeamID
    else fixture.awayTeamID == sidedFixture.sideTeamID
}