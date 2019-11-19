# Example Dataflow Pipeline
Run DataFlow pipeline with:

```shell script
PROJECT_ID=my-project-name
GCS_BUCKET=my-project-gcs-bucket-203291974
GOOGLE_APPLICATION_CREDENTIALS=CREDENTIALS_FILE.json
./gradlew run -Dpipeline="gcs2bigquery" -Dorg.gradle.java.home="/usr/lib/jvm/java-8-openjdk-amd64" --args="--runner=DataFlowRunner --project='$PROJECT_ID' --tempLocation='gs://$GCS_BUCKET/dataflow/staging' --subnetwork=https://www.googleapis.com/compute/alpha/projects/$PROJECT_ID/regions/europe-west4/subnetworks/poc-subnet --region=europe-west1 --zone=europe-west4-a --usePublicIps=false --inputGCSPath=gs://$GCS_BUCKET/data/input_csv.csv"
```

Deploy as template with:
```shell script
./gradlew run  -Dpipeline="gcs2bigquery" -Dorg.gradle.java.home="/usr/lib/jvm/java-8-openjdk-amd64" --args="--runner=DataflowRunner --project='$PROJECT_ID' --stagingLocation=gs://$GCS_BUCKET/staging --templateLocation=gs://$GCS_BUCKET/dataflow/templates/gcs_batch_process"
```