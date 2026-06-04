package com.xxl.job.executor.mapper;

import com.xxl.job.executor.entity.UserPushConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户推送配置 Mapper（静态数据，替代数据库）
 */
@Component
public class UserMapper {

    private static final List<UserPushConfig> STATIC_USERS = new ArrayList<>();

    static {
        STATIC_USERS.add(buildUser(1L, "张三", "北京", "wechat", "wx_openid_zhangsan"));
        STATIC_USERS.add(buildUser(2L, "李四", "上海", "wechat", "wx_openid_lisi"));
        STATIC_USERS.add(buildUser(3L, "王五", "广州", "email", "wangwu@example.com"));
        STATIC_USERS.add(buildUser(4L, "赵六", "深圳", "email", "zhaoliu@example.com"));
        STATIC_USERS.add(buildUser(5L, "孙七", "杭州", "sms", "13800000001"));
        STATIC_USERS.add(buildUser(6L, "周八", "成都", "wechat", "wx_openid_zhouba"));
        STATIC_USERS.add(buildUser(7L, "吴九", "北京", "sms", "13800000002"));
        STATIC_USERS.add(buildUser(8L, "郑十", "重庆", "email", "zhengshi@example.com"));
    }

    private static UserPushConfig buildUser(Long id, String nickname, String city, String pushType, String targetId) {
        UserPushConfig user = new UserPushConfig();
        user.setId(id);
        user.setNickname(nickname);
        user.setCity(city);
        user.setPushType(pushType);
        user.setTargetId(targetId);
        return user;
    }

    /**
     * 按分片查询用户列表
     *
     * @param shardTotal 总分片数
     * @param shardIndex 当前分片序号
     * @return 当前分片对应的用户列表
     */
    public List<UserPushConfig> selectByShard(int shardTotal, int shardIndex) {
        List<UserPushConfig> result = new ArrayList<>();
        for (int i = 0; i < STATIC_USERS.size(); i++) {
            if (i % shardTotal == shardIndex) {
                result.add(STATIC_USERS.get(i));
            }
        }
        return result;
    }

    /**
     * 查询全部用户
     */
    public List<UserPushConfig> selectAll() {
        return new ArrayList<>(STATIC_USERS);
    }
}
