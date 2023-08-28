package top.mangod.springwebsocket.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import top.mangod.springwebsocket.common.KafkaConfigProperties;

/**
 * WebSocket配置
 */
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    @ConfigurationProperties(prefix = "im.message.kafka.socket")
    public KafkaConfigProperties sendingKafkaConfigProperties() {
        return new KafkaConfigProperties();
    }

}
