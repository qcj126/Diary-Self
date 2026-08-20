package diary.common.entity.love.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录心情关联持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_record_mood")
public class LoveRecordMoodPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long recordId;
    private Long moodId;
    private Integer sort;
    private LocalDateTime createTime;
}
