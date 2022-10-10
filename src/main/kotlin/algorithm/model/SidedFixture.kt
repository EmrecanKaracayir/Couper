package algorithm.model

import model.Fixture

data class SidedFixture(
    val fixture: Fixture,
    val sideTeamID: String,
) {
    fun isSideHome(): Boolean = sideTeamID == fixture.homeTeamID
}