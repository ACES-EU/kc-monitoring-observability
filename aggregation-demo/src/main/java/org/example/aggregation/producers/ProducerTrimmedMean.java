package org.example.aggregation.producers;

import com.kumuluz.ee.streaming.common.annotations.StreamProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ProducerTrimmedMean {

    private static final Logger log = Logger.getLogger(ProducerTrimmedMean.class.getName());

    @Inject
    @StreamProducer
    private Producer<String, String> producer;

    public void produceTrimmedMean(List<ConsumerRecord<String, String>> records) {
        double trimPercentage = 10; // The percentage of values to trim from each end

        List<Double> values = new ArrayList<>();
        for (ConsumerRecord<String, String> r : records) {
            // calculate the trimmed mean for the r.offset() which is a long
            values.add((double) r.offset());
        }

        Collections.sort(values);

        int trimCount = (int) (values.size() * trimPercentage / 100); // Calculating the count to trim from each end
        List<Double> trimmedValues = values.subList(trimCount, values.size() - trimCount); // Trimming

        double sum = 0;
        for (Double value : trimmedValues) {
            sum += value;
        }

        double trimmedMean = sum / trimmedValues.size(); // Calculating the trimmed mean

        log.info("Sending aggregated Trimmed Mean result to topic metric_values_trimmed_mean: " + trimmedMean);

        producer.send(new ProducerRecord<>("metric_values_trimmed_mean", "TrimmedMean", String.valueOf(trimmedMean)), (metadata, exception) -> {
            if (exception != null) {
                // Handle the send error
                exception.printStackTrace();
            } else {
                log.info("Aggregated Trimmed Mean result sent successfully to topic: " + metadata.topic());
            }
        });
    }
}
