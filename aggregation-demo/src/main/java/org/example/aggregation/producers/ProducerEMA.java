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
public class ProducerEMA {
    private static final Logger log = Logger.getLogger(ProducerEMA.class.getName());

    @Inject
    @StreamProducer
    private Producer<String, String> producer;

    public void produceEMA(List<ConsumerRecord<String, String>> record) {
        //calculate the exponential moving average (EMA) of the offset
        double sum = record.get(0).offset();
        double alpha = 0.1;
        for (ConsumerRecord<String, String> r : record) {
            sum = alpha * r.offset() + (1 - alpha) * sum;
        }

        log.info("Sending aggregated result to topic metric_values_EMA: " + sum);

        producer.send(new ProducerRecord<>("metric_values_EMA", "EMA", String.valueOf(sum)), (metadata, exception) -> {
            if (exception != null) {
                // Handle the send error
                exception.printStackTrace();
            } else {
                log.info("Aggregated result sent successfully to topic: " + metadata.topic());
            }
        });
    }
}
