package data

import model.Fixture

data class FixturePool(
    val fixtureList: MutableList<Fixture>
) {
    private val removedFixtureList = ArrayList<Fixture>()
    fun deleteUnfinishedFixtures() {
        val fixtureListCopy = ArrayList(fixtureList)
        for (fixture in fixtureListCopy)
            if (fixture.fixtureStatus != "FT") {
                fixtureList.remove(fixture)
                removedFixtureList.add(fixture)
            }
    }

    fun recoverUnfinishedFixtures() {
        val fixtureListCopy = ArrayList(removedFixtureList)
        for (fixture in fixtureListCopy)
            if (fixture.fixtureStatus != "FT") {
                removedFixtureList.remove(fixture)
                fixtureList.add(fixture)
            }
    }

    fun sortFixtureListByTimestampAsc() {
        fixtureList.sortBy { it.fixtureTimeStamp }
    }
}