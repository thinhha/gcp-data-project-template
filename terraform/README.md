# Sample Data Project Creation Terraform

This directory contains GCP resource definitions in Terraform that you can use to bootstrap your data project.

Before executing `terraform apply`, you will need to change the following variables:
- In `variables.tf `, change the default `project_name` to your project name.
- In `main.tf`, change the gcs state backend for terraform from `my-gcs-bucket-for-terraform-state` to the name of a GCS bucket accessible by your Service Account.
- In `data.tf`,  change the `google_storage_bucket` bucket name to a new globally unique name.

Please see the [Getting Started with GCP](https://learn.hashicorp.com/terraform/gcp/intro) guide for instructions to 1. [install Terraform](https://learn.hashicorp.com/terraform/gcp/install) and 2. [sett up the prerequisite](https://learn.hashicorp.com/terraform/gcp/build) GCP project and Service Account Key.
