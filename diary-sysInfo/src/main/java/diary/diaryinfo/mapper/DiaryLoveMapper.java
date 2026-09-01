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

    int insertLoveRecordMood(LoveRecordMoodPO loveRecordMoodPO);

    int insertLoveTag(LoveTagPO loveTagPO);

    int insertLoveRecordTag(LoveRecordTagPO loveRecordTagPO);
}
