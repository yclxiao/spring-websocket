package top.mangod.springwebsocket.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Encoder;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public final class ImUtils {
    private static final String LOCALHOST = "127.0.0.1";

    private static final String ANY_HOST = "0.0.0.0";

    private static final String LOCAL_IP = calculateLocalIP();

    private static final String HOSTNAME = calculateHostName();

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String WX_ACTOR_PREFIX = "wx-";

    private static final BASE64Encoder ENCODER = new BASE64Encoder();

    static {
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private ImUtils() {
    }

    /**
     * 获取本地IP
     *
     * @return 本地IP
     */
    public static String getLocalIp() {
        return LOCAL_IP;
    }

    /**
     * 获取本机hostname
     *
     * @return 获取本机hostname
     */
    public static String getHostname() {
        return HOSTNAME;
    }

    /**
     * 将Java对象转化为JSON字符串
     *
     * @param obj obj对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (null == obj) {
            return "";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Fail to convert object to json string {}", obj, e);
        }

        return "";
    }


    /**
     * 将Json字符串转化为Java对象
     *
     * @param jsonStr json字符串
     * @param tClass  Java对象类
     * @param <T>     泛型
     * @return Java对象
     */
    public static <T> T fromJson(String jsonStr, Class<T> tClass) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, tClass);
        } catch (Exception e) {
            log.error("Fail to convert json string={} to object={}", jsonStr, tClass, e);
        }

        return null;
    }

    /**
     * 返回Long数组最大值
     *
     * @param array 数组
     * @return 最大值
     */
    public static long max(Long[] array) {
        return Stream.of(array).mapToLong(Long::longValue).max().orElse(-1);
    }

    /**
     * 拷贝属性
     *
     * @param dest   目的对象
     * @param source 源对象
     */
    public static void copyProperties(Object dest, Object source) {
        try {
            BeanUtils.copyProperties(dest, source);
        } catch (Exception e) {
            log.info("CopyProperties has error source={} , dest={}", source, dest, e);
        }
    }

    /**
     * 根据ticket id 获取客户微信actorID
     *
     * @param ticketId ticket ID
     * @return 微信Actor ID
     */
    public static String getWxActorId(String ticketId) {
        return WX_ACTOR_PREFIX + ticketId;
    }

    /**
     * 是否是微信用户Actor
     *
     * @param actorId actor ID
     * @return boolean
     */
    public static boolean isWxActor(String actorId) {
        return StringUtils.isNotBlank(actorId) && actorId.startsWith(WX_ACTOR_PREFIX);
    }

    /**
     * 用Base64对字符串进行编码
     *
     * @param str 待编码的字符串
     * @return 编码后的字符串
     */
    public static String encodeBase64(String str) {
        if (null == str) {
            return null;
        }

        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return ENCODER.encode(bytes);
    }

    /**
     * 获取本地IP地址
     *
     * @return 本地IP地址
     */
    private static String calculateLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface network = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress address = addresses.nextElement();
                            if (isValidAddress(address)) {
                                return address.getHostAddress();
                            }
                        } catch (Throwable e) {
                        }
                    }
                }
            }
        } catch (Throwable e) {
            return LOCALHOST;
        }
        return LOCALHOST;
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return (name != null
                && !ANY_HOST.equals(name)
                && !LOCALHOST.equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    /**
     * 获取机器hostname
     *
     * @return hostname hostname
     */
    private static String calculateHostName() {
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException ex) {
            log.error("Fail to get host name ", ex);
            return LOCAL_IP;
        }
    }
}
