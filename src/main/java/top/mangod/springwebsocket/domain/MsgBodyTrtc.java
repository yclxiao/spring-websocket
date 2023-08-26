package top.mangod.springwebsocket.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频通话的消息内容
 */
@Data
public class MsgBodyTrtc implements Serializable {
    /**
     * 操作类型：1麦克风操作
     */
    private Integer operateType;
    /**
     * 操作内容：1打开，2关闭
     */
    private String operateContent;
}