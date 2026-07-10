package diary.common.entity.xxlJob;

import lombok.Data;

/**
 * 用户推送配置实体
 */
@Data
public class UserPushConfig {
    private Long id;
    private String nickname;     // 用户昵称
    private String city;         // 所在城市
    private String pushType;     // 推送类型（wechat / email / sms）
    private String targetId;     // 推送目标ID（如 openId、邮箱地址、手机号等）
}
