CREATE TABLE transactions (
  id STRING(40),
  asset STRING(1000),
  amount INT64,
  insertion_timestamp TIMESTAMP,
  effective_timestamp TIMESTAMP,
) PRIMARY KEY (id)