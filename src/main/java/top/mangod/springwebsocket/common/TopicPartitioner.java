package top.mangod.springwebsocket.common;

import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import top.mangod.springwebsocket.domain.SocketMsg;

import java.util.List;

public class TopicPartitioner extends DefaultPartitioner {
    /**
     * Compute the partition for the given record.
     *
     * @param topic      The topic name
     * @param key        The key to partition on (or null if no key)
     * @param keyBytes   serialized key to partition on (or null if no key)
     * @param value      The value to partition on or null
     * @param valueBytes serialized value to partition on or null
     * @param cluster    The current cluster metadata
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (value instanceof SocketMsg) {
            SocketMsg message = (SocketMsg) value;
            String channelId = message.getRoomId();
            if (null != channelId) {
                long hashNum = Long.valueOf(channelId.substring(1));
                List<PartitionInfo> availablePartitions = cluster.availablePartitionsForTopic(topic);
                int size = availablePartitions.size();
                if (size > 0) {
                    return (int) (hashNum % size);
                }
            }
        }

        return super.partition(topic, key, keyBytes, value, valueBytes, cluster);
    }
}
