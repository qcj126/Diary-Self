package diary.common.entity.love.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AddLoveRecordDto {
    private Long id;

    @NotNull
    private Long coupleId;

    @NotBlank
    @Size(max = 100)
    private String title;

    private String content;

    private String clientRequestId;

    @NotNull
    private LocalDate recordDate;

    @NotBlank
    @Pattern(regexp = "DATE|DAILY|TRAVEL|ANNIVERSARY")
    private String categoryCode;

    private Boolean important;

    @Min(0)
    private Integer sort;

    /** 使用已有地点 */
    private Long locationId;

    /** 没有已有地点时，随记录一起创建 */
    @Valid
    private NewLocationDto newLocation;

    /** 图片关联 */
    @Valid
    private List<NewRecordImageDto> images;

    /** 已有心情字典ID */
    private List<@NotNull Long> moodIds;

    /** 已有标签ID */
    private List<@NotNull Long> tagIds;

    /** 用户在新增记录时临时创建的新标签 */
    @Valid
    private List<NewTagDto> newTags;
}
