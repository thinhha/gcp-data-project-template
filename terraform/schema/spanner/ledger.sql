CREATE TABLE ledger (
  id STRING(40),
  account_id STRING(40),
  amount INT64,
  insertion_timestamp TIMESTAMP,
  effective_timestamp TIMESTAMP
) PRIMARY KEY (id)