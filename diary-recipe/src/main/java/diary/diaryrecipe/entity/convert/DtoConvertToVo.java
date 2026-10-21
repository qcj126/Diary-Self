package diary.diaryrecipe.entity.convert;

import diary.diaryrecipe.entity.dto.RecipeCategoryIconDto;
import diary.diaryrecipe.entity.vo.RecipeCategoryVO;
import lombok.Data;

@Data
public class DtoConvertToVo {
    public static RecipeCategoryVO convertToRecipeCategoryVO(RecipeCategoryIconDto dto) {
        return RecipeCategoryVO.builder()
                .id(dto.getCategoryId())
                .userId(dto.getUserId())
                .categoryName(dto.getCategoryName())
                .categoryNum(dto.getCategoryNum())
                .iconId(dto.getIconId())
                .iconPath(dto.getIconPath())
                .sort(dto.getSort())
                .build();
    }
}
