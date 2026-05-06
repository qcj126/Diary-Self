package diary.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static ApiResponse<Map<String, Object>> success(Map<String, Object> data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static ApiResponse<String> success(String info) {
        return new ApiResponse<>(200, "success", info);
    }
}