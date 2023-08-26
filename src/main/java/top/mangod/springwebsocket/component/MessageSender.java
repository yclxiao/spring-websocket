package top.mangod.springwebsocket.component;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.mangod.springwebsocket.domain.SocketMsg;

import javax.annotation.PostConstruct;
import javax.websocket.Session;

@Service
public class MessageSender {

    @Autowired
    private KafkaProducer kafkaProducer;
    @Value("${kafka.topic.socket.msg}")
    private String socketMsgTopic;
    @Autowired
    private MessageStore msgStore;

    @PostConstruct
    public void init() {
        WebSocketServer.setMessageSender(this);
    }

    /**
     * 发送消息要考虑可能session不在本机上的情况，就算redis获取session也没用，session无法被序列化。
     * 所以只能发送给MQ，然后多台机器消费，根据用户ID找到对应的session，再发送消息
     *
     * @param socketMsgStr
     */
    public void sendToMqBroker(String socketMsgStr) {
        kafkaProducer.sendMessage(socketMsgTopic, socketMsgStr);
    }

    public void sendMsgToClient(String socketMsgStr) throws Exception {
        SocketMsg socketMsg = JSON.parseObject(socketMsgStr, SocketMsg.class);
        // 根据消息里的用户ID 寻找对应的 session，然后再发送消息
        String sessionIdKey = MessageStore.getLocalSessionByUser(socketMsg.getReceivedUserId());
        if (StringUtils.isNotBlank(sessionIdKey)) {
            Session receivedSession = MessageStore.getLocalSession(sessionIdKey);
            if (receivedSession != null) {
                if (!receivedSession.isOpen()) {
                    msgStore.deleteSession(receivedSession);
                } else {
                    receivedSession.getBasicRemote().sendText(JSON.toJSONString(socketMsg));
                }
            }
        }
    }

    public void sendHeartBeatMsgToClient(Session session, String msg) throws Exception {
        if (session != null) {
            if (!session.isOpen()) {
                msgStore.deleteSession(session);
            } else {
                session.getBasicRemote().sendText(msg);
            }
        }
    }
}
