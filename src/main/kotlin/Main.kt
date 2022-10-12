import modes.ExportMode
import modes.NormalMode
import modes.StatisticsMode
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

enum class Safety(val value: Double) {
    SAFEST(4.00), SAFE(2.00), NORMAL(1.00), RISKY(0.50), RISKIEST(0.25)
}

const val TITLE_VERSION = "COUPER V:1.2.6"
const val UTC_CONSTANT = 3
const val MAX_SEASON_DEPTH = 3

val SAFETY_MODE = Safety.NORMAL
const val MAX_ACCEPTABLE_RESULT_DEVIATION = Double.MAX_VALUE

fun main() {
    System.setOut(
        PrintStream(
            FileOutputStream(FileDescriptor.out), true, "UTF-8"
        )
    )

    println("\n========== $TITLE_VERSION ==========")
    println("\n - Welcome to coupon advisor Couper!\n")

    print(" > Enter \"normal\", or enter \"export\", or enter \"statistics\", for mode selection: ")

    when (readln().lowercase()) {
        "n", "normal" -> {
            println("\n - Normal mode selected!\n")
            NormalMode.normalMode()
        }

        "e", "export" -> {
            println("\n - Export mode selected!")
            ExportMode.exportMode()
        }

        "s", "statistics" -> {
            println("\n - Statistics mode selected!\n")
            StatisticsMode.statisticsMode()
        }

        else -> {
            println("\n - Wrong mode selection!\n")
            exitProcess(1)
        }
    }
}
