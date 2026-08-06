package diary.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    // 添加失败291，删除失败290，修改失败292，查询失败293
    private int code;
    private String message;
    private T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static ApiResponse<String> delFail() {
        return new ApiResponse<>(290, "删除失败", null);
    }

    public static ApiResponse<String> addFail() {
        return new ApiResponse<>(291, "添加失败", null);
    }

    public static ApiResponse<String> updateFail() {
        return new ApiResponse<>(292, "修改失败", null);
    }

    public static <T> ApiResponse<T> queryFail() {
        return new ApiResponse<>(293, "查询失败", null);
    }

    public static ApiResponse<Map<String, Object>> success(Map<String, Object> data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static ApiResponse<String> success(String info) {
        return new ApiResponse<>(200, "success", info);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
}
