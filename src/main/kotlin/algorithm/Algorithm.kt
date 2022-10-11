package algorithm

import algorithm.manager.H2hAnalysisManager
import algorithm.manager.OpponentAnalysisManager
import algorithm.model.SidedFixture
import data.AlgorithmData
import data.AlgorithmResults
import kotlin.math.*

const val BASE_VALUE_OF_COEFFICENT = 1.25
const val NEW_SEASON_COEFFICENT = 10
const val H2H_MATCHES_COEFFICENT = 25
const val LAST_X_MATCHES_COEFFICENT = 75
const val HOME_AWAY_CORRECT_POSITION_COEFFICENT = 6
const val HOME_AWAY_WRONG_POSITION_COEFFICENT = 4
const val SCORE_WEIGHT_DIFFERENCE_COEFFICENT = 1
const val STANDARD_DEVIATION_COEFFICENT = 1
const val STANDARD_DEVIATION_PENALTY_COEFFICENT = 5
const val PAAG_COEFFICENT = 50
const val PAYG_COEFFICENT = 50

class Algorithm(private val data: AlgorithmData) {
    private val oam = OpponentAnalysisManager(data)
    private val h2ham = H2hAnalysisManager(data)

    fun getResults(): AlgorithmResults {

        val homeSidedFixtureTBA =
            SidedFixture(data.fixtureToPredict, data.fixtureToPredict.homeTeamID)
        oam.analyzeOpponent(homeSidedFixtureTBA)
        val homeTeamAAG = homeSidedFixtureTBA.fixture.homeTeamAAG!!
        val homeTeamAYG = homeSidedFixtureTBA.fixture.homeTeamAYG!!

        val awaySidedFixtureTBA =
            SidedFixture(data.fixtureToPredict, data.fixtureToPredict.awayTeamID)
        oam.analyzeOpponent(awaySidedFixtureTBA)
        val awayTeamAAG = awaySidedFixtureTBA.fixture.awayTeamAAG!!
        val awayTeamAYG = awaySidedFixtureTBA.fixture.awayTeamAYG!!

        val h2hFixturesTotalGoals =
            h2ham.analyzeH2H(SidedFixture(data.fixtureToPredict, data.fixtureToPredict.homeTeamID))
                ?: Double.NaN

        val maxGLastMatches = (max(
            homeTeamAAG.coerceAtLeast(0.0), awayTeamAYG.coerceAtLeast(0.0)
        ) + max(awayTeamAAG.coerceAtLeast(0.0), homeTeamAYG.coerceAtLeast(0.0)))

        val homeTeamMidPAG =
            ((homeTeamAAG * PAAG_COEFFICENT + awayTeamAYG * PAYG_COEFFICENT) / (PAAG_COEFFICENT + PAYG_COEFFICENT)).coerceAtLeast(
                0.0
            )
        val awayTeamMidPAG =
            ((awayTeamAAG * PAAG_COEFFICENT + homeTeamAYG * PAYG_COEFFICENT) / (PAAG_COEFFICENT + PAYG_COEFFICENT)).coerceAtLeast(
                0.0
            )

        val minGLastMatches = (min(
            homeTeamAAG.coerceAtLeast(0.0), awayTeamAYG.coerceAtLeast(0.0)
        ) + min(
            awayTeamAAG.coerceAtLeast(0.0), homeTeamAYG.coerceAtLeast(0.0)
        ))

        val maxG: Double
        val midG: Double
        val minG: Double
        if (h2hFixturesTotalGoals.isNaN()) {
            maxG = maxGLastMatches

            midG = (homeTeamMidPAG + awayTeamMidPAG).coerceAtLeast(0.0)

            minG = minGLastMatches
        } else {
            maxG = if (h2hFixturesTotalGoals < maxGLastMatches) maxGLastMatches
            else (h2hFixturesTotalGoals * H2H_MATCHES_COEFFICENT + maxGLastMatches * LAST_X_MATCHES_COEFFICENT) / (H2H_MATCHES_COEFFICENT + LAST_X_MATCHES_COEFFICENT)

            val h2hMatchesGoalsWithCoefficent =
                (h2hFixturesTotalGoals * H2H_MATCHES_COEFFICENT).coerceAtLeast(0.0)
            val lastXMatchesMidGWithCoefficent =
                ((homeTeamMidPAG + awayTeamMidPAG) * LAST_X_MATCHES_COEFFICENT).coerceAtLeast(0.0)
            midG =
                (h2hMatchesGoalsWithCoefficent + lastXMatchesMidGWithCoefficent) / (H2H_MATCHES_COEFFICENT + LAST_X_MATCHES_COEFFICENT)

            minG = if (h2hFixturesTotalGoals > minGLastMatches) minGLastMatches
            else (h2hFixturesTotalGoals * H2H_MATCHES_COEFFICENT + minGLastMatches * LAST_X_MATCHES_COEFFICENT) / (H2H_MATCHES_COEFFICENT + LAST_X_MATCHES_COEFFICENT)
        }

        // Over Coupons
        var minGFloor = floor(minG).toInt()
        val midGFloor = floor(midG).toInt()
        val maxGFloor = floor(maxG).toInt()

        if (minGFloor == midGFloor) --minGFloor

        val overCouponLowRisk = minGFloor - 0.5
        val overCouponNormal = midGFloor - 0.5
        val overCouponHighRisk = maxGFloor - 0.5

        // Under Coupons
        var maxGCeil = ceil(maxG).toInt()
        val midGCeil = ceil(midG).toInt()
        var minGCeil = ceil(minG).toInt()

        if (maxGCeil == midGCeil) ++maxGCeil

        if (minGCeil <= 0) minGCeil = 0

        val underCouponLowRisk = maxGCeil + 0.5
        val underCouponNormal = midGCeil + 0.5
        val underCouponHighRisk = minGCeil + 0.5

        val favoriteCouponIsOver: Boolean
        val favoriteCoupon: Double
        if (maxG - midG > midG - minG) {
            val favoriteCouponVal = round((midG + minG) / 2) - 0.5
            favoriteCoupon = if (favoriteCouponVal < 0) 0.5
            else favoriteCouponVal
            favoriteCouponIsOver = true
        } else {
            favoriteCoupon = round((midG + maxG) / 2) + 0.5
            favoriteCouponIsOver = false
        }

        return AlgorithmResults(
            resultDeviationCalculator(minG, midG, maxG),
            data.fixtureToPredict.homeTeamName,
            data.fixtureToPredict.awayTeamName,
            homeTeamMidPAG,
            awayTeamMidPAG,
            homeTeamAAG,
            homeTeamAYG,
            awayTeamAAG,
            awayTeamAYG,
            h2hFixturesTotalGoals,
            minG,
            midG,
            maxG,
            overCouponLowRisk,
            overCouponNormal,
            overCouponHighRisk,
            underCouponLowRisk,
            underCouponNormal,
            underCouponHighRisk,
            favoriteCoupon,
            favoriteCouponIsOver
        )
    }

    private fun resultDeviationCalculator(minG: Double, midG: Double, maxG: Double): Double {
        val arr = arrayOf(minG, midG, maxG)

        var sum = 0.0
        var standardDeviation = 0.0
        for (num in arr) sum += num
        val mean = sum / arr.size
        for (num in arr) standardDeviation += (num - mean).pow(2.0)

        return sqrt(standardDeviation / arr.size)
    }
}