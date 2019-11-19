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

// Provider configuration
provider "google" {
  region      = var.region
  credentials = file("CREDENTIALS_FILE.json")
  version     = "~> 2.5"
}

// Backend configuration
terraform {
  backend "gcs" {
    bucket      = "my-gcs-bucket-for-terraform-state"
    prefix      = "tfstate"
    credentials = "CREDENTIALS_FILE.json"
  }
}

// Terraform plugin for creating random ids
resource "random_id" "instance_id" {
  byte_length = 8
}

// GCE VM
resource "google_compute_instance" "bastion" {
  name         = "bastion-${random_id.instance_id.hex}"
  description  = "Bastion Host"
  project      = var.project_name
  zone         = var.zone
  machine_type = "n1-standard-1"
  tags         = ["bastion"]

  network_interface {
    subnetwork = google_compute_subnetwork.gce.self_link

    access_config {
      // Included to generate an external IP
    }
  }

  boot_disk {
    initialize_params {
      image = "ubuntu-1904"
    }
  }

  allow_stopping_for_update = "true"

  metadata_startup_script = <<EOS

# Basic first steps
export CLOUDSDK_CORE_DISABLE_PROMPTS=1
export DEBIAN_FRONTEND=noninteractive
sudo apt-get update
sudo apt-get install -yq vim htop build-essential kafkacat;

cat<<'EOF' > /usr/local/bin/setup-server
#!/bin/bash
set -x

export CLOUDSDK_CORE_DISABLE_PROMPTS=1
export DEBIAN_FRONTEND=noninteractive

# Install Docker CE
sudo apt-get install -yq apt-transport-https ca-certificates gnupg-agent software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -yq docker-ce docker-ce-cli containerd.io docker-compose
sudo usermod -a -G docker $(whoami)

EOF

chmod +x /usr/local/bin/setup-server

EOS

}

// Outputs
output "bastion_ip" {
  value = google_compute_instance.bastion.network_interface.0.access_config.0.nat_ip
}