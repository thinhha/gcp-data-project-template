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

// Network configuration
resource "google_compute_network" "vpc" {
  project                 = var.project_name
  name                    = "my-vpc-network"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "gce" {
  project                  = var.project_name
  name                     = "my-vpc-subnet"
  ip_cidr_range            = "10.10.0.0/22"
  region                   = var.region
  network                  = google_compute_network.vpc.self_link
  private_ip_google_access = true
}

// GCE External Firewall
resource "google_compute_firewall" "external" {
  name    = "gce-external-fw"
  network = google_compute_network.vpc.self_link
  project = var.project_name

  target_tags = [
    "bastion",
  ]

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports = [
      "22" // SSH
    ]
  }
}

// GCE Firewall for Dataflow
resource "google_compute_firewall" "dataflow_vms" {
  name    = "dataflow-vms"
  network = google_compute_network.vpc.self_link
  project = var.project_name

  source_tags = ["dataflow"]
  target_tags = ["dataflow"]
  priority    = 0

  allow {
    protocol = "tcp"
    ports = [
      "12345-12346"
    ]
  }
}

// GCE Firewall for Dataflow
resource "google_compute_firewall" "dataflow_access_all" {
  name    = "dataflow-access-all"
  network = google_compute_network.vpc.self_link
  project = var.project_name

  source_tags = ["dataflow"]
  priority    = 0

  allow {
    protocol = "tcp"
  }
}