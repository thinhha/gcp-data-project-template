/**
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mypackage.foundation.utils

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/**
 * Format an Instant as an ISO8601 timestamp
 */
fun Instant.toISO8601(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    .withZone(ZoneId.of("UTC")).format(this)

/**
 * Format a LocalDate in ISO8601 format
 */
fun LocalDate.toISO8601(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.of("UTC")).format(this)

/**
 * Find the day of week of an Instant
 */
fun Instant.firstDayOfWeek(): LocalDate = this.atZone(ZoneId.of("UTC")).toLocalDate()
    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Custom GSON configuration with support for serializing java.time classes into ISO8601 format
 */
fun gsonBuilder(): Gson = Converters.registerAll(GsonBuilder())
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    .create()

/**
 * Execute a block of code when a variable is not null
 */
fun <T : Any> T?.whenNotNull(callback: (it: T) -> Unit) {
    if (this != null) callback(this)
}

/**
 * Execute a block of code when a variable is null
 */
fun <T : Any> T?.whenNull(callback: () -> Unit) {
    this ?: callback()
}

/**
 * Decompress a Gzip-compressed byte array
 * @param data Data to decompress
 */
fun decompressGzip(data: ByteArray): String {
    return GZIPInputStream(data.inputStream()).bufferedReader(UTF_8).use { it.readText() }
}

/**
 * Compress a string to Gzip
 * @param data Data to compress
 */
fun compressGzip(data: String): ByteArray {
    val os = ByteArrayOutputStream()
    GZIPOutputStream(os).bufferedWriter(UTF_8).use { it.write(data) }
    return os.toByteArray()
}
