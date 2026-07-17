package diary.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 离线消息实体
 * 对应数据库表：notify_offline_message
 *
 * 表结构说明：
 *   - id：主键，自增
 *   - user_id：目标用户ID
 *   - notify_type：通知类型（对应 NotifyTypeEnum.code）
 *   - content：通知内容
 *   - extra：扩展数据（JSON 字符串）
 *   - timestamp：消息时间戳（毫秒）
 *   - status：推送状态（0=未推送，1=已推送）
 *   - create_time：创建时间
 *   - update_time：更新时间
 *
 * 使用场景：
 *   当用户不在线时，将通知消息存入此表
 *   用户上线后查询并补推离线消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notify_offline_message")
@Schema(description = "离线消息实体，对应 notify_offline_message 表")
public class OfflineMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     * 自增策略，由数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Long id;

    /**
     * 目标用户ID
     * 消息的接收者
     */
    @Schema(description = "目标用户ID", example = "1001")
    private Long userId;

    /**
     * 通知类型
     * 对应 NotifyTypeEnum.code，如 GOAL_DUE、DIET_REMIND 等
     */
    @Schema(description = "通知类型", example = "GOAL_DUE")
    private String notifyType;

    /**
     * 通知内容
     * 推送给前端展示的文本
     */
    @Schema(description = "通知内容", example = "阶段目标即将到期！")
    private String content;

    /**
     * 扩展数据
     * JSON 字符串格式，存储业务相关的附加信息
     * 例如：{"goalId": 123, "userId": 456}
     */
    @Schema(description = "扩展数据（JSON字符串）", example = "{\"goalId\": 123}")
    private String extra;

    /**
     * 消息时间戳
     * 消息创建时的毫秒级时间戳
     */
    @Schema(description = "消息时间戳（毫秒）", example = "1721136000000")
    private Long timestamp;

    /**
     * 推送状态
     * 0 = 未推送（离线消息）
     * 1 = 已推送（已补推给用户）
     */
    @Schema(description = "推送状态：0=未推送，1=已推送", example = "0")
    private Integer status;

    /**
     * 创建时间
     * 记录入库时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录状态变更时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
