package top.mangod.springwebsocket.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import top.mangod.springwebsocket.common.KafkaConfigProperties;
import top.mangod.springwebsocket.domain.SocketMsg;
import top.mangod.springwebsocket.util.ImUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 监听来自kafka的消息
 **/
@Slf4j
@Component
public class MessageConsumer implements ApplicationListener<ApplicationReadyEvent> {
    /**
     * consumer poll 超时时间
     */
    private static final int CONSUME_POOL_TIMEOUT_MS = 100;

    private boolean running = false;

    @Resource
    private MessageSender messageSender;
    @Resource
    private KafkaConfigProperties sendingKafkaConfigProperties;

    /*@KafkaListener(topics = "${kafka.topic.socket.msg}")
    public void listeneTableInsertUpdate(ConsumerRecord<?, ?> record) {
        String key = Objects.isNull(record.key()) ? "" : (String) record.key();
        String value = (String) record.value();
        String topic = record.topic();
        log.info("接收到的信息key为：" + key);
        log.info("接收到的信息value为：" + value);

        try {
            messageSender.sendMsgToClient(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/

    /**
     * 启动消费
     */
    public synchronized void start() {
        log.info("Message broke is starting.....");
        if (running) {
            log.warn("Message broker has started, start will be ignored......");
        }
        running = true;

        IntStream.range(1, sendingKafkaConfigProperties.getPartitionNum() + 1)
                .mapToObj(no -> {
                    String threadName = "EhomeCloudImBrokerKafka_" + no;
                    return new Thread(() -> {
                        KafkaConsumer kafkaConsumer = new KafkaConsumer(sendingKafkaConfigProperties.getKafkaParams(),
                                new StringDeserializer(), new StringDeserializer());
                        kafkaConsumer.subscribe(Collections.singletonList(sendingKafkaConfigProperties.getTopic()));
                        while (running) {
                            try {
                                ConsumerRecords records = kafkaConsumer.poll(CONSUME_POOL_TIMEOUT_MS);
                                List<ConsumerRecord> recordList = IteratorUtils.toList(records.iterator());
                                recordList.stream()
                                        .map(record -> ImUtils.fromJson(record.value().toString(), SocketMsg.class))
                                        .filter(sendingMessage -> {
                                            String receiverId = sendingMessage.getReceivedUserId();
                                            return !StringUtils.isBlank(receiverId)
                                                    && MessageStore.existInLocalServer(receiverId);

                                        })
                                        .forEach(sendingMessage -> {
                                            try {
                                                messageSender.sendMsgToClient(ImUtils.toJson(sendingMessage));
                                            } catch (Exception e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                            } catch (Exception e) {
                                log.error("{} message broker subscribe has error ", threadName, e);
                            }
                        }
                    }, threadName);
                }).forEach(Thread::start);
    }

    /**
     * 停止消费
     */
    @PreDestroy
    public synchronized void stop() {
        running = false;
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        start();
    }
}
