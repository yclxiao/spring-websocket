package top.mangod.springwebsocket.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import top.mangod.springwebsocket.constant.SocketConstants;
import top.mangod.springwebsocket.domain.MsgBodyTrtc;
import top.mangod.springwebsocket.domain.SocketMsg;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Objects;

@ServerEndpoint("/trtc/websocket/{userId}")
@Component
@Slf4j
public class WebSocketServer {
    private static MessageStore messageStore;
    private static MessageSender messageSender;

    public static void setMessageStore(MessageStore messageStore) {
        WebSocketServer.messageStore = messageStore;
    }

    public static void setMessageSender(MessageSender messageSender) {
        WebSocketServer.messageSender = messageSender;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        messageStore.saveSession(session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        messageStore.deleteSession(session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @ Param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        log.warn("=========== 收到来自窗口" + session.getId() + "的信息:" + message);
        handleTextMessage(session, new TextMessage(message));
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, @PathParam("userId") String userId, Throwable error) {
        log.error("=========== 发生错误");
        error.printStackTrace();
//        msgStore.deleteSession(session);
    }

    public void handleTextMessage(Session session, TextMessage message) throws Exception {
        log.warn("=========== Received message: {}", message.getPayload());
        // 更新心跳和过期时间
        messageStore.updateConnectionHeartbeatTime(session, System.currentTimeMillis());
        if (StringUtils.isBlank(message.getPayload())) {
            return;
        }
        if ("ping".equals(message.getPayload())) {
            messageSender.sendHeartBeatMsgToClient(session, "pong");
            return;
        }

        SocketMsg socketMsg = JSON.parseObject(message.getPayload(), SocketMsg.class);
        if (socketMsg.getMsgType() == SocketConstants.MsgType.HEART_BEAT) {
            // 心跳消息
//            sendMsgToClient(session, "pong");

        } else if (socketMsg.getMsgType() == SocketConstants.MsgType.LOGIN) {
            // session里要包含用户信息，用户信息要与session进行绑定，链接建立完，立马调用登录接口，将用户信息和对应session存储起来
            if (StringUtils.isBlank(socketMsg.getSendUserId())) {
                throw new Exception("用户ID不能为空");
            }
            if (socketMsg.getBizOptModule() == SocketConstants.BizOptModule.TRTC) {
                SocketMsg<MsgBodyTrtc> msgBody = JSON.parseObject(message.getPayload(), new TypeReference<SocketMsg<MsgBodyTrtc>>() {
                });
                if (StringUtils.isBlank(msgBody.getRoomId())) {
                    throw new Exception("房间号不能为空");
                }
                messageStore.saveSessionUser(session, msgBody.getSendUserId());
            }

        } else if (socketMsg.getMsgType() == SocketConstants.MsgType.BIZ_OPERATE) {
            if (StringUtils.isBlank(socketMsg.getSendUserId())) {
                throw new Exception("用户ID不能为空");
            }
            if (socketMsg.getBizOptModule() == SocketConstants.BizOptModule.TRTC) {
                handleTrtcMsg(message);
            }

        } else {
            log.error("消息类型错误");
        }
    }

    /**
     * 处理视频面试消息
     *
     * @param message
     * @throws Exception
     */
    private void handleTrtcMsg(TextMessage message) throws Exception {
        SocketMsg<MsgBodyTrtc> msgBody = JSON.parseObject(message.getPayload(), new TypeReference<SocketMsg<MsgBodyTrtc>>() {
        });
        if (StringUtils.isBlank(msgBody.getRoomId())) {
            throw new Exception("房间号不能为空");
        }
        MsgBodyTrtc msgBodyTrtc = msgBody.getMsgBody();

        if (null == msgBodyTrtc.getOperateType()) {
            throw new Exception("操作类型不能为空");
        }
        if (StringUtils.isBlank(msgBodyTrtc.getOperateContent())) {
            throw new Exception("操作内容不能为空");
        }

        String roomUserMsgTypeOperateKey = MessageStore.getUserRoomOperateKey(SocketConstants.MsgType.BIZ_OPERATE, msgBody.getSendUserId(), msgBody.getRoomId(), SocketConstants.OperateType.MICRO_PHONE);
        String storedMsg = messageStore.getMsgByKey(roomUserMsgTypeOperateKey);
        SocketMsg<MsgBodyTrtc> storedMsgBody = null;
        if (StringUtils.isNotBlank(storedMsg)) {
            storedMsgBody = JSON.parseObject(storedMsg, new TypeReference<SocketMsg<MsgBodyTrtc>>() {
            });
        }

        if (msgBodyTrtc.getOperateType() == SocketConstants.OperateType.MICRO_PHONE) {
            SocketMsg<MsgBodyTrtc> sendSocketMsg = new SocketMsg<>();
            MsgBodyTrtc targetMsgBodyTrtc = new MsgBodyTrtc();
            sendSocketMsg.setMsgType(SocketConstants.MsgType.BIZ_OPERATE);
            sendSocketMsg.setSendUserId(msgBody.getSendUserId());
            sendSocketMsg.setReceivedUserId(msgBody.getReceivedUserId());
            sendSocketMsg.setBizType(msgBody.getBizType());
            sendSocketMsg.setMsgBody(targetMsgBodyTrtc);
            sendSocketMsg.setBizOptModule(msgBody.getBizOptModule());
            sendSocketMsg.setRoomId(msgBody.getRoomId());

            targetMsgBodyTrtc.setOperateType(SocketConstants.OperateType.MICRO_PHONE);
            targetMsgBodyTrtc.setOperateContent(String.valueOf(
                    (storedMsgBody == null || Objects.equals(String.valueOf("1"),
                            storedMsgBody.getMsgBody().getOperateContent())) ?
                            "0" : "1"));

            String msgStr = JSON.toJSONString(sendSocketMsg);
            messageStore.saveMsgByKey(roomUserMsgTypeOperateKey, msgStr);

            // 发送到MQ，再有各个负载消费，找到对应的session，发送消息
            messageSender.sendToMqBroker(msgStr);
        }
    }


}
