#!/bin/bash

NATS_SERVER_URL="nats://nats-server:4222"  # Use the service name to connect
SUBJECTS="prometheus.cpu prometheus.memory prometheus.disk"

# Function to publish random data to NATS
publish_random_data() {
  local subject=$1
  local data=$((RANDOM % 100))
  echo "Publishing $data to $subject"
  nats pub $subject $data -s $NATS_SERVER_URL
}

while [ true ]; do
    for subject in $SUBJECTS; do
      publish_random_data $subject
    done

    sleep 3
done
