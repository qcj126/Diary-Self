package diary.common.convert.timemachine;


import diary.common.entity.timemachine.dto.TimeCategoryIconDTO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;

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
