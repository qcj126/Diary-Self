package diary.diaryinfo.mapper;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.po.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiaryLoveMapper {

    int insertLoveMood(LoveMoodPO loveMoodPO);

    List<LoveMoodPO> selectLoveMoods(@Param("enabled") Boolean enabled);

    int updateLoveMood(LoveMoodPO loveMoodPO);

    int deleteLoveMood(@Param("id") Long id);
}
