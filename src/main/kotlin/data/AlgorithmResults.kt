package data

data class AlgorithmResults(
    val resultDeviation: Double,

    val homeTeamName: String,
    val awayTeamName: String,

    val homeTeamPredictedScore: Double,
    val awayTeamPredictedScore: Double,

    val homePAAG: Double,
    val homePAYG: Double,
    val awayPAAG: Double,
    val awayPAYG: Double,

    val h2hMIDG: Double,

    val minG: Double,
    val midG: Double,
    val maxG: Double,

    val overCouponLOW_RISK: Double,
    val overCouponNORMAL: Double,
    val overCouponHIGH_RISK: Double,

    val underCouponLOW_RISK: Double,
    val underCouponNORMAL: Double,
    val underCouponHIGH_RISK: Double,

    val favoriteCoupon: Double,
    val favoriteCouponIsOver: Boolean
)
