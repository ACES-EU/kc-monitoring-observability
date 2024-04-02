package org.example.aggregation.producers;

import com.kumuluz.ee.streaming.common.annotations.StreamProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ProducerWMA {

    private static final Logger log = Logger.getLogger(ProducerWMA.class.getName());

    @Inject
    @StreamProducer
    private Producer<String, String> producer;

    public void produceWMA(List<ConsumerRecord<String, String>> records) {
        double weightedSum = 0;
        double totalWeight = 0;
        double weight = 1; // Starting weight, incrementing for each record

        for (ConsumerRecord<String, String> r : records) {
            // calculate the weighted moving average (WMA) of the offset
            double value = r.offset();
            weightedSum += value * weight;
            totalWeight += weight;
            weight++; // Increment weight for the next record
        }

        double wma = weightedSum / totalWeight;

        log.info("Sending aggregated WMA result to topic metric_values_WMA: " + wma);

        producer.send(new ProducerRecord<>("metric_values_WMA", "WMA", String.valueOf(wma)), (metadata, exception) -> {
            if (exception != null) {
                // Handle the send error
                exception.printStackTrace();
            } else {
                log.info("Aggregated WMA result sent successfully to topic: " + metadata.topic());
            }
        });
    }
}
