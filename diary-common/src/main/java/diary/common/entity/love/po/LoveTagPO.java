package diary.common.entity.love.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录自定义标签持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("love_tag")
public class LoveTagPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long coupleId;
    private Long creatorUserId;
    private String tagName;
    private String color;
    private Integer useCount;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
