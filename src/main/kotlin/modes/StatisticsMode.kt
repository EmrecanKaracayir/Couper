package modes

import MAX_ACCEPTABLE_RESULT_DEVIATION
import algorithm.Algorithm
import api.FootballApi
import data.AlgorithmData
import data.AlgorithmResults
import data.FixturePool
import model.Fixture
import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class StatisticsMode {
    companion object {
        private val scorePredictionList = ArrayList<Int>()

        private val matchResultPredictionList = ArrayList<Int>()

        private val kgPredictionList = ArrayList<Int>()

        private val overCouponsLowRiskPredictionList = ArrayList<Int>()
        private val overCouponsNormalRiskPredictionList = ArrayList<Int>()
        private val overCouponsHighRiskPredictionList = ArrayList<Int>()

        private val underCouponsLowRiskPredictionList = ArrayList<Int>()
        private val underCouponsNormalRiskPredictionList = ArrayList<Int>()
        private val underCouponsHighRiskPredictionList = ArrayList<Int>()

        private val favoriteCouponPredictionList = ArrayList<Int>()

        fun statisticsMode() {
            print(" > Enter the country name: ")
            val countryName: String = readln().lowercase()
            if (countryName.isBlank()) {
                println(" - [EXIT] | INCORRECT INPUT!")
                exitProcess(-1)
            }

            val leagues = FootballApi.leagueRequestByCountryName(countryName)
            println("\n - Leagues in ${countryName.replaceFirstChar(Char::titlecase)} are listed under!\n")
            for (league in leagues) {
                println(" - LEAGUE ID: ${league.leagueID} | LEAGUE NAME: ${league.leagueName}")
            }

            print("\n > Select a league from the list and enter its ID: ")
            val leagueID: String = readln().lowercase()
            if (leagueID.isBlank()) {
                println(" - [EXIT] | INCORRECT INPUT!")
                exitProcess(-1)
            }

            println("\n - ${leagues.first { l -> l.leagueID == leagueID.toInt() }.leagueName} is selected!")

            print("\n > Enter the season start year (2021 for 2021-2022 season): ")
            val leagueSeason = readln().toInt()

            println("\n - Fetching the fixtures...")
            val fixturePool = FootballApi.fixturePoolFiller(leagueID, leagueSeason)
            fixturePool.sortFixtureListByTimestampAsc()

            println("\n - ALGORITHM V4: Working...\n")
            fixturePool.deleteUnfinishedFixtures()
            for (fixture in fixturePool.fixtureList) {
                if (fixture.leagueSeason == leagueSeason) {
                    algorithmPhase(fixture, fixturePool)
                }
            }

            printResults()
        }

        private fun algorithmPhase(fixture: Fixture, fixturePool: FixturePool) {
            val algorithm = Algorithm(
                AlgorithmData(
                    fixture, ArrayList(), fixturePool
                )
            )
            printAndSaveScorePredictions(fixture, algorithm.getResults())
        }

        private fun printAndSaveScorePredictions(fixture: Fixture, results: AlgorithmResults) {
            val homeTeamPredictedScore = results.homeTeamPredictedScore.roundToInt()
            val awayTeamPredictedScore = results.awayTeamPredictedScore.roundToInt()

            if (results.resultDeviation > MAX_ACCEPTABLE_RESULT_DEVIATION) return

            if (fixture.homeTeamScore != null) println(" - SCORES: | ${fixture.homeTeamName} ${fixture.homeTeamScore} : ${fixture.awayTeamScore} ${fixture.awayTeamName}")
            else println(" - SCORES: | ${fixture.homeTeamName} [UNDETERMINED] : [UNDETERMINED] ${fixture.awayTeamName}")

            if (fixture.homeTeamScore != null && fixture.awayTeamScore != null) {
                val totalGoals = fixture.homeTeamScore + fixture.awayTeamScore

                // ? SCORE PREDICTION
                if (homeTeamPredictedScore == fixture.homeTeamScore && awayTeamPredictedScore == fixture.awayTeamScore) scorePredictionList.add(
                    1
                )
                else scorePredictionList.add(0)

                // ? END MATCH PREDICTION
                // Home Wins
                if (fixture.homeTeamScore > fixture.awayTeamScore) {
                    if (homeTeamPredictedScore > awayTeamPredictedScore) matchResultPredictionList.add(
                        1
                    )
                    else matchResultPredictionList.add(0)
                }
                // Away Wins
                else if (fixture.homeTeamScore < fixture.awayTeamScore) {
                    if (homeTeamPredictedScore < awayTeamPredictedScore) matchResultPredictionList.add(
                        1
                    )
                    else matchResultPredictionList.add(0)
                }
                // Draw
                else {
                    if (homeTeamPredictedScore == awayTeamPredictedScore) matchResultPredictionList.add(
                        1
                    )
                    else matchResultPredictionList.add(0)
                }

                // ? KG PREDICTION
                if (homeTeamPredictedScore == 0 || awayTeamPredictedScore == 0) {
                    if (fixture.homeTeamScore == 0 || fixture.awayTeamScore == 0) kgPredictionList.add(
                        1
                    )
                    else kgPredictionList.add(0)
                } else {
                    if (fixture.homeTeamScore == 0 || fixture.awayTeamScore == 0) kgPredictionList.add(
                        0
                    )
                    else kgPredictionList.add(1)
                }

                // ? OVER COUPONS PREDICTION
                if (totalGoals > results.overCouponLOW_RISK) overCouponsLowRiskPredictionList.add(1)
                else overCouponsLowRiskPredictionList.add(0)
                if (totalGoals > results.overCouponNORMAL) overCouponsNormalRiskPredictionList.add(1)
                else overCouponsNormalRiskPredictionList.add(0)
                if (totalGoals > results.overCouponHIGH_RISK) overCouponsHighRiskPredictionList.add(
                    1
                )
                else overCouponsHighRiskPredictionList.add(0)

                // ? UNDER COUPONS PREDICTION
                if (totalGoals < results.underCouponLOW_RISK) underCouponsLowRiskPredictionList.add(
                    1
                )
                else underCouponsLowRiskPredictionList.add(0)
                if (totalGoals < results.underCouponNORMAL) underCouponsNormalRiskPredictionList.add(
                    1
                )
                else underCouponsNormalRiskPredictionList.add(0)
                if (totalGoals < results.underCouponHIGH_RISK) underCouponsHighRiskPredictionList.add(
                    1
                )
                else underCouponsHighRiskPredictionList.add(0)

                // ? FAVORITE COUPON PREDICTION
                if (results.favoriteCouponIsOver) {
                    if (totalGoals > results.favoriteCoupon) favoriteCouponPredictionList.add(1)
                    else favoriteCouponPredictionList.add(0)
                } else {
                    if (totalGoals < results.favoriteCoupon) favoriteCouponPredictionList.add(1)
                    else favoriteCouponPredictionList.add(0)
                }

            }
            println(
                " - ALGOV4: | ${fixture.homeTeamName} $homeTeamPredictedScore : $awayTeamPredictedScore ${fixture.awayTeamName} | Deviation: ${
                    DecimalFormat(
                        "#.##"
                    ).format(results.resultDeviation)
                }"
            )
            println()
        }

        private fun printResults() {
            println("\n\n########## SONUÇLAR ##########")
            println("\n - Minimum kabul edilebilir sonuç tutarlılığı yüzdesi: $MAX_ACCEPTABLE_RESULT_DEVIATION")

            println("\n SKOR TAHMİNİ:")
            val scoreSuccessRate = scorePredictionList.sum() * 100.0 / scorePredictionList.size
            println("\n - BAŞARI ORANI: $scoreSuccessRate% | DOĞRU TAHMİN EDİLEN: ${scorePredictionList.sum()}/${scorePredictionList.size}\n")


            println("\n MAÇ SONUCU TAHMİNİ:")
            val matchResultSuccessRate =
                matchResultPredictionList.sum() * 100.0 / matchResultPredictionList.size
            println("\n - BAŞARI ORANI: $matchResultSuccessRate% | DOĞRU TAHMİN EDİLEN: ${matchResultPredictionList.sum()}/${matchResultPredictionList.size}\n")


            println("\n KG TAHMİNİ:")
            val kgSuccessRate = kgPredictionList.sum() * 100.0 / kgPredictionList.size
            println("\n - BAŞARI ORANI: $kgSuccessRate% | DOĞRU TAHMİN EDİLEN: ${kgPredictionList.sum()}/${kgPredictionList.size}\n")


            println("\n ÜST KUPONLAR TAHMİNİ:")
            val overCouponsLowRiskSuccessRate =
                overCouponsLowRiskPredictionList.sum() * 100.0 / overCouponsLowRiskPredictionList.size
            println("\n - DÜŞÜK RİSK BAŞARI ORANI: $overCouponsLowRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${overCouponsLowRiskPredictionList.sum()}/${overCouponsLowRiskPredictionList.size}")

            val overCouponsNormalRiskSuccessRate =
                overCouponsNormalRiskPredictionList.sum() * 100.0 / overCouponsNormalRiskPredictionList.size
            println("\n - NORMAL RİSK BAŞARI ORANI: $overCouponsNormalRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${overCouponsNormalRiskPredictionList.sum()}/${overCouponsNormalRiskPredictionList.size}")

            val overCouponsHighRiskSuccessRate =
                overCouponsHighRiskPredictionList.sum() * 100.0 / overCouponsHighRiskPredictionList.size
            println("\n - YÜKSEK RİSK BAŞARI ORANI: $overCouponsHighRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${overCouponsHighRiskPredictionList.sum()}/${overCouponsHighRiskPredictionList.size}\n")


            println("\n ALT KUPONLAR TAHMİNİ:")
            val underCouponsLowRiskSuccessRate =
                underCouponsLowRiskPredictionList.sum() * 100.0 / underCouponsLowRiskPredictionList.size
            println("\n - DÜŞÜK RİSK BAŞARI ORANI: $underCouponsLowRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${underCouponsLowRiskPredictionList.sum()}/${underCouponsLowRiskPredictionList.size}")

            val underCouponsNormalRiskSuccessRate =
                underCouponsNormalRiskPredictionList.sum() * 100.0 / underCouponsNormalRiskPredictionList.size
            println("\n - NORMAL RİSK BAŞARI ORANI: $underCouponsNormalRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${underCouponsNormalRiskPredictionList.sum()}/${underCouponsNormalRiskPredictionList.size}")

            val underCouponsHighRiskSuccessRate =
                underCouponsHighRiskPredictionList.sum() * 100.0 / underCouponsHighRiskPredictionList.size
            println("\n - YÜKSEK RİSK BAŞARI ORANI: $underCouponsHighRiskSuccessRate% | DOĞRU TAHMİN EDİLEN: ${underCouponsHighRiskPredictionList.sum()}/${underCouponsHighRiskPredictionList.size}\n")


            println("\n FAVORİ KUPON TAHMİNİ:")
            val favoriteCouponSuccessRate =
                favoriteCouponPredictionList.sum() * 100.0 / favoriteCouponPredictionList.size
            println("\n - BAŞARI ORANI: $favoriteCouponSuccessRate% | DOĞRU TAHMİN EDİLEN: ${favoriteCouponPredictionList.sum()}/${favoriteCouponPredictionList.size}")

        }
    }
}