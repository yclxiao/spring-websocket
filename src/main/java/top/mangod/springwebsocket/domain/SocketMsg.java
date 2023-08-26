package top.mangod.springwebsocket.domain;

import lombok.Data;

/**
 * 视频通话的消息内容
 */
@Data
public class SocketMsg<T> {

    /**
     * 消息类型：1心跳  2登录 3业务操作
     */
    private Integer msgType;

    /**
     * 发送用户ID
     */
    private String sendUserId;

    /**
     * 接受用户ID
     */
    private String receivedUserId;

    /**
     * 房间号
     */
    private String roomId;

    /**
     * 业务类型
     */
    private Integer bizType;

    /**
     * 业务操作模块
     */
    private Integer bizOptModule;

    /**
     * 消息内容
     */
    private T msgBody;
}