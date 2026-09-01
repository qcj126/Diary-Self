package diary.diarylove.mapper;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.po.LoveAnniversaryPO;
import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.po.LoveLocationPO;
import diary.common.entity.love.po.LoveMoodPO;
import diary.common.entity.love.po.LoveRecordImagePO;
import diary.common.entity.love.po.LoveRecordMoodPO;
import diary.common.entity.love.po.LoveRecordPO;
import diary.common.entity.love.po.LoveRecordTagPO;
import diary.common.entity.love.po.LoveTagPO;
import diary.common.entity.mq.po.MqOutboxPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiaryLoveMapper {
    int insertLoveCouple(LoveCouplePO loveCouplePO);

    LoveCouplePO selectLoveCouple(@Param("query") LoveCoupleDTO query);

    int updateLoveCouple(LoveCouplePO loveCouplePO);

    int deleteLoveCouple(@Param("id") Long id);

    int insertLoveAnniversary(LoveAnniversaryPO loveAnniversaryPO);

    LoveAnniversaryPO selectLoveAnniversaryById(@Param("id") Long id);

    List<LoveAnniversaryPO> selectLoveAnniversariesByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveAnniversary(LoveAnniversaryPO loveAnniversaryPO);

    int deleteLoveAnniversary(@Param("id") Long id);

    int insertLoveLocation(LoveLocationPO loveLocationPO);

    LoveLocationPO selectLoveLocationById(@Param("id") Long id);

    List<LoveLocationPO> selectLoveLocationsByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveLocation(LoveLocationPO loveLocationPO);

    int deleteLoveLocation(@Param("id") Long id);

    int insertLoveRecord(LoveRecordPO loveRecordPO);

    LoveRecordPO selectLoveRecordById(@Param("id") Long id);

    List<LoveRecordPO> selectLoveRecords(@Param("query") LoveRecordDTO query);

    int updateLoveRecord(LoveRecordPO loveRecordPO);

    int deleteLoveRecord(@Param("id") Long id);

    int insertLoveRecordImage(@Param("loveRecordImagePOList") List<LoveRecordImagePO> loveRecordImagePOList);

    LoveRecordImagePO selectLoveRecordImageById(@Param("id") Long id);

    List<LoveRecordImagePO> selectLoveRecordImagesByRecordId(@Param("recordId") Long recordId);

    int updateLoveRecordImage(LoveRecordImagePO loveRecordImagePO);

    int deleteLoveRecordImage(@Param("id") Long id);

    int insertLoveMood(LoveMoodPO loveMoodPO);

    LoveMoodPO selectLoveMoodById(@Param("id") Long id);

    List<LoveMoodPO> selectLoveMoods(@Param("enabled") Boolean enabled);

    int updateLoveMood(LoveMoodPO loveMoodPO);

    int deleteLoveMood(@Param("id") Long id);

    int insertLoveRecordMood(LoveRecordMoodPO loveRecordMoodPO);

    LoveRecordMoodPO selectLoveRecordMoodById(@Param("id") Long id);

    List<LoveRecordMoodPO> selectLoveRecordMoodsByRecordId(@Param("recordId") Long recordId);

    int updateLoveRecordMood(LoveRecordMoodPO loveRecordMoodPO);

    int deleteLoveRecordMood(@Param("id") Long id);

    int insertLoveTag(LoveTagPO loveTagPO);

    LoveTagPO selectLoveTagById(@Param("id") Long id);

    List<LoveTagPO> selectLoveTagsByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveTag(LoveTagPO loveTagPO);

    int deleteLoveTag(@Param("id") Long id);

    int insertLoveRecordTag(LoveRecordTagPO loveRecordTagPO);

    LoveRecordTagPO selectLoveRecordTagById(@Param("id") Long id);

    List<LoveRecordTagPO> selectLoveRecordTagsByRecordId(@Param("recordId") Long recordId);

    int updateLoveRecordTag(LoveRecordTagPO loveRecordTagPO);

    int deleteLoveRecordTag(@Param("id") Long id);

    int insertOutbox(MqOutboxPO outbox);
}
