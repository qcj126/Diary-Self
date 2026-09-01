package diary.utils.commonutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static long lastTimestamp = -1L;
    private static long sequence = 0L;
    private static final long START_TIMESTAMP = 1704038400000L; // 2024-01-01
    public static synchronized long getPrimaryKey() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨了");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095; // 12位序列号，最大值4095
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << 22)  // 时间戳左移22位
                | (sequence);                          // 序列号
    }

    // 判断字符串为空或空串
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // 判断文件为空
    public static boolean isFileEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    // 序列化为json
    public static String writeJson(Object value, String message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    // 添加这个静态方法，调用更简洁
    public static Checker check() {
        return new Checker();
    }

    // 判断入参是否为空
    public static class Checker {
        public Checker notEmpty(String value, String fieldName) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + " 不能为空字符串");
            }
            return this;
        }

        public Checker listNotContainsEmpty(List<?> value, String fieldName) {
            if (value.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(fieldName + " 不能包含空值");
            }
            return this;
        }

        public Checker stringKeyMapNotContainsEmpty(Map<String, String> map, String fieldName) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                    throw new IllegalArgumentException(fieldName + " 不能包含空值");
                }
            }
            return this;
        }

        public Checker longKeyMapNotContainsEmpty(Map<Long, String> map, String fieldName) {
            for (Map.Entry<Long, String> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                    throw new IllegalArgumentException(fieldName + " 不能包含空值");
                }
            }
            return this;
        }

        public Checker notNull(Object value, String fieldName) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + " 不能为 null");
            }
            return this;
        }
        public Checker listNotEmpty(List<?> value, String fieldName) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException(fieldName + " 不能为空");
            }
            return this;
        }
    }
}