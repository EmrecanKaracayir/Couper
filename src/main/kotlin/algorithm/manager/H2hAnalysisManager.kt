package algorithm.manager

import algorithm.data.AnalysisList
import algorithm.model.SidedFixture
import algorithm.utils.isHomeTeam
import data.AlgorithmData

class H2hAnalysisManager(private val data: AlgorithmData) {
    fun analyzeH2H(sidedFixtureTBA: SidedFixture): Double? {
        val initialOAL = AnalysisList(sidedFixtureTBA)

        val h2hLastSidedFixtures = ArrayList<SidedFixture>()
        for (fixture in data.h2hLastFixtures) {
            if (isHomeTeam(
                    fixture,
                    initialOAL.nodeTBA.sidedFixture.sideTeamID
                )
            ) h2hLastSidedFixtures.add(SidedFixture(fixture, fixture.awayTeamID))
            else h2hLastSidedFixtures.add(SidedFixture(fixture, fixture.homeTeamID))
        }

        return initialOAL.analyzeH2H()
    }
}