package top.mangod.springwebsocket.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageStore {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ApplicationContext applicationContext;


    // 保存所有的用户session
    public static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    // 保存所有的用户和session的关系
    public static Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    // 保存所有的用户和session的关系
    public static Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    // 保存房间用户消息操作
    public static Map<String, String> msgMap = new ConcurrentHashMap<>();

    // 连接超时时间，秒
    public static final int CONNECTION_TIMEOUT = 300;

    @PostConstruct
    public void init() {
        /*if (null == redisTemplate) {
            synchronized (MessageStore.class) {
                if (null == redisTemplate) {
                    redisTemplate = applicationContext.getBean(StringRedisTemplate.class);
                }
            }
        }*/
        WebSocketServer.setMessageStore(this);
    }

    /**
     * 根据sessionId获取本地session，因为无法session无法被序列化，所以无法集中存储到redis
     *
     * @param sessionIdKey
     * @return
     */
    public static Session getLocalSession(String sessionIdKey) {
        Session session = sessionMap.get(sessionIdKey);
        return session;
    }

    /**
     * 根据userId获取本地sessionId，因为无法session无法被序列化，所以无法集中存储到redis
     *
     * @param userId
     * @return
     */
    public static String getLocalSessionByUser(String userId) {
        String sessionIdKey = userSessionMap.get(getUserIdKey(userId));
        return sessionIdKey;
    }

    /**
     * 保存session，session只有本地有用，无需存储到redis
     *
     * @param session
     */
    public void saveSession(Session session) {
//        session.getAttributes().get("userId");
        String sessionId = session.getId();
        sessionMap.put(getSessionIdKey(sessionId), session);
    }

    /**
     * 删除session
     *
     * @param session
     */
    public void deleteSession(Session session) {
        String sessionId = session.getId();
        sessionMap.remove(getSessionIdKey(sessionId));
        userSessionMap.remove(getUserIdKey(sessionId));
        sessionUserMap.remove(getSessionIdKey(sessionId));
        redisTemplate.delete(getSessionIdKey(sessionId));
        // 删除用户对应的session，要按照hash的结构存储和删除
        redisTemplate.delete(getUserIdKey(sessionId));
    }

    /**
     * 保存用户和session的关系
     *
     * @param session
     * @param userId
     */
    public void saveSessionUser(Session session, String userId) {
        userSessionMap.put(getUserIdKey(userId), getSessionIdKey(session.getId()));
        sessionUserMap.put(getSessionIdKey(session.getId()), getUserIdKey(userId));
        redisTemplate.opsForValue().set(getSessionIdKey(session.getId()), getUserIdKey(userId), CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        // 删除用户对应的session，要按照hash的结构存储和删除
        redisTemplate.opsForValue().set(getUserIdKey(userId), getSessionIdKey(session.getId()), CONNECTION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 根据key获取消息
     *
     * @param roomUserMsgTypeOperateKey
     * @return
     */
    public String getMsgByKey(String roomUserMsgTypeOperateKey) {
        String storedMsg = msgMap.get(roomUserMsgTypeOperateKey);
        if (StringUtils.isNotBlank(storedMsg)) {
            return storedMsg;
        }
        Boolean hasKey = redisTemplate.hasKey(roomUserMsgTypeOperateKey);
        if (hasKey != null && hasKey) {
            storedMsg = (String) redisTemplate.opsForValue().get(roomUserMsgTypeOperateKey);
        }
        return storedMsg;
    }

    /**
     * 保存房间用户消息操作
     *
     * @param roomUserMsgTypeOperateKey
     * @param textMessage
     */
    public void saveMsgByKey(String roomUserMsgTypeOperateKey, String textMessage) {
        msgMap.put(roomUserMsgTypeOperateKey, textMessage);
        redisTemplate.opsForValue().set(roomUserMsgTypeOperateKey, textMessage);
    }

    public static String getSessionIdKey(String sessionId) {
        return "ws-session-ehomecloud-" + sessionId;
    }

    public static String getUserIdKey(String userId) {
        return "ws-user-ehomecloud-" + userId;
    }

    public static String getUserRoomOperateKey(Integer msgType, String userId, String roomId, Integer operateType) {
        return "ws-msgtype-user-room-operate-ehomecloud-" + msgType + "-" + userId + "-" + roomId + "-" + operateType;
    }

    /**
     * 更新连接心跳时间
     *
     * @param session
     */
    public void updateConnectionHeartbeatTime(Session session, Long timestamp) {
        String sessionId = session.getId();
        String sessionKey = getSessionIdKey(sessionId);
        redisTemplate.expire(sessionKey, CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        Object userIdKey = redisTemplate.opsForValue().get(sessionKey);
        if (userIdKey != null) {
            redisTemplate.expire((String) userIdKey, CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }
    }
}
