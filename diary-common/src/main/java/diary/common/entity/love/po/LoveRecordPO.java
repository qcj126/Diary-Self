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

/** 恋爱记录持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_record")
public class LoveRecordPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long coupleId;
    private Long creatorUserId;
    private Long locationId;
    private String title;
    private String content;
    private LocalDate recordDate;
    private String categoryCode;
    private Boolean important;
    private Integer sort;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
