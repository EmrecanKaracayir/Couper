package algorithm

import SAFETY_MODE
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

        // ? AAG & AYG MIN MAX
        val homeTeamMinAAG = homeTeamAAG - data.fixtureToPredict.deviationHomeTeamAAG!!
        val homeTeamMaxAAG = homeTeamAAG + data.fixtureToPredict.deviationHomeTeamAAG!!
        val homeTeamMinAYG = homeTeamAYG - data.fixtureToPredict.deviationHomeTeamAYG!!
        val homeTeamMaxAYG = homeTeamAYG + data.fixtureToPredict.deviationHomeTeamAYG!!


        val awayTeamMinAAG = awayTeamAAG - data.fixtureToPredict.deviationAwayTeamAAG!!
        val awayTeamMaxAAG = awayTeamAAG + data.fixtureToPredict.deviationAwayTeamAAG!!
        val awayTeamMinAYG = awayTeamAYG - data.fixtureToPredict.deviationAwayTeamAYG!!
        val awayTeamMaxAYG = awayTeamAYG + data.fixtureToPredict.deviationAwayTeamAYG!!

        // ? MING
        val minGLastMatches =
            ((homeTeamMinAAG + awayTeamMinAYG) / 2 + (awayTeamMinAAG + homeTeamMinAYG) / 2)

        // ? MAXG
        val maxGLastMatches =
            ((homeTeamMaxAAG + awayTeamMaxAYG) / 2 + (awayTeamMaxAAG + homeTeamMaxAYG) / 2).coerceAtLeast(
                0.0
            )

        // ? MIDG
        val homeTeamMidPAG = ((homeTeamAAG + awayTeamAYG) / 2).coerceAtLeast(
            0.0
        )
        val awayTeamMidPAG = ((awayTeamAAG + homeTeamAYG) / 2).coerceAtLeast(
            0.0
        )

        val maxG: Double
        val midG: Double
        val minG: Double
        if (h2hFixturesTotalGoals.isNaN()) {
            maxG = maxGLastMatches

            midG = (homeTeamMidPAG + awayTeamMidPAG)

            minG = minGLastMatches
        } else {
            maxG = if (h2hFixturesTotalGoals < maxGLastMatches) maxGLastMatches
            else (h2hFixturesTotalGoals * H2H_MATCHES_COEFFICENT + maxGLastMatches * LAST_X_MATCHES_COEFFICENT) / (H2H_MATCHES_COEFFICENT + LAST_X_MATCHES_COEFFICENT)

            val h2hMatchesGoalsWithCoefficent =
                (h2hFixturesTotalGoals * H2H_MATCHES_COEFFICENT)
            val lastXMatchesMidGWithCoefficent =
                ((homeTeamMidPAG + awayTeamMidPAG) * LAST_X_MATCHES_COEFFICENT)
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
            val favoriteCouponVal =
                round((midG + minG * SAFETY_MODE.value) / (1 + SAFETY_MODE.value)) - 0.5
            if (favoriteCouponVal < 1){
                favoriteCoupon = overCouponNormal
                favoriteCouponIsOver = true
            } else {
                favoriteCoupon = favoriteCouponVal
                favoriteCouponIsOver = true
            }
        } else {
            val favoriteCouponVal =
                round((midG + maxG * SAFETY_MODE.value) / (1 + SAFETY_MODE.value)) + 0.5
            if (favoriteCouponVal > 4) {
                favoriteCoupon = underCouponNormal
                favoriteCouponIsOver = false
            } else {
                favoriteCoupon = favoriteCouponVal
                favoriteCouponIsOver = false
            }
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