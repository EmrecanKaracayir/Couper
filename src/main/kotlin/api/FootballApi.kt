package api

import MAX_SEASON_DEPTH
import UTC_CONSTANT
import com.google.gson.Gson
import com.google.gson.JsonObject
import data.FixturePool
import model.Fixture
import model.League
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.DecimalFormat
import kotlin.system.exitProcess

class FootballApi {
    companion object {
        private val client = OkHttpClient()

        fun leagueRequestByCountryName(countryName: String): List<League> {
            val leagues = ArrayList<League>()

            val request = Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/leagues?country=$countryName&type=league")
                .get().addHeader(
                    "X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994"
                ).addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println(" - [EXIT] | NETWORK ERROR!")
                    exitProcess(-2)
                } else {
                    val json = response.body!!.string()
                    val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                        .getAsJsonPrimitive("results").asInt

                    if (resultCount <= 0) {
                        println(" - [EXIT] | NO DATA FOUND!")
                        exitProcess(-3)
                    }

                    for (i in 0 until resultCount) {
                        val responseObject =
                            Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("response")
                                .get(i).asJsonObject

                        val leagueObject = responseObject.getAsJsonObject("league")


                        var currentSeason = 0
                        var seasonC = 0
                        var season =
                            responseObject.getAsJsonArray("seasons").get(seasonC).asJsonObject
                        while (season != null) {
                            if (season.getAsJsonPrimitive("current").asBoolean) {
                                currentSeason = season.getAsJsonPrimitive("year").asInt
                                break
                            }
                            season =
                                responseObject.getAsJsonArray("seasons").get(++seasonC).asJsonObject
                        }

                        leagues.add(
                            League(
                                leagueObject.getAsJsonPrimitive("id").asInt,
                                leagueObject.getAsJsonPrimitive("name").asString,
                                currentSeason
                            )
                        )
                    }
                }
            }
            return leagues
        }

        fun leagueRequestById(leagueID: String): League {
            val request = Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/leagues?id=$leagueID").get()
                .addHeader(
                    "X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994"
                ).addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println(" - [EXIT] | NETWORK ERROR!")
                    exitProcess(-2)
                } else {
                    val json = response.body!!.string()
                    val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                        .getAsJsonPrimitive("results").asInt

                    if (resultCount <= 0) {
                        println(" - [EXIT] | NO DATA FOUND!")
                        exitProcess(-3)
                    }
                    val responseObject =
                        Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("response")
                            .get(0).asJsonObject

                    val leagueObject = responseObject.getAsJsonObject("league")

                    var currentSeason = 0
                    var seasonC = 0
                    var season = responseObject.getAsJsonArray("seasons").get(seasonC).asJsonObject
                    while (season != null) {
                        if (season.getAsJsonPrimitive("current").asBoolean) {
                            currentSeason = season.getAsJsonPrimitive("year").asInt
                            break
                        }
                        season =
                            responseObject.getAsJsonArray("seasons").get(++seasonC).asJsonObject
                    }

                    return League(
                        leagueObject.getAsJsonPrimitive("id").asInt,
                        leagueObject.getAsJsonPrimitive("name").asString,
                        currentSeason
                    )
                }
            }
        }

        fun leagueCurrentRoundRequest(leagueID: String, leagueSeason: Int): Int {
            val curLeagueRound: Int
            val request = Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/fixtures/rounds?league=$leagueID&season=$leagueSeason&current=true")
                .get()
                .addHeader("X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994")
                .addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                println(" - [EXIT] | NETWORK ERROR!")
                exitProcess(-2)
            } else {
                val json = response.body!!.string()
                val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                    .getAsJsonPrimitive("results").asInt

                if (resultCount <= 0) {
                    println(" - [EXIT] | NO DATA FOUND!")
                    exitProcess(-3)
                }

                val currentRoundStr =
                    Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("response")
                        .get(0).asJsonPrimitive.asString
                val curLeagueRoundSub = currentRoundStr.filter { it.isDigit() }
                if (curLeagueRoundSub.isEmpty()) exitProcess(-99)
                curLeagueRound = curLeagueRoundSub.toInt()
            }
            return curLeagueRound
        }

        fun fixturePoolFiller(leagueID: String, leagueSeason: Int): FixturePool {
            val fixtureList = ArrayList<Fixture>()
            for (seasonC in 0 until MAX_SEASON_DEPTH) {
                val request = Request.Builder()
                    .url("https://api-football-v1.p.rapidapi.com/v3/fixtures?league=$leagueID&season=${leagueSeason - seasonC}")
                    .get().addHeader(
                        "X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994"
                    ).addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println(" - [EXIT] | NETWORK ERROR!")
                        exitProcess(-2)
                    } else {
                        val json = response.body!!.string()
                        val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                            .getAsJsonPrimitive("results").asInt

                        if (resultCount <= 0) {
                            println(" - [EXIT] | NO DATA FOUND!")
                            exitProcess(-3)
                        }

                        season@ for (resultC in 0 until resultCount) {
                            val responseObject = Gson().fromJson(json, JsonObject::class.java)
                                .getAsJsonArray("response").get(resultC).asJsonObject

                            val fixtureID = responseObject.getAsJsonObject("fixture")
                                .getAsJsonPrimitive("id").asString
                            val fixtureTimeStamp = responseObject.getAsJsonObject("fixture")
                                .getAsJsonPrimitive("timestamp").asInt
                            val fixtureStatus =
                                responseObject.getAsJsonObject("fixture").getAsJsonObject("status")
                                    .getAsJsonPrimitive("short").asString
                            val fixtureDatePhase1 = responseObject.getAsJsonObject("fixture")
                                .getAsJsonPrimitive("date").asString
                            val fixtureDateTimeUTC = fixtureDatePhase1.substring(
                                fixtureDatePhase1.indexOf('T') + 1,
                                fixtureDatePhase1.indexOf('T') + 6
                            )
                            val fixtureTimeHours = fixtureDateTimeUTC.substring(0, 2).toInt()

                            val fixtureDateTime =
                                fixtureDatePhase1.substring(0, fixtureDatePhase1.indexOf('T'))
                                    .replace('-', '/')

                            var fixtureDateTimeLocalHour = fixtureTimeHours + UTC_CONSTANT
                            if (fixtureDateTimeLocalHour > 24) {
                                fixtureDateTimeLocalHour -= 24
                            }

                            val fixtureDateTimeLocalMinutes = fixtureDateTimeUTC.substring(3, 5)

                            val fixtureDateTimeLocal =
                                "$fixtureDateTimeLocalHour:$fixtureDateTimeLocalMinutes"

                            val curLeagueSeason = responseObject.getAsJsonObject("league")
                                .getAsJsonPrimitive("season").asInt
                            val curLeagueRoundStr = responseObject.getAsJsonObject("league")
                                .getAsJsonPrimitive("round").asString
                            val curLeagueRoundSub = curLeagueRoundStr.filter { it.isDigit() }
                            if (curLeagueRoundSub.isEmpty()) break@season
                            val curLeagueRound = curLeagueRoundSub.toInt()

                            val homeTeamID =
                                responseObject.getAsJsonObject("teams").getAsJsonObject("home")
                                    .getAsJsonPrimitive("id").asString
                            val homeTeamName =
                                responseObject.getAsJsonObject("teams").getAsJsonObject("home")
                                    .getAsJsonPrimitive("name").asString

                            val awayTeamID =
                                responseObject.getAsJsonObject("teams").getAsJsonObject("away")
                                    .getAsJsonPrimitive("id").asString
                            val awayTeamName =
                                responseObject.getAsJsonObject("teams").getAsJsonObject("away")
                                    .getAsJsonPrimitive("name").asString

                            var homeTeamScore: Int? = null
                            var awayTeamScore: Int? = null
                            if (fixtureStatus == "FT") {
                                homeTeamScore = responseObject.getAsJsonObject("goals")
                                    .getAsJsonPrimitive("home").asInt
                                awayTeamScore = responseObject.getAsJsonObject("goals")
                                    .getAsJsonPrimitive("away").asInt
                            }

                            fixtureList.add(
                                Fixture(
                                    fixtureID,
                                    fixtureTimeStamp,
                                    fixtureStatus,
                                    fixtureDate = "$fixtureDateTime $fixtureDateTimeLocal",
                                    leagueID,
                                    curLeagueSeason,
                                    curLeagueRound,
                                    homeTeamID,
                                    homeTeamName,
                                    awayTeamID,
                                    awayTeamName,
                                    homeTeamScore,
                                    awayTeamScore
                                )
                            )
                        }
                    }
                }
            }
            fixtureList.sortBy { f -> f.fixtureTimeStamp }
            return FixturePool(fixtureList)
        }

        fun h2hMatchesRequest(
            fixtureTimeStamp: Int,
            leagueID: String,
            homeTeamID: String, homeTeamName: String,
            awayTeamID: String, awayTeamName: String,
        ): List<Fixture> {
            val h2hLastFixtures = ArrayList<Fixture>()

            val request = Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/fixtures/headtohead?h2h=$homeTeamID-$awayTeamID&status=FT")
                .get()
                .addHeader("X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994")
                .addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println(" - [EXIT] | NETWORK ERROR!")
                    exitProcess(-2)
                } else {
                    val json = response.body!!.string()
                    val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                        .getAsJsonPrimitive("results").asInt
                    for (resultC in 0 until resultCount) {
                        val responseObject =
                            Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("response")
                                .get(resultC).asJsonObject

                        val fixtureID = responseObject.getAsJsonObject("fixture")
                            .getAsJsonPrimitive("id").asString
                        val curFixtureTimeStamp = responseObject.getAsJsonObject("fixture")
                            .getAsJsonPrimitive("timestamp").asInt
                        val curLeagueID = responseObject.getAsJsonObject("league")
                            .getAsJsonPrimitive("id").asString

                        if (curLeagueID == leagueID && curFixtureTimeStamp < fixtureTimeStamp) {
                            val curLeagueSeason = responseObject.getAsJsonObject("league")
                                .getAsJsonPrimitive("season").asInt
                            val curLeagueRoundStr = responseObject.getAsJsonObject("league")
                                .getAsJsonPrimitive("round").asString
                            val curLeagueRoundSub = curLeagueRoundStr.filter { it.isDigit() }
                            if (curLeagueRoundSub.isEmpty()) break
                            val curLeagueRound = curLeagueRoundSub.toInt()

                            val homeTeamScore = responseObject.getAsJsonObject("goals")
                                .getAsJsonPrimitive("home").asInt
                            val awayTeamScore = responseObject.getAsJsonObject("goals")
                                .getAsJsonPrimitive("away").asInt

                            h2hLastFixtures.add(
                                Fixture(
                                    fixtureID,
                                    curFixtureTimeStamp,
                                    fixtureStatus = "FT",
                                    fixtureDate = "NO INFO",
                                    leagueID,
                                    curLeagueSeason,
                                    curLeagueRound,
                                    homeTeamID,
                                    homeTeamName,
                                    awayTeamID,
                                    awayTeamName,
                                    homeTeamScore,
                                    awayTeamScore
                                )
                            )
                        }
                    }
                }
            }
            h2hLastFixtures.sortByDescending { it.fixtureTimeStamp }

            return h2hLastFixtures
        }

        fun apiPredictionsPhase(selectedFixture: Fixture) {
            var totalGoals = -1

            if (selectedFixture.homeTeamScore != null && selectedFixture.awayTeamScore != null) totalGoals =
                selectedFixture.homeTeamScore + selectedFixture.awayTeamScore

            println("\n - API PREDICTIONS: Requesting...")

            val request = Request.Builder()
                .url("https://api-football-v1.p.rapidapi.com/v3/predictions?fixture=${selectedFixture.fixtureID}")
                .get()
                .addHeader("X-RapidAPI-Key", "0c26146e9amsh89ddfbd276994e5p133e6fjsnd250f107e994")
                .addHeader("X-RapidAPI-Host", "api-football-v1.p.rapidapi.com").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println(" - [EXIT] | NETWORK ERROR!")
                    exitProcess(-2)
                } else {
                    val json = response.body!!.string()
                    val resultCount: Int = Gson().fromJson(json, JsonObject::class.java)
                        .getAsJsonPrimitive("results").asInt
                    if (resultCount > 0) {
                        val responseObject =
                            Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("response")
                                .get(0).asJsonObject
                        try {
                            var underOverPrediction = responseObject.getAsJsonObject("predictions")
                                .getAsJsonPrimitive("under_over").asString
                            if (underOverPrediction[0] == '-') {
                                underOverPrediction = underOverPrediction.drop(1)
                                val predictionDouble = underOverPrediction.toDouble()
                                val decimalPrediction =
                                    DecimalFormat("#.##").format(predictionDouble)
                                if (totalGoals != -1) {
                                    println("\n - API PREDICTION: $decimalPrediction UNDER | ${if (predictionDouble > totalGoals) "[WIN]" else "[LOSE]"}")
                                } else {
                                    println("\n - API PREDICTION: $decimalPrediction UNDER | [UNDETERMINED]")
                                }
                            } else {
                                underOverPrediction = underOverPrediction.drop(1)
                                val predictionDouble = underOverPrediction.toDouble()
                                val decimalPrediction =
                                    DecimalFormat("#.##").format(predictionDouble)
                                if (totalGoals != -1) {
                                    println("\n - API PREDICTION: $decimalPrediction OVER | ${if (predictionDouble < totalGoals) "[WIN]" else "[LOSE]"}")
                                } else {
                                    println("\n - API PREDICTION: $decimalPrediction OVER | [UNDETERMINED]")
                                }
                            }
                        } catch (e: ClassCastException) {
                            println("\n - API PREDICTION: NO PREDICTION PROVIDED FROM API!")
                        }
                    }
                }
            }
        }
    }
}