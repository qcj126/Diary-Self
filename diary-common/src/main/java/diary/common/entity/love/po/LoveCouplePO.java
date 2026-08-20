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

/** 恋爱关系持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_couple")
public class LoveCouplePO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerUserId;
    private Long partnerUserId;
    private String partnerName;
    private LocalDate startDate;
    private Byte status;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
