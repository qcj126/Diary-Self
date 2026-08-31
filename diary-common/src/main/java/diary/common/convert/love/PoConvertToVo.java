package diary.common.convert.love;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import diary.common.entity.love.po.*;
import diary.common.entity.love.vo.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PoConvertToVo {
    public static LoveCoupleVO convertToVo(LoveCouplePO loveCouplePO) {
        return LoveCoupleVO.builder()
                .id(loveCouplePO.getId())
                .ownerUserId(loveCouplePO.getOwnerUserId())
                .partnerUserId(loveCouplePO.getPartnerUserId())
                .partnerName(loveCouplePO.getPartnerName())
                .startDate(loveCouplePO.getStartDate())
                .status(loveCouplePO.getStatus())
                .createTime(loveCouplePO.getCreateTime())
                .updateTime(loveCouplePO.getUpdateTime())
                .build();
    }
    public static LoveAnniversaryVO convertToVo(LoveAnniversaryPO loveAnniversaryPO) {
        return LoveAnniversaryVO.builder()
                .id(loveAnniversaryPO.getId())
                .coupleId(loveAnniversaryPO.getCoupleId())
                .creatorUserId(loveAnniversaryPO.getCreatorUserId())
                .name(loveAnniversaryPO.getName())
                .eventDate(loveAnniversaryPO.getEventDate())
                .repeatType(loveAnniversaryPO.getRepeatType())
                .remindDays(loveAnniversaryPO.getRemindDays())
                .pinned(loveAnniversaryPO.getPinned())
                .sort(loveAnniversaryPO.getSort())
                .createTime(loveAnniversaryPO.getCreateTime())
                .updateTime(loveAnniversaryPO.getUpdateTime())
                .build();
    }
    public static LoveLocationVO convertToVo(LoveLocationPO loveLocationPO) {
        return LoveLocationVO.builder()
                .id(loveLocationPO.getId())
                .coupleId(loveLocationPO.getCoupleId())
                .name(loveLocationPO.getName())
                .address(loveLocationPO.getAddress())
                .longitude(loveLocationPO.getLongitude())
                .latitude(loveLocationPO.getLatitude())
                .cityCode(loveLocationPO.getCityCode())
                .cityName(loveLocationPO.getCityName())
                .createTime(loveLocationPO.getCreateTime())
                .updateTime(loveLocationPO.getUpdateTime())
                .build();
    }
    public static LoveRecordVO convertToVo(LoveRecordPO loveRecordPO) {
        return LoveRecordVO.builder()
                .id(loveRecordPO.getId())
                .coupleId(loveRecordPO.getCoupleId())
                .creatorUserId(loveRecordPO.getCreatorUserId())
                .locationId(loveRecordPO.getLocationId())
                .title(loveRecordPO.getTitle())
                .content(loveRecordPO.getContent())
                .recordDate(loveRecordPO.getRecordDate())
                .categoryCode(loveRecordPO.getCategoryCode())
                .important(loveRecordPO.getImportant())
                .sort(loveRecordPO.getSort())
                .createTime(loveRecordPO.getCreateTime())
                .updateTime(loveRecordPO.getUpdateTime())
                .build();
    }
    public static LoveRecordImageVO convertToVo(LoveRecordImagePO loveRecordImagePO) {
        return LoveRecordImageVO.builder()
                .id(loveRecordImagePO.getId())
                .recordId(loveRecordImagePO.getRecordId())
                .imageId(loveRecordImagePO.getImageId())
                .isCover(loveRecordImagePO.getIsCover())
                .sort(loveRecordImagePO.getSort())
                .createTime(loveRecordImagePO.getCreateTime())
                .build();
    }
    public static LoveMoodVO convertToVo(LoveMoodPO loveMoodPO) {
        return LoveMoodVO.builder()
                .id(loveMoodPO.getId())
                .moodCode(loveMoodPO.getMoodCode())
                .moodName(loveMoodPO.getMoodName())
                .emoji(loveMoodPO.getEmoji())
                .sort(loveMoodPO.getSort())
                .enabled(loveMoodPO.getEnabled())
                .createTime(loveMoodPO.getCreateTime())
                .updateTime(loveMoodPO.getUpdateTime())
                .build();
    }
    public static LoveRecordMoodVO convertToVo(LoveRecordMoodPO loveRecordMoodPO) {
        return LoveRecordMoodVO.builder()
                .id(loveRecordMoodPO.getId())
                .recordId(loveRecordMoodPO.getRecordId())
                .moodId(loveRecordMoodPO.getMoodId())
                .sort(loveRecordMoodPO.getSort())
                .createTime(loveRecordMoodPO.getCreateTime())
                .build();
    }
    public static LoveTagVO convertToVo(LoveTagPO loveTagPO) {
        return LoveTagVO.builder()
                .id(loveTagPO.getId())
                .coupleId(loveTagPO.getCoupleId())
                .creatorUserId(loveTagPO.getCreatorUserId())
                .tagName(loveTagPO.getTagName())
                .color(loveTagPO.getColor())
                .useCount(loveTagPO.getUseCount())
                .createTime(loveTagPO.getCreateTime())
                .updateTime(loveTagPO.getUpdateTime())
                .build();
    }
    public static LoveRecordTagVO convertToVo(LoveRecordTagPO loveRecordTagPO) {
        return LoveRecordTagVO.builder()
                .id(loveRecordTagPO.getId())
                .recordId(loveRecordTagPO.getRecordId())
                .tagId(loveRecordTagPO.getTagId())
                .sort(loveRecordTagPO.getSort())
                .createTime(loveRecordTagPO.getCreateTime())
                .build();
    }
}
