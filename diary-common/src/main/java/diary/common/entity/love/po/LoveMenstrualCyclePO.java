package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生理期记录持久化对象；symptoms 保存为 JSON 数组字符串。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMenstrualCyclePO {
    private Long id;
    private Long coupleId;
    private Long subjectUserId;
    private Long recorderUserId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer cycleLength;
    private Integer periodLength;
    private String symptoms;
    private String note;
    private Byte privacyScope;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
