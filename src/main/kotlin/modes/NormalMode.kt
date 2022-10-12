package modes

import algorithm.Algorithm
import api.FootballApi
import data.AlgorithmData
import data.AlgorithmResults
import data.FixturePool
import model.Fixture
import java.text.DecimalFormat
import kotlin.system.exitProcess

class NormalMode {
    companion object {
        fun normalMode() {
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

            println("\n - You can enter \"current\" or 'c' for the current season/round!")

            print("\n > Enter the season start year (2021 for 2021-2022 season): ")
            val seasonInput = readln().lowercase()

            val leagueSeason: Int
            val leagueRound: Int
            if (seasonInput == "c" || seasonInput == "current") {
                leagueSeason = leagues.first { l -> l.leagueID == leagueID.toInt() }.currentSeason
                leagueRound = FootballApi.leagueCurrentRoundRequest(leagueID, leagueSeason)
            } else {
                leagueSeason = seasonInput.toInt()

                print("\n > Enter the week of the season: ")
                leagueRound = readln().toInt()
            }

            println("\n - Fetching the fixtures...")
            val fixturePool = FootballApi.fixturePoolFiller(leagueID, leagueSeason)
            fixturePool.sortFixtureListByTimestampAsc()

            while (true) {
                fixturePool.recoverUnfinishedFixtures()
                fixturePool.sortFixtureListByTimestampAsc()

                println("\n - Fixtures in week $leagueRound are listed under!\n")
                for (fixture in fixturePool.fixtureList) {
                    if (fixture.leagueSeason == leagueSeason && fixture.leagueRound == leagueRound) {
                        if (fixture.homeTeamScore != null) println(" - DATE: ${fixture.fixtureDate} | FIXTURE ID: ${fixture.fixtureID} | ${fixture.homeTeamName} ${fixture.homeTeamScore} : ${fixture.awayTeamScore} ${fixture.awayTeamName}")
                        else println(" - DATE: ${fixture.fixtureDate} | FIXTURE ID: ${fixture.fixtureID} | ${fixture.homeTeamName} [UNDETERMINED] : [UNDETERMINED] ${fixture.awayTeamName}")
                    }
                }
                print("\n > Select a fixture from the list and enter its ID: ")
                val fixtureID = readln().lowercase()
                if (fixtureID.isBlank()) {
                    println(" - [EXIT] | INCORRECT INPUT!")
                    exitProcess(-1)
                }

                var selectedFixture: Fixture? = null
                for (fixture in fixturePool.fixtureList) {
                    if (fixtureID == fixture.fixtureID) {
                        if (fixture.homeTeamScore != null) println("\n - DATE: ${fixture.fixtureDate} | SELECTED FIXTURE: ${fixture.homeTeamName} ${fixture.homeTeamScore} : ${fixture.awayTeamScore} ${fixture.awayTeamName}\n")
                        else println("\n - DATE: ${fixture.fixtureDate} | SELECTED FIXTURE: ${fixture.homeTeamName} [UNDETERMINED] : [UNDETERMINED] ${fixture.awayTeamName}\n")
                        selectedFixture = fixture
                        break
                    }
                }
                if (selectedFixture == null) {
                    println(" - [EXIT] | FIXTURE NOT FOUND!")
                    exitProcess(-4)
                }

                algorithmPhase(selectedFixture, fixturePool)
                fixturePool.resetFixtures()
                FootballApi.apiPredictionsPhase(selectedFixture)

                println("\n########## ------- ##########\n")
            }
        }

        private fun algorithmPhase(fixture: Fixture, fixturePool: FixturePool) {
            fixturePool.deleteUnfinishedFixtures()

            val h2hLastFixtures = FootballApi.h2hMatchesRequest(
                fixture.fixtureTimeStamp,
                fixture.leagueID,
                fixture.homeTeamID,
                fixture.homeTeamName,
                fixture.awayTeamID,
                fixture.awayTeamName
            )

            println("\n - ALGORITHM V4: Working...")

            val algorithm = Algorithm(
                AlgorithmData(
                    fixture, h2hLastFixtures, fixturePool
                )
            )
            printResults(fixture, algorithm.getResults())
        }

        private fun printResults(fixture: Fixture, algorithmResults: AlgorithmResults) {
            println("\n\n########## RESULTS ##########")
            var totalGoals = -1

            if (fixture.homeTeamScore != null && fixture.awayTeamScore != null) totalGoals =
                fixture.homeTeamScore + fixture.awayTeamScore

            val decimalResultDeviation = DecimalFormat("#.##").format(algorithmResults.resultDeviation)
            println("\n - Result Deviation: $decimalResultDeviation")

            val decimalHomeTeamPredictedScore =
                DecimalFormat("#.##").format(algorithmResults.homeTeamPredictedScore)
            val decimalAwayTeamPredictedScore =
                DecimalFormat("#.##").format(algorithmResults.awayTeamPredictedScore)

            val decimalHomePAG = DecimalFormat("#.##").format(algorithmResults.homePAAG)
            val decimalHomePYG = DecimalFormat("#.##").format(algorithmResults.homePAYG)
            val decimalAwayPAG = DecimalFormat("#.##").format(algorithmResults.awayPAAG)
            val decimalAwayPYG = DecimalFormat("#.##").format(algorithmResults.awayPAYG)

            val decimalMING = DecimalFormat("#.##").format(algorithmResults.minG)
            val decimalMIDG = DecimalFormat("#.##").format(algorithmResults.midG)
            val decimalMAXG = DecimalFormat("#.##").format(algorithmResults.maxG)

            val decimalH2hMIDG = DecimalFormat("#.##").format(algorithmResults.h2hMIDG)

            val decimalOverCouponLowRisk =
                DecimalFormat("#.##").format(algorithmResults.overCouponLOW_RISK)
            val decimalOverCouponNormal =
                DecimalFormat("#.##").format(algorithmResults.overCouponNORMAL)
            val decimalOverCouponHighRisk =
                DecimalFormat("#.##").format(algorithmResults.overCouponHIGH_RISK)

            val decimalUnderCouponLowRisk =
                DecimalFormat("#.##").format(algorithmResults.underCouponLOW_RISK)
            val decimalUnderCouponNormal =
                DecimalFormat("#.##").format(algorithmResults.underCouponNORMAL)
            val decimalUnderCouponHighRisk =
                DecimalFormat("#.##").format(algorithmResults.underCouponHIGH_RISK)

            val decimalFavoriteCoupon = DecimalFormat("#.##").format(algorithmResults.favoriteCoupon)

            if (totalGoals != -1) {
                println(
                    "\n - PREDICTED SCORE: | ${algorithmResults.homeTeamName} $decimalHomeTeamPredictedScore : $decimalAwayTeamPredictedScore ${algorithmResults.awayTeamName} |\n" + "\n" + " - HOME PAAG: $decimalHomePAG | HOME PAYG: $decimalHomePYG\n" + " - AWAY PAAG: $decimalAwayPAG | AWAY PAYG: $decimalAwayPYG\n" + "\n - H2H-MIDG: $decimalH2hMIDG\n" + "\n - MING: $decimalMING\n" + " - MIDG: $decimalMIDG\n" + " - MAXG: $decimalMAXG\n" + "\n" + " - OVER COUPONS:\n" + " - LOW RISK: $decimalOverCouponLowRisk OVER | ${if (totalGoals > algorithmResults.overCouponLOW_RISK) "[WIN]" else "[LOSE]"}\n" + " - NORMAL: $decimalOverCouponNormal OVER | ${if (totalGoals > algorithmResults.overCouponNORMAL) "[WIN]" else "[LOSE]"}\n" + " - HIGH RISK: $decimalOverCouponHighRisk OVER | ${if (totalGoals > algorithmResults.overCouponHIGH_RISK) "[WIN]" else "[LOSE]"}\n" + "\n" + " - UNDER COUPONS:\n" + " - LOW RISK: $decimalUnderCouponLowRisk UNDER | ${if (totalGoals < algorithmResults.underCouponLOW_RISK) "[WIN]" else "[LOSE]"}\n" + " - NORMAL: $decimalUnderCouponNormal UNDER | ${if (totalGoals < algorithmResults.underCouponNORMAL) "[WIN]" else "[LOSE]"}\n" + " - HIGH RISK: $decimalUnderCouponHighRisk UNDER | ${if (totalGoals < algorithmResults.underCouponHIGH_RISK) "[WIN]" else "[LOSE]"}\n" + "\n\n - FAVORITE COUPON: $decimalFavoriteCoupon ${if (algorithmResults.favoriteCouponIsOver) "OVER" else "UNDER"} | ${
                        if (algorithmResults.favoriteCouponIsOver) {
                            if (totalGoals > algorithmResults.favoriteCoupon) "[WIN]" else "[LOSE]"
                        } else {
                            if (totalGoals < algorithmResults.favoriteCoupon) "[WIN]" else "[LOSE]"
                        }
                    }\n"
                )
            } else {
                println(
                    "\n - PREDICTED SCORE: | ${algorithmResults.homeTeamName} $decimalHomeTeamPredictedScore : $decimalAwayTeamPredictedScore ${algorithmResults.awayTeamName} |\n" + "\n" + " - HOME PAAG: $decimalHomePAG | HOME PAYG: $decimalHomePYG\n" + " - AWAY PAAG: $decimalAwayPAG | AWAY PAYG: $decimalAwayPYG\n" + "\n - H2H-MIDG: $decimalH2hMIDG\n" + "\n - MING: $decimalMING\n" + " - MIDG: $decimalMIDG\n" + " - MAXG: $decimalMAXG\n" + "\n" + " - OVER COUPONS:\n" + " - LOW RISK: $decimalOverCouponLowRisk OVER | [UNDETERMINED]\n" + " - NORMAL: $decimalOverCouponNormal OVER | [UNDETERMINED]\n" + " - HIGH RISK: $decimalOverCouponHighRisk OVER | [UNDETERMINED]\n" + "\n" + " - UNDER COUPONS:\n" + " - LOW RISK: $decimalUnderCouponLowRisk UNDER | [UNDETERMINED]\n" + " - NORMAL: $decimalUnderCouponNormal UNDER | [UNDETERMINED]\n" + " - HIGH RISK: $decimalUnderCouponHighRisk UNDER | [UNDETERMINED]\n" + "\n\n - FAVORITE COUPON: $decimalFavoriteCoupon ${if (algorithmResults.favoriteCouponIsOver) "OVER" else "UNDER"} | [UNDETERMINED]\n"
                )
            }
        }
    }
}