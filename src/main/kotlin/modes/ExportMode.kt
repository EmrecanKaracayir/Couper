package modes

import TITLE_VERSION
import algorithm.Algorithm
import api.FootballApi
import data.AlgorithmData
import data.AlgorithmResults
import data.FixturePool
import io.Exporter
import model.Fixture
import java.text.DecimalFormat

class ExportMode {
    companion object {
        enum class SelectedLeagues(val country: String, val leagueIDs: Array<String>) {
            ENGLAND("england", arrayOf("39", "40")), GERMANY("germany", arrayOf("78")), SPAIN(
                "spain",
                arrayOf("140")
            ),
            ITALY("italy", arrayOf("135")), FRANCE("france", arrayOf("61", "62")), TURKEY(
                "turkey",
                arrayOf("203")
            )
        }

        fun exportMode() {
            val document = ArrayList<String>()
            document.add(TITLE_VERSION)
            for (leagueItem in SelectedLeagues.values()) {
                document.add(leagueItem.country.uppercase())
                println("\n - COUNTRY: ${leagueItem.country.uppercase()}")
                for (leagueID in leagueItem.leagueIDs) {
                    val league = FootballApi.leagueRequestById(leagueID)

                    document.add(league.leagueName)
                    println("\n - LEAGUE: ${league.leagueName}")

                    val leagueRound =
                        FootballApi.leagueCurrentRoundRequest(leagueID, league.currentSeason)
                    val fixturePool = FootballApi.fixturePoolFiller(leagueID, league.currentSeason)
                    fixturePool.sortFixtureListByTimestampAsc()

                    val fixturesToAnalyze = ArrayList<Fixture>()
                    for (fixture in fixturePool.fixtureList) if (fixture.leagueSeason == league.currentSeason && fixture.leagueRound == leagueRound) fixturesToAnalyze.add(
                        fixture
                    )

                    for (fixture in fixturesToAnalyze) {
                        val results = algorithmPhase(fixture, fixturePool)
                        val decimalResultDeviation =
                            DecimalFormat("#.##").format(results.resultDeviation)
                        if (results.favoriteCouponIsOver) document.add("- Tarih: ${fixture.fixtureDate} | Tahmin: ${results.favoriteCoupon} ÜST | Sapma: $decimalResultDeviation | Fikstür: ${fixture.homeTeamName} : ${fixture.awayTeamName}")
                        else document.add("- Tarih: ${fixture.fixtureDate} | Tahmin: ${results.favoriteCoupon} ALT | Sapma: $decimalResultDeviation | Fikstür: ${fixture.homeTeamName} : ${fixture.awayTeamName}")
                        fixturePool.recoverUnfinishedFixtures()
                        fixturePool.sortFixtureListByTimestampAsc()
                    }

                    println("\n - 1 minute pause!")
                    Thread.sleep(60000)
                }
            }
            val success = Exporter.export(document)
            if (success) println("\n - Analysis successfully exported.")
            else println("n - There was an error exporting analysis!")
        }

        private fun algorithmPhase(fixture: Fixture, fixturePool: FixturePool): AlgorithmResults {
            fixturePool.deleteUnfinishedFixtures()

            val h2hLastFixtures = FootballApi.h2hMatchesRequest(
                fixture.fixtureTimeStamp,
                fixture.leagueID,
                fixture.homeTeamID,
                fixture.homeTeamName,
                fixture.awayTeamID,
                fixture.awayTeamName
            )

            val algorithm = Algorithm(
                AlgorithmData(
                    fixture, h2hLastFixtures, fixturePool
                )
            )
            return algorithm.getResults()
        }
    }
}