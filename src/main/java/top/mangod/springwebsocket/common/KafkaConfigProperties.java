package top.mangod.springwebsocket.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import top.mangod.springwebsocket.util.ImUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Data
@Slf4j
public class KafkaConfigProperties implements Serializable {

    private Properties properties = new Properties();

    private String name;

    private String topic;

    private int partitionNum = 12;

    private int pollTime = 100;

    /**
     * 获取kafka配置参数
     *
     * @return kafka配置参数
     */
    public Map<String, Object> getKafkaParams() {
        return new HashMap<String, Object>() {
            {
                put("bootstrap.servers", properties.getProperty("bootstrap.servers"));

                String groupId = properties.getProperty("group.id");
                if (StringUtils.isNoneBlank(groupId)) {
                    groupId = groupId.replace("{hostName}", ImUtils.getHostname());
                    put("group.id", groupId);
                }
                if (StringUtils.isNumeric(properties.getProperty("heartbeat.interval.ms"))) {
                    put("heartbeat.interval.ms", NumberUtils.toInt(properties.getProperty("heartbeat.interval.ms")));
                } else {
                    put("heartbeat.interval.ms", 5000);
                }

                String keyDeserializerString = properties.getProperty("key.deserializer");
                if (StringUtils.isNoneBlank(keyDeserializerString)) {
                    try {
                        put("key.deserializer", Class.forName(keyDeserializerString));
                    } catch (Exception e) {
                        log.error("create key.deserializer fail {}", keyDeserializerString, e);
                    }
                }

                String valueDeserializerString = properties.getProperty("value.deserializer");
                if (StringUtils.isNoneBlank(valueDeserializerString)) {
                    try {
                        put("value.deserializer", Class.forName(valueDeserializerString));
                    } catch (Exception e) {
                        log.error("create value.deserializer fail {}", valueDeserializerString, e);
                    }
                }

                String clientIdValue = properties.getProperty("client.id");
                if (StringUtils.isNotBlank(clientIdValue)) {
                    put("client.id", clientIdValue);
                }

                put("partitioner.class", "top.mangod.springwebsocket.common.TopicPartitioner");
            }
        };
    }
}
