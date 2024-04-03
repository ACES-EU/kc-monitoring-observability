# Prometheus kafka demo

In this project, we have created a system with three Quarkus microservices, a Prometheus and Kafka instance and KumuluzEE Java aggregation microservice. Prometheus is collecting data from microservices and feds it into Kafka using the Prometheus Kafka Adapter. This data is then read by a KumuluzEE Java microservice, which also produces four topics that aggregate the collected data. Additionally, we have a Kafka UI for easy management of the Kafka system.

We have created 5 Kafka topics - one with raw data and 4 with aggregated data.

![309650708-bbf48cf0-25e6-41bd-bc5f-f7ffa5ced14c](https://github.com/ACES-EU/kc-monitoring-observability/assets/79155108/78cfed30-3e0e-459a-95f2-c63266b86089)

This is how the stream of metrci_values_WMA looks like:

![309651477-56ca405f-8801-422c-bbaf-4a44f795d206](https://github.com/ACES-EU/kc-monitoring-observability/assets/79155108/c47d011b-d28c-46c1-b136-3fcd2323ed70)

## Components

- **Asset demo 1**: A dummy Quarkus service that produces random metrics. It's accessible on port 8082. It represents 'ACES asset'.

- **Asset demo 2**: A dummy Quarkus service that produces random metrics. It's accessible on port 8083. It represents 'ACES asset'.

- **Asset demo 3**: A dummy Quarkus service that produces random metrics. It's accessible on port 8084. It represents 'ACES asset'.

- **Prometheus**: Monitoring and alerting toolkit. It's configured with a custom configuration file and accessible on port 9090. It's set up to scrape metrics from the dummy services. It represents 'Monitoring system'.

- **Kafka**: A distributed streaming platform. It's set up with a Zookeeper instance and accessible on port 9092.

- **Prometheus Kafka Adapter**: An adapter to push Prometheus metrics to Kafka. It's configured to connect to the Kafka broker and push data to the `prometheus_raw_data` topic. It represents 'Forwarder'.

- **Kafka UI**: A user interface for Kafka for easy management. It's configured to connect to the Kafka broker and Zookeeper instance and accessible on port 8081.

- **Aggregation Demo**: A demo service that produces and consumes from the Kafka broker. It's accessible on port 8080. It represents 'Event store and stream processing'.

![image](https://github.com/ACES-EU/kc-monitoring-observability/assets/79155108/cbd5b3bd-3d7b-4931-8dd6-4ab5de463dce)

## Prerequisites

- [docker](https://docs.docker.com/get-docker/)
- [minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

## Development

## Running the application in dev mode

You can deploy an entire application using docker-compose. All the docker images are already built and pushed to the docker hub.

```shell script
docker-compose up -d

# stop the services
docker-compose down
```

## Running the application in minikube

You can deploy the application to minikube using the following commands:

```shell script
minikube delete --all

minikube start

#create namespace ul
kubectl create namespace ul

# move the namespace to ul
kubectl config set-context --current --namespace=ul

# deploy the kafka and zookeeper
kubectl create -f "https://strimzi.io/install/latest?namespace=ul" -n ul
kubectl apply -f https://strimzi.io/examples/latest/kafka/kafka-persistent-single.yaml -n ul

# wait for the kafka to be ready
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n ul

# deploy the rest of the services
kubectl apply -f deployment/k8s -n ul

# wait for the services to be ready
kubectl wait pod --for=condition=Ready --all --timeout=300s -n ul

# go to kafka-ui web page
minikube service kafka-ui -n ul

````
