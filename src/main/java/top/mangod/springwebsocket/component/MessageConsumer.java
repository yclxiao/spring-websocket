package top.mangod.springwebsocket.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 监听来自dataocean的rocketmq消息
 **/

@Slf4j
@Component
public class MessageConsumer {

    @Autowired
    private MessageSender messageSender;

    @KafkaListener(topics = "${kafka.topic.socket.msg}")
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
    }
}
