package diary.common.entity.love.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UpdateLoveRecordDto {
    @NotNull
    private Long recordId;

    @NotNull
    private Long coupleId;

    @NotBlank
    private String title;

    private String content;

    @NotNull
    private LocalDate recordDate;

    @Pattern(regexp = "DATE|DAILY|TRAVEL|ANNIVERSARY")
    private String categoryCode;

    private Boolean important;
    private Integer sort;

    private Long locationId;

    @Valid
    private LoveLocationDTO loveLocationDTO;

    /** 修改后最终保留的全部图片 */
    @Valid
    private List<LoveRecordImageDTO> loveRecordImageDTOS;

    /** 修改后最终保留的全部心情ID */
    private List<Long> moodIds;

    /** 修改后最终保留的已有标签ID */
    private List<Long> tagIds;

    /** 编辑过程中新增的标签 */
    @Valid
    private List<LoveTagDTO> loveTagDTOS;

    /** 乐观锁版本号，推荐后续添加 */
    private Integer version;
}
