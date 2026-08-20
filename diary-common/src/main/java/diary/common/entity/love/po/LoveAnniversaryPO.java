package diary.common.entity.love.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱纪念日持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_anniversary")
public class LoveAnniversaryPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long coupleId;
    private Long creatorUserId;
    private String name;
    private LocalDate eventDate;
    private Byte repeatType;
    private Integer remindDays;
    private Boolean pinned;
    private Integer sort;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
