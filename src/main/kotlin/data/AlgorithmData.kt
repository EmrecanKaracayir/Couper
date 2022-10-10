package data

import model.Fixture

data class AlgorithmData(
    val fixtureToPredict: Fixture,
    val h2hLastFixtures: List<Fixture>,
    val fixturePool: FixturePool
)
