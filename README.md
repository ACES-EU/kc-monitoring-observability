# Monitoring and Observability component

[![GPLv3 License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Test & Build](https://github.com/ACES-EU/kc-monitoring-observability/actions/workflows/test_and_build.yaml/badge.svg?event=push)](https://github.com/ACES-EU/kc-monitoring-observability/actions/workflows/test_and_build.yaml)

Monitoring and Observability is an ACES kernel component that delivers comprehensive monitoring and observability capabilities across various software stack layers, including edge, application, network, and cloud. In that, it ensures proactive issue identification and resolution, ultimately promoting smooth operations and optimal resource utilization. The ACES Monitoring and Observability component utilizes open-source tools to achieve comprehensive telemetry collection from ACES assets and instrumented applications. These assets, encompassing workloads, clusters, infrastructure elements, and applications, generate various data sources like metrics, logs, and traces. The collected data undergoes analysis for anomaly detection and alert generation. This processed information is then strategically distributed across all ACES components, facilitating real-time system-wide visibility for informed decision-making.

![architecture](docs/architecture.png)

As shown in the architecture, the component is composed of the following functionalities and subcomponents:

|Component|Functionality / Sub-Component|Functionality Description|Technologies Used|
|:----|:----|:----|:----|
|Monitoring and Observability component|Instrumentation & export|Instruments the applications and exports the metrics, logs, telemetry.|OpenTelemetry API/SDK and collector (instrumentation, exporter), KumuluzEE, KumuluzEE Metrics Extension|
| |Telemetry collector|A proxy to receive, process and export telemetry data to the monitoring backend.|OpenTelemetry collector|
| |Monitoring system|Monitoring backend, ingression of metrics, anomaly detection, alerting, analysis, and visualization.|Prometheus|
| |Forwarder|Ingests/converts monitoring and telemetry data and dispatches them to the event store/processing.|prometheus-kafka-adapter|
| |ETL/stream aggregation|Data aggregation and transformation service.|KumuluzEE, Kafka Java Client|
| |Visualization|Monitoring and observability data visualization and analysis.|Grafana|
|Event store and stream processing||Raw and aggregated event and metric/telemetry dispatch to other ACES components.|Kafka, Zookeper, Kafka UI, NATS Jetstream|


## Prerequisites

- [docker](https://docs.docker.com/get-docker/)
- [minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

## Running the application

### Running the application in dev mode

You can deploy an entire application using docker-compose. All the docker images are already built and pushed to the docker hub.

```shell script
docker-compose up -d

# stop the services
docker-compose down
```

### Deploying and running the application in minikube

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

# go to grafana web page
minikube service grafana -n ul

```

## Demonstration

For evaluation purposes, a system with three Quarkus microservices, an OpenTelemetry collector, a Prometheus and Kafka instance and KumuluzEE Java aggregation microservice with an additional Grafana dashboard is created. Prometheus is collecting data from microservices and OpenTelemetry collector and feds it into Kafka using the Prometheus Kafka Adapter. This data is then read by a KumuluzEE Java microservice, which also produces four topics that aggregate the collected data. Additionally, we have a Kafka UI for easy management of the Kafka system.

We have created 5 Kafka topics - one with raw data and 4 with aggregated data.

![kafka topics](docs/kafka-topics.png)

This is how the stream of metrcic_values_WMA looks like:

![aggregation](docs/aggregation.png)

Finally, a Grafana dashboard with alerts is set up (assets available in `/demo-resources` directory):

![dasboard](docs/dashboard.png)


### Demo components

- **Asset demo 1**: A dummy Quarkus service that produces random metrics. It's accessible on port 8082.

- **Asset demo 2**: A dummy Quarkus service that produces random metrics. It's accessible on port 8083.

- **Asset demo 3**: A dummy Quarkus service that produces random metrics. It's accessible on port 8084.

- **Prometheus**: Monitoring and alerting toolkit. It's configured with a custom configuration file and accessible on port 9090. It's set up to scrape metrics from the dummy services.

- **Kafka**: A distributed streaming platform. It's set up with a Zookeeper instance and accessible on port 9092.

- **Prometheus Kafka Adapter**: An adapter to push Prometheus metrics to Kafka. It's configured to connect to the Kafka broker and push data to the `prometheus_raw_data` topic.

- **Kafka UI**: A user interface for Kafka for easy management. It's configured to connect to the Kafka broker and Zookeeper instance and accessible on port 8081.

- **Aggregation Demo**: A demo service that produces and consumes from the Kafka broker. It's accessible on port 8080.

- **OpenTelemetry collector**: Exposes a telemetry collection endpoint that is fed into the system. Exposed on port 8888.

- **Grafana**: A visualization and dashboarding tool. It is accessible on port 3000.


## License

This project is licensed under the terms of the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

© 2024 Faculty of Computer and Information Science, University of Ljubljana