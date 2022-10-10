package algorithm.data

import algorithm.*
import algorithm.model.SidedFixture
import algorithm.utils.isHomeTeam
import algorithm.utils.isInSameSeason
import algorithm.utils.isSideInCorrectPosition
import java.util.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
class AnalysisList(
    sidedFixtureTBA: SidedFixture
) {
    val nodeTBA: Node = Node(sidedFixtureTBA)
    private var childNodeCount = 0

    fun appendSidedFixtures(sidedFixturesToAppend: List<SidedFixture>) {
        for (sidedFixture in sidedFixturesToAppend) {
            val nodeToAppend = Node(sidedFixture, nodeOrder = childNodeCount)
            var lastNode = nodeTBA
            while (lastNode.hasDownNode()) lastNode = lastNode.downNode!!
            lastNode.downNode = nodeToAppend
            childNodeCount++
        }
    }

    fun isEmpty(): Boolean = !nodeTBA.hasDownNode()

    fun lastSidedFixtureNotCalculated(): SidedFixture? {
        var lastSidedFixtureNotCalculated: SidedFixture? = null
        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (!lastNode.sidedFixture.fixture.isFixtureAnalyzedForTeamID(lastNode.sidedFixture.sideTeamID)) lastSidedFixtureNotCalculated =
                lastNode.sidedFixture
        }
        return lastSidedFixtureNotCalculated
    }

    fun analyze() {
        if (nodeTBA.hasDownNode()) analysisChain()
        else {
            nodeTBA.sidedFixture.fixture.homeTeamAAG =
                nodeTBA.sidedFixture.fixture.homeTeamScore!!.toDouble()
            nodeTBA.sidedFixture.fixture.homeTeamAYG =
                nodeTBA.sidedFixture.fixture.awayTeamScore!!.toDouble()

            nodeTBA.sidedFixture.fixture.awayTeamAAG =
                nodeTBA.sidedFixture.fixture.awayTeamScore!!.toDouble()
            nodeTBA.sidedFixture.fixture.awayTeamAYG =
                nodeTBA.sidedFixture.fixture.homeTeamScore!!.toDouble()
        }
    }

    private fun analysisChain() {
        childWeightedScoresCalculator()
        if (childNodeCount > 1) {
            childWeightAssigner()
            childWeightPenaltyAssigner()
        }
        parentWeightAssigner()
    }

    private fun childWeightedScoresCalculator() {
        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isHomeTeam(lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID)) {
                lastNode.sidedFixture.fixture.homeTeamAAG =
                    lastNode.sidedFixture.fixture.homeTeamScore!! + (lastNode.sidedFixture.fixture.homeTeamScore!! - lastNode.sidedFixture.fixture.awayTeamAYG!!) * SCORE_WEIGHT_DIFFERENCE_COEFFICENT

                lastNode.sidedFixture.fixture.homeTeamAYG =
                    lastNode.sidedFixture.fixture.awayTeamScore!! + (lastNode.sidedFixture.fixture.awayTeamScore!! - lastNode.sidedFixture.fixture.awayTeamAAG!!) * SCORE_WEIGHT_DIFFERENCE_COEFFICENT
            } else {
                lastNode.sidedFixture.fixture.awayTeamAAG =
                    lastNode.sidedFixture.fixture.awayTeamScore!! + (lastNode.sidedFixture.fixture.awayTeamScore!! - lastNode.sidedFixture.fixture.homeTeamAYG!!) * SCORE_WEIGHT_DIFFERENCE_COEFFICENT

                lastNode.sidedFixture.fixture.awayTeamAYG =
                    lastNode.sidedFixture.fixture.homeTeamScore!! + (lastNode.sidedFixture.fixture.homeTeamScore!! - lastNode.sidedFixture.fixture.homeTeamAAG!!) * SCORE_WEIGHT_DIFFERENCE_COEFFICENT
            }
        }
    }

    private fun childWeightAssigner() {
        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            lastNode.sidedFixture.fixture.weight = formWeightCalculator(lastNode.nodeOrder + 1)
        }

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isInSameSeason(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.fixture
                )
            ) lastNode.sidedFixture.fixture.weight *= NEW_SEASON_COEFFICENT

            if (isSideInCorrectPosition(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture
                )
            ) lastNode.sidedFixture.fixture.weight *= HOME_AWAY_CORRECT_POSITION_COEFFICENT
            else lastNode.sidedFixture.fixture.weight *= HOME_AWAY_WRONG_POSITION_COEFFICENT
        }
    }

    private fun formWeightCalculator(order: Int): Double {
        return BASE_VALUE_OF_COEFFICENT.pow(order)
    }

    private fun childWeightPenaltyAssigner() {
        // AAG STANDARD DEVIATION
        val aagAllDiffs = ArrayList<Double>()
        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isHomeTeam(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID
                )
            ) aagAllDiffs.add(lastNode.sidedFixture.fixture.homeTeamAAG!! - lastNode.sidedFixture.fixture.homeTeamScore!!)
            else aagAllDiffs.add(lastNode.sidedFixture.fixture.awayTeamAAG!! - lastNode.sidedFixture.fixture.awayTeamScore!!)
        }
        val aagAllDiffsMean = aagAllDiffs.sum() / childNodeCount

        var aagDiffsVariantSum = 0.0
        for (aag in aagAllDiffs) aagDiffsVariantSum += (aag - aagAllDiffsMean).pow(2)

        val aagDiffSD = sqrt(aagDiffsVariantSum / (childNodeCount - 1))

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            val aagDiff = if (isHomeTeam(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID
                )
            ) lastNode.sidedFixture.fixture.homeTeamAAG!! - lastNode.sidedFixture.fixture.homeTeamScore!!
            else lastNode.sidedFixture.fixture.awayTeamAAG!! - lastNode.sidedFixture.fixture.awayTeamScore!!
            if (aagDiff > (aagAllDiffsMean + aagDiffSD * STANDARD_DEVIATION_COEFFICENT) || aagDiff < (aagAllDiffsMean - aagDiffSD * STANDARD_DEVIATION_COEFFICENT)) lastNode.sidedFixture.fixture.weight /= STANDARD_DEVIATION_PENALTY_COEFFICENT
        }

        // AYG STANDARD DEVIATION
        val aygAllDiffs = ArrayList<Double>()

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isHomeTeam(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID
                )
            ) aygAllDiffs.add(lastNode.sidedFixture.fixture.homeTeamAYG!! - lastNode.sidedFixture.fixture.awayTeamScore!!)
            else aygAllDiffs.add(lastNode.sidedFixture.fixture.awayTeamAYG!! - lastNode.sidedFixture.fixture.homeTeamScore!!)
        }
        val aygAllDiffsMean = aygAllDiffs.sum() / childNodeCount

        var aygDiffsVariantSum = 0.0
        for (ayg in aygAllDiffs) aygDiffsVariantSum += (ayg - aygAllDiffsMean).pow(2)

        val aygDiffSD = sqrt(aygDiffsVariantSum / (childNodeCount - 1))

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            val aygDiff = if (isHomeTeam(
                    lastNode.sidedFixture.fixture, lastNode.sidedFixture.sideTeamID
                )
            ) lastNode.sidedFixture.fixture.homeTeamAYG!! - lastNode.sidedFixture.fixture.awayTeamScore!!
            else lastNode.sidedFixture.fixture.awayTeamAYG!! - lastNode.sidedFixture.fixture.homeTeamScore!!
            if (aygDiff > (aygAllDiffsMean + aygDiffSD * STANDARD_DEVIATION_COEFFICENT) || aygDiff < (aygAllDiffsMean - aygDiffSD * STANDARD_DEVIATION_COEFFICENT)) lastNode.sidedFixture.fixture.weight /= STANDARD_DEVIATION_PENALTY_COEFFICENT
        }
    }

    private fun parentWeightAssigner() {
        // PAAG CALCULATION
        val aags = DoubleArray(childNodeCount) { 0.0 }

        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isHomeTeam(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID
                )
            ) aags[lastNode.nodeOrder] = lastNode.sidedFixture.fixture.homeTeamAAG!!
            else aags[lastNode.nodeOrder] = lastNode.sidedFixture.fixture.awayTeamAAG!!
        }

        var aagTotal = 0.0
        var aagFixtureCoefficentSum = 0.0
        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            aagTotal += aags[lastNode.nodeOrder] * lastNode.sidedFixture.fixture.weight
            aagFixtureCoefficentSum += lastNode.sidedFixture.fixture.weight
        }

        if (nodeTBA.sidedFixture.isSideHome()) nodeTBA.sidedFixture.fixture.homeTeamAAG =
            aagTotal / aagFixtureCoefficentSum
        else nodeTBA.sidedFixture.fixture.awayTeamAAG = aagTotal / aagFixtureCoefficentSum

        // PAYG CALCULATION
        val aygs = DoubleArray(childNodeCount) { 0.0 }

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            if (isHomeTeam(
                    lastNode.sidedFixture.fixture, nodeTBA.sidedFixture.sideTeamID
                )
            ) aygs[lastNode.nodeOrder] = lastNode.sidedFixture.fixture.homeTeamAYG!!
            else aygs[lastNode.nodeOrder] = lastNode.sidedFixture.fixture.awayTeamAYG!!
        }

        var aygTotal = 0.0
        var aygFixtureCoefficentSum = 0.0
        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            aygTotal += aygs[lastNode.nodeOrder] * lastNode.sidedFixture.fixture.weight
            aygFixtureCoefficentSum += lastNode.sidedFixture.fixture.weight
        }

        if (nodeTBA.sidedFixture.isSideHome()) nodeTBA.sidedFixture.fixture.homeTeamAYG =
            aygTotal / aygFixtureCoefficentSum
        else nodeTBA.sidedFixture.fixture.awayTeamAYG = aygTotal / aygFixtureCoefficentSum
    }

    fun analyzeH2H(): Double? {
        return if (nodeTBA.hasDownNode()) analysisChainH2H() else null
    }

    private fun analysisChainH2H(): Double {
        childWeightAssigner()
        childScoreDifferencePenaltyAssigner()
        return parentTotalGoalsProvider()
    }

    private fun childScoreDifferencePenaltyAssigner() {
        val h2hAllGoals = ArrayList<Int>()

        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            h2hAllGoals.add(lastNode.sidedFixture.fixture.homeTeamScore!! + lastNode.sidedFixture.fixture.awayTeamScore!!)
        }

        val h2hAllGoalsMean = h2hAllGoals.sum().toDouble() / childNodeCount

        var h2hVariantSum = 0.0
        for (goalC in h2hAllGoals) h2hVariantSum += (goalC - h2hAllGoalsMean).pow(2)

        val h2hSD = sqrt(h2hVariantSum / (childNodeCount - 1))

        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            val goalC =
                lastNode.sidedFixture.fixture.homeTeamScore!! + lastNode.sidedFixture.fixture.awayTeamScore!!
            if (goalC > (h2hAllGoalsMean + h2hSD * STANDARD_DEVIATION_COEFFICENT) || goalC < (h2hAllGoalsMean - h2hSD * STANDARD_DEVIATION_COEFFICENT)) lastNode.sidedFixture.fixture.weight /= STANDARD_DEVIATION_PENALTY_COEFFICENT
        }
    }

    private fun parentTotalGoalsProvider(): Double {
        val goals = IntArray(childNodeCount) { 0 }

        var lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            lastNode = lastNode.downNode!!
            goals[lastNode.nodeOrder] =
                lastNode.sidedFixture.fixture.homeTeamScore!! + lastNode.sidedFixture.fixture.awayTeamScore!!
        }

        var totalScore = 0.0
        var fixtureCoefficentsSum = 0.0
        lastNode = nodeTBA
        while (lastNode.hasDownNode()) {
            totalScore += goals[lastNode.nodeOrder] * lastNode.sidedFixture.fixture.weight
            fixtureCoefficentsSum += lastNode.sidedFixture.fixture.weight
        }

        return totalScore / fixtureCoefficentsSum
    }
}