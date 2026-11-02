package diary.diaryai.metrics;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据库按状态聚合后的数量行，供 MyBatis 映射并写入监控 Gauge。
 */
@Getter
@Setter
public class StatusCount {
    /** AI 任务或 Outbox 消息的固定状态枚举值。 */
    private String status;

    /** 该状态在当前数据库快照中的记录数。 */
    private long total;
}
