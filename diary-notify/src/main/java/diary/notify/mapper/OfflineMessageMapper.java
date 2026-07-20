package diary.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import diary.notify.entity.OfflineMessage;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 离线消息 Mapper
 * 负责离线消息的数据库 CRUD 操作
 *
 * 核心职责：
 *   1. 插入离线消息记录
 *   2. 查询用户未推送的离线消息
 *   3. 更新消息推送状态
 *   4. 批量删除已推送的消息（可选）
 *
 * 继承 BaseMapper：
 *   - MyBatis-Plus 提供的通用 Mapper 接口
 *   - 自动实现单表的 CRUD 操作
 *   - 无需编写 SQL，通过方法名自动生成
 *
 * 对应表结构：
 *   notify_offline_message 表
 *   - id：主键
 *   - user_id：用户ID
 *   - notify_type：通知类型
 *   - content：通知内容
 *   - timestamp：时间戳
 *   - extra：扩展数据（JSON）
 *   - status：推送状态（0=未推送，1=已推送）
 *   - create_time：创建时间
 *   - update_time：更新时间
 */
@Mapper
public interface OfflineMessageMapper extends BaseMapper<OfflineMessage> {

    /**
     * 查询用户未推送的离线消息
     * 按时间戳升序排序（先推送旧消息）
     *
     * @param userId 用户ID
     * @return 未推送的离线消息列表
     */
    // TODO: 第一步：使用 MyBatis-Plus 的查询方法
    //   - 方式1：使用 selectList + QueryWrapper
    //     return selectList(new QueryWrapper<OfflineMessage>()
    //         .eq("user_id", userId)
    //         .eq("status", 0)
    //         .orderByAsc("timestamp"));
    //   - 方式2：自定义 SQL 方法（在 XML 中编写）
    List<OfflineMessage> selectUnreadMessages(@Param("userId") Long userId);

    /**
     * 批量更新消息推送状态
     *
     * @param messageIds 消息ID列表
     * @param status     推送状态（0=未推送，1=已推送）
     * @return 更新的记录数
     */
    // TODO: 第一步：使用 MyBatis-Plus 的批量更新方法
    //   - 方式1：使用 update + UpdateWrapper
    //     return update(new UpdateWrapper<OfflineMessage>()
    //         .in("id", messageIds)
    //         .set("status", status));
    //   - 方式2：自定义 SQL 方法（在 XML 中编写）
    int batchUpdateStatus(@Param("messageIds") List<Long> messageIds, @Param("status") Integer status);

    /**
     * 删除用户已推送的离线消息
     * 可选操作，用于清理历史数据
     *
     * @param userId 用户ID
     * @return 删除的记录数
     */
    // TODO: 第一步：使用 MyBatis-Plus 的删除方法
    //   - 方式1：使用 delete + QueryWrapper
    //     return delete(new QueryWrapper<OfflineMessage>()
    //         .eq("user_id", userId)
    //         .eq("status", 1));
    //   - 方式2：自定义 SQL 方法（在 XML 中编写）
    int deleteDeliveredMessages(@Param("userId") Long userId);
}
