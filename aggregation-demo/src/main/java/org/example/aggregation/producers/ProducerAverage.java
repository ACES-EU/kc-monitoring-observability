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
public class ProducerAverage {

    private static final Logger log = Logger.getLogger(ProducerAverage.class.getName());

    @Inject
    @StreamProducer
    private Producer<String, String> producer;

    public void produceAverage(List<ConsumerRecord<String, String>> record) {
        //calculate the average (for now just of the offset)
        double sum = 0;
        for (ConsumerRecord<String, String> r : record) {
            sum += r.offset();
        }

        sum = sum / record.size();

        log.info("Sending aggregated result to topic metric_values_average: " + sum);

        producer.send(new ProducerRecord<>("metric_values_average", "metric_values_average", String.valueOf(sum)), (metadata, exception) -> {
            if (exception != null) {
                // Handle the send error
                exception.printStackTrace();
            } else {
                log.info("Aggregated result sent successfully to topic: " + metadata.topic());
            }
        });
    }
}
