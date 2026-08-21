package diary.diarylove.mapper;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.po.LoveCouplePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaryLoveMapper {
    int insertLoveCouples(LoveCouplePO loveCouplePO);

    LoveCouplePO queryCouples(LoveCoupleDTO loveCoupleDTO);
}
