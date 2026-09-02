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

    int insertLoveTag(LoveTagPO loveTagPO);

    List<LoveMoodPO> selectLoveMoods(@Param("enabled") Boolean enabled);

    List<LoveTagPO> selectLoveTagsByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveMood(LoveMoodPO loveMoodPO);

    int updateLoveTag(LoveTagPO loveTagPO);

    int deleteLoveMood(@Param("id") Long id);

    int deleteLoveTag(@Param("id") Long id);
}
