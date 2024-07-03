package org.example.aggregation.producers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.streaming.common.annotations.StreamProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class ProducerAverage {

    private static final Logger log = Logger.getLogger(ProducerAverage.class.getName());

    @Inject
    @StreamProducer
    private Producer<String, String> producer;

    public void produceAverage(List<ConsumerRecord<String, String>> record) {
        //calculate the average (for now just of the offset)

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Double> valueSums = new HashMap<>();
        Map<String, Integer> valueCounts = new HashMap<>();

        double sum = 0;
        for (ConsumerRecord<String, String> r : record) {
            String jsonString = r.value();
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                String metricNameKey = "name";
                String metricValueKey = "value";
                if (jsonNode.has(metricNameKey) && jsonNode.has(metricValueKey)) {
                    double value = jsonNode.get(metricValueKey).asDouble();
                    String metricName = jsonNode.get(metricNameKey).asText();
                    // Aggregate sums and counts
                    valueSums.put(metricName, valueSums.getOrDefault(metricName, 0.0) + value);
                    valueCounts.put(metricName, valueCounts.getOrDefault(metricName, 0) + 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String key : valueSums.keySet()) {
            System.out.println("Property: " + key + ", Sum: " + valueSums.get(key) + ", Count: " + valueCounts.get(key));
            sum = valueSums.get(key) / valueCounts.get(key);

            log.info("Sending aggregated result to topic metric_values_average: " + key + " " + sum);

            producer.send(new ProducerRecord<>("metric_values_average", key, String.valueOf(sum)), (metadata, exception) -> {
                if (exception != null) {
                    // Handle the send error
                    exception.printStackTrace();
                } else {
                    log.info("Aggregated result sent successfully to topic: " + metadata.topic());
                }
            });
        }

        
    }
}
