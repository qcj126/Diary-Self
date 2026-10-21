package diary.diarytimemachine.entity.convert;

import diary.diarytimemachine.entity.dto.TimeCategoryIconDTO;
import diary.diarytimemachine.entity.vo.TimeCategoryVO;

public class DtoToVo {
    public static TimeCategoryVO convertToVo(TimeCategoryIconDTO timeCategoryIconDTO) {
        return TimeCategoryVO.builder()
                .id(timeCategoryIconDTO.getCategoryId())
                .userId(timeCategoryIconDTO.getUserId())
                .categoryName(timeCategoryIconDTO.getCategoryName())
                .sort(timeCategoryIconDTO.getSort())
                .iconName(timeCategoryIconDTO.getIconName())
                .iconPath(timeCategoryIconDTO.getIconPath())
                .build();
    }
}
