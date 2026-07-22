# Kafka Demo Project

This project demonstrates core Kafka capabilities including Producers, Consumers, and advanced configurations.

## Features Implemented

1.  **Basic Producer**: Asynchronous sending with callbacks and keys for partitioning.
2.  **Basic Consumer**: Graceful shutdown handling using `WakeupException` and shutdown hooks.
3.  **Advanced Producer**:
    *   **Idempotence**: Ensuring exactly-once delivery semantics (Kafka 3.0+ default).
    *   **High Throughput**: Using compression (Snappy), batching, and `linger.ms`.
    *   **Reliability**: `acks=all` and retries configuration.
4.  **Infrastructure**: `docker-compose.yml` to spin up a local Kafka broker and Zookeeper.

## How to Run

1.  **Start Kafka**: `docker-compose up -d`
2.  **Build**: `mvn clean install`
3.  **Produce**: Run `KafkaProducerDemo` or `KafkaProducerAdvanced`.
4.  **Consume**: Run `KafkaConsumerDemo`.
