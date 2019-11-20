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

package com.mypackage.data.poc

import com.google.api.services.bigquery.model.TableRow
import mu.KotlinLogging
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.Default
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.time.Instant
import java.util.*
import com.mypackage.foundation.messages.DataMessage
import com.mypackage.foundation.utils.*


private val logger = KotlinLogging.logger {}

interface Gcs2BqPipelineConfig : DataflowPipelineOptions {

    @get:Default.String("non-existent-bucket")
    var inputGCSPath: String
    @get:Default.String("dataset_name.table_name")
    var outputBigQueryTable: String?
}

/**
 * Main application class
 */
class Gcs2BqPipeline {

    companion object {

        /**
         * Application entry point.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            configureLogger()
            createPipeline(args)
        }

        /**
         * Configure logging framework.
         */
        private fun configureLogger() {
            Configurator.setAllLevels(LogManager.getLogger("com.mypackage.data.poc").getName(), Level.INFO)
        }

        /**
         * Create the base pipeline from an array of options.
         *
         * @param args Array of option arguments.
         */
        private fun createPipeline(args: Array<String>) {

            // Create the pipeline with options
            val options = PipelineOptionsFactory.fromArgs(*args).withValidation()
                .`as`(Gcs2BqPipelineConfig::class.java)

            val (dataset, table) = options.outputBigQueryTable?.split(".").orEmpty()
            val runId = UUID.randomUUID()

            logger.info {
                """Runner: ${options.runner.name}
                    |Job name: ${options.jobName}
                """.trimMargin()
            }

            // Create a (very) basic pipeline
            val pipeline = Pipeline.create(options)

            pipeline
                .fromText("Read from GCS", options.inputGCSPath)
                .map("Insert into BigQuery") { line ->
                    val message = gsonBuilder().fromJson<DataMessage>(line, DataMessage::class.java)
                    TableRow()
                        .set("id", message.id)
                        .set("name", message.name)
                        .set("location", message.location)
                        .set("run_id", runId)
                        .set("insertion_timestamp", Instant.now().toISO8601())
                }
                .toBigquery(table = table,
                    dataset = dataset,
                    project = options.project)

            // Run the pipeline
            pipeline.run()

        }

    }
}
