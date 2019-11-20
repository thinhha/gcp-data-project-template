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
 
resource "google_pubsub_topic" "my_pubsub_topic" {
  name    = "my-pubsub-topic"
  project = var.project_name

  message_storage_policy {
    allowed_persistence_regions = [
      "europe-west4",
    ]
  }
}

resource "google_pubsub_subscription" "my_pubsub_subscription" {
  name  = "my-pubsub-subscription"
  topic = google_pubsub_topic.my_pubsub_topic.name
  project = var.project_name

  message_retention_duration = "3600s"
  retain_acked_messages = true
  ack_deadline_seconds = 60
}

resource "google_storage_bucket" "gcs" {
  name     = "kthxbayes-com-gcs-terraform"
  location = "EU"
  project  = var.project_name
}

resource "google_bigquery_dataset" "dataset" {
  dataset_id    = "dataset_name"
  friendly_name = "dataset name"
  description   = "Raw data ingested directly from source systems"
  location      = "EU"
  project       = var.project_name
}

resource "google_bigquery_table" "table" {
  dataset_id = "${google_bigquery_dataset.dataset.dataset_id}"
  table_id   = "table_name"
  friendly_name = "Raw data table (may contain duplicates)"
  project    = var.project_name
  schema = file("schema/test_data.json")
}
