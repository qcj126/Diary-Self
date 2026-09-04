package diary.diarylove.mapper;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.po.*;
import diary.common.entity.mq.po.MqOutboxPO;
import jakarta.validation.constraints.NotNull;
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

    List<LoveAnniversaryPO> selectLoveAnniversariesByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveAnniversary(LoveAnniversaryPO loveAnniversaryPO);

    int deleteLoveAnniversary(@Param("id") Long id);

    int insertLoveLocation(LoveLocationPO loveLocationPO);

    List<LoveLocationPO> selectLoveLocationsByCoupleId(@Param("coupleId") Long coupleId);

    int updateLoveLocation(LoveLocationPO loveLocationPO);

    int deleteLoveLocation(@Param("id") Long id);

    int insertLoveRecord(LoveRecordPO loveRecordPO);

    LoveRecordPO selectLoveRecordById(@Param("id") Long id);

    List<LoveRecordPO> selectLoveRecords(@Param("query") LoveRecordDTO query);

    int updateLoveRecord(LoveRecordPO loveRecordPO);

    int deleteLoveRecord(@Param("id") Long id);

    int insertLoveRecordImage(@Param("loveRecordImagePOList") List<LoveRecordImagePO> loveRecordImagePOList);

    List<LoveRecordImagePO> selectLoveRecordImagesByRecordId(@Param("recordId") Long recordId);

    int updateLoveRecordImage(LoveRecordImagePO loveRecordImagePO);

    int deleteLoveRecordImage(@Param("id") Long id);

    List<MqOutboxPO> selectReadyOutbox(int publisherBatchSize);

    int claimOutbox(Long id, Integer versionId);

    int markOutboxSent(Long id, Integer versionId, String brokerMessageId);

    Long selectUserIdByPartnerName(String partnerName);

    int selectExistLoveCouple(@Param("ownerUserId") Long ownerUserId);
}
