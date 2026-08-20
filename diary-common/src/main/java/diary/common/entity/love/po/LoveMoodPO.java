package diary.common.entity.love.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录心情字典持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_mood")
public class LoveMoodPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String moodCode;
    private String moodName;
    private String emoji;
    private Integer sort;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
