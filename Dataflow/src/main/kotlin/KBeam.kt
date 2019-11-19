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

import com.google.api.services.bigquery.model.TableReference
import com.google.api.services.bigquery.model.TableRow
import com.google.cloud.spanner.Mutation
import mu.KotlinLogging
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.NullableCoder
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.WriteResult
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage
import org.apache.beam.sdk.io.gcp.spanner.SpannerIO
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.io.kafka.KafkaRecord
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.*
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.PDone
import org.apache.beam.sdk.values.TupleTag
import org.apache.beam.sdk.values.TypeDescriptor
import org.apache.kafka.common.serialization.Deserializer

private val logger = KotlinLogging.logger {}


fun <A, B> ((A) -> B).toSerializableFunction(): SerializableFunction<A, B> = SerializableFunction { this(it) }

/**
 * Adapted and extended from https://github.com/Dan-Dongcheol-Lee/apachebeam-kotlin-blog-examples
 */
object KPipeline {
    inline fun <reified R : PipelineOptions> from(args: Array<String>): Pair<Pipeline, R> {
        val options = PipelineOptionsFactory.fromArgs(*args)
            .withValidation()
            .`as`(R::class.java)
        return Pipeline.create(options) to options
    }
}

/**
 * Read data from a text file.
 */
fun Pipeline.fromText(name: String = "Read from Text", path: String): PCollection<String> {
    return this.apply(name, TextIO.read().from(path))
}

/**
 * Write data to a text file.
 */
fun PCollection<String>.toText(name: String = "Write to Text", filename: String): PDone {
    return this.apply(name, TextIO.write().to(filename))
}

/**
 * Run a function on each member of a PCollect
 */
inline fun <I, reified O> PCollection<I>.map(
    name: String = "Map to ${O::class.simpleName}",
    noinline transform: (I) -> O): PCollection<O> {
    val pc = this.apply(name, MapElements.into(TypeDescriptor.of(O::class.java))
        .via(transform.toSerializableFunction()))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

/**
 * Convert a nested PCollect into a single PCollect
 */
inline fun <I, reified O> PCollection<I>.flatMap(
    name: String = "FlatMap to ${O::class.simpleName}",
    noinline transform: (I) -> Iterable<O>): PCollection<O> {
    val pc = this.apply(name, FlatMapElements.into(TypeDescriptor.of(O::class.java))
        .via(transform.toSerializableFunction()))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

/**
 * Filter items in a PCollect.
 */
fun <I> PCollection<I>.filter(
    name: String = "Filter items",
    transform: (I) -> Boolean): PCollection<I> {
    val pc = this.apply(name, Filter.by(transform.toSerializableFunction()))
    return pc.setCoder(NullableCoder.of(pc.coder))
}

/**
 * Read data from a Kafka queue.
 */
inline fun <K, reified V>Pipeline.fromKafka(
    name: String = "Read from Kafka",
    brokers: String,
    topics: List<String>,
    keyDeserializer: Class<out Deserializer<K>>,
    valueDeserializer: Class<out Deserializer<V>>): PCollection<KafkaRecord<K,V>> {
    return this.apply(name,
        KafkaIO.read<K,V>()
            .withBootstrapServers(brokers)
            .withTopics(topics)
            .withKeyDeserializer(keyDeserializer)
            .withValueDeserializer(valueDeserializer)
            .withReadCommitted()
    )
}

/**
 * Write data to a PubSub topic.
 */
fun PCollection<PubsubMessage>.toPubsub(name: String = "Write to PubSub", topic: String,
                                        idAttribute: String?, maxBatchSize: Int? = 10): PDone {
    return this.apply(name, PubsubIO.writeMessages().to(topic).apply {
        maxBatchSize?.let {withMaxBatchSize(maxBatchSize)}
        idAttribute?.let { withIdAttribute(idAttribute) }
    })
}

/**
 * Write data to a BigQuery table.
 */
fun PCollection<TableRow>.toBigquery(name: String = "Write to BigQuery", table: String, dataset: String,
                                     project: String): WriteResult {
    val tableRef = TableReference().setTableId(table).setDatasetId(dataset).setProjectId(project)

    return this.apply(name, BigQueryIO.writeTableRows()
        .to(tableRef)
        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
    )
}

/**
 * Write data to a Spanner table.
 */
fun PCollection<Mutation>.toSpanner(name: String = "Write to Spanner", instanceId: String, databaseId: String,
                                    batchSizeBytes: Long  = (1024*10)) {
    this.apply(name, SpannerIO.write()
        .withInstanceId(instanceId)
        .withDatabaseId(databaseId)
        .withBatchSizeBytes(batchSizeBytes)
    )
}

/**
 * Generic DoFn context implementation
 */
open class DoFnContext<I, O>(val context: DoFn<I, O>.ProcessContext) {

    val options: PipelineOptions
        get() = context.pipelineOptions

    val element: I
        get() = context.element()

    fun output(item: O) {
        context.output(item)
    }

    fun <T> outputTagged(tag: TupleTag<T>, item: T) {
        context.output(tag, item)
    }
}

/**
 * Generic parDo implementation.
 */
inline fun <I, reified O> PCollection<I>.parDo(
    name: String = "ParDo to ${O::class.simpleName}",
    crossinline transform: DoFnContext<I,O>.() -> Unit): PCollection<O> {
    val pc = this.apply(
        ParDo.of(object: DoFn<I, O>() {
            @DoFn.ProcessElement
            fun processElement(context: ProcessContext) {
                DoFnContext(context).apply(transform)
            }
        }
        ))
    return pc.setCoder(NullableCoder.of(pc.coder))
}
