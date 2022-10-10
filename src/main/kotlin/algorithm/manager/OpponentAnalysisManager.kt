package algorithm.manager

import algorithm.data.AnalysisList
import algorithm.model.SidedFixture
import algorithm.utils.isHomeTeam
import data.AlgorithmData

class OpponentAnalysisManager(private val data: AlgorithmData) {

    fun analyzeOpponent(sidedFixtureTBA: SidedFixture) {
        val initialAL = AnalysisList(sidedFixtureTBA)

        val listAL = ArrayList<AnalysisList>()
        listAL.add(initialAL)

        while (!initialAL.nodeTBA.sidedFixture.fixture.isFixtureAnalyzedForTeamID(initialAL.nodeTBA.sidedFixture.sideTeamID)) {
            val curAL = listAL[listAL.lastIndex]
            if (curAL.isEmpty()) {
                val curSideLastSidedFixtures = findSidedFixtures(curAL.nodeTBA.sidedFixture)
                if (curSideLastSidedFixtures.isEmpty()) {
                    curAL.analyze()
                    listAL.remove(curAL)
                } else {
                    curAL.appendSidedFixtures(curSideLastSidedFixtures)
                    listAL.add(AnalysisList(curAL.nodeTBA.downNode!!.sidedFixture))
                }
            } else {
                val firstSidedFixtureNotCalculated = curAL.lastSidedFixtureNotCalculated()
                if (firstSidedFixtureNotCalculated != null) listAL.add(
                    AnalysisList(
                        firstSidedFixtureNotCalculated
                    )
                )
                else {
                    curAL.analyze()
                    listAL.remove(curAL)
                }
            }
        }
    }

    private fun findSidedFixtures(sidedFixture: SidedFixture): List<SidedFixture> {
        val sideLastFixtures = data.fixturePool.fixtureList.filter { f ->
            (f.homeTeamID == sidedFixture.sideTeamID || f.awayTeamID == sidedFixture.sideTeamID) && f.fixtureTimeStamp < sidedFixture.fixture.fixtureTimeStamp
        }.sortedByDescending { it.fixtureTimeStamp }
        val sideLastSidedFixtures = ArrayList<SidedFixture>()
        for (fixture in sideLastFixtures) {
            if (isHomeTeam(
                    fixture, sidedFixture.sideTeamID
                )
            ) sideLastSidedFixtures.add(SidedFixture(fixture, fixture.awayTeamID))
            else sideLastSidedFixtures.add(SidedFixture(fixture, fixture.homeTeamID))
        }
        return sideLastSidedFixtures
    }
}