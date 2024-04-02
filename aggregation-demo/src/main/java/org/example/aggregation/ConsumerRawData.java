package org.example.aggregation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.kumuluz.ee.streaming.common.annotations.StreamListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.aggregation.producers.ProducerAverage;
import org.example.aggregation.producers.ProducerEMA;
import org.example.aggregation.producers.ProducerTrimmedMean;
import org.example.aggregation.producers.ProducerWMA;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ConsumerRawData {

    private static final Logger log = Logger.getLogger(ConsumerRawData.class.getName());
    private List<String> messages = new ArrayList<>();

    @Inject
    private ProducerAverage producerAverage;

    @Inject
    private ProducerEMA producerEMA;

    @Inject
    private ProducerWMA producerWMA;

    @Inject
    private ProducerTrimmedMean producerTrimmedMean;

    @StreamListener(topics = {"prometheus_raw_data"}, batchListener = true)
    public void onMessage(List<ConsumerRecord<String, String>> record) {

        log.info("Consumed batch of messages");

        //add all messages to the list
        record.forEach(r -> messages.add(r.value()));

        if (record.isEmpty()) {
            log.info("No records to aggregate");
            return;
        }
        //send the average to the topic
        producerAverage.produceAverage(record);

        //send the EMA to the topic
        producerEMA.produceEMA(record);

        //send the WMA to the topic
        producerWMA.produceWMA(record);

        //send the Trimmed Mean to the topic
        producerTrimmedMean.produceTrimmedMean(record);
    }

    @StreamListener(topics = {"test-kumuluzee"})
    public void onMessageTest(ConsumerRecord<String, String> record) {

        log.info(String.format("Consumed message: offset = %d, key = %s, value = %s%n", record.offset(), record.key()
                , record.value()));

        messages.add(record.value());
    }

    public List<String> getLastFiveMessages() {
        if (messages.size() < 5)
            return messages;
        return messages.subList(messages.size() - 5, messages.size());
    }
}
