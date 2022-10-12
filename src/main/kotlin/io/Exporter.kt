package io

import TITLE_VERSION
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class Exporter {
    companion object {
        fun export(document: ArrayList<String>): Boolean {
            val fileName =
                "$TITLE_VERSION " + DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
                    .format(Instant.now())
            return try {
                val file = File(fileName)
                file.createNewFile()
                val fos = FileOutputStream(file)
                val bw = BufferedWriter(OutputStreamWriter(fos))
                for (line in document) {
                    bw.write(line)
                    bw.newLine()
                }
                bw.close()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
}