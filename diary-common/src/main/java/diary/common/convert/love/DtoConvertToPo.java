package diary.common.convert.love;

import diary.common.entity.love.dto.*;
import diary.common.entity.love.po.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DtoConvertToPo {
    public static LoveCouplePO convertToPo(LoveCoupleDTO loveCoupleDTO) {
        return LoveCouplePO.builder()
                .id(loveCoupleDTO.getId())
                .ownerUserId(loveCoupleDTO.getOwnerUserId())
                .partnerUserId(loveCoupleDTO.getPartnerUserId())
                .partnerName(loveCoupleDTO.getPartnerName())
                .startDate(loveCoupleDTO.getStartDate())
                .status(loveCoupleDTO.getStatus())
                .build();
    }
    public static LoveAnniversaryPO convertToPo(LoveAnniversaryDTO loveAnniversaryDTO) {
        return LoveAnniversaryPO.builder()
                .id(loveAnniversaryDTO.getId())
                .coupleId(loveAnniversaryDTO.getCoupleId())
                .creatorUserId(loveAnniversaryDTO.getCreatorUserId())
                .name(loveAnniversaryDTO.getName())
                .eventDate(loveAnniversaryDTO.getEventDate())
                .repeatType(loveAnniversaryDTO.getRepeatType())
                .remindDays(loveAnniversaryDTO.getRemindDays())
                .pinned(loveAnniversaryDTO.getPinned())
                .sort(loveAnniversaryDTO.getSort())
                .build();
    }
    public static LoveLocationPO convertToPo(LoveLocationDTO loveLocationDTO) {
        return LoveLocationPO.builder()
                .id(loveLocationDTO.getId())
                .coupleId(loveLocationDTO.getCoupleId())
                .name(loveLocationDTO.getName())
                .address(loveLocationDTO.getAddress())
                .longitude(loveLocationDTO.getLongitude())
                .latitude(loveLocationDTO.getLatitude())
                .cityCode(loveLocationDTO.getCityCode())
                .cityName(loveLocationDTO.getCityName())
                .build();
    }
    public static LoveRecordPO convertToPo(LoveRecordDTO loveRecordDTO) {
        return LoveRecordPO.builder()
                .id(loveRecordDTO.getId())
                .coupleId(loveRecordDTO.getCoupleId())
                .creatorUserId(loveRecordDTO.getCreatorUserId())
                .locationId(loveRecordDTO.getLocationId())
                .title(loveRecordDTO.getTitle())
                .content(loveRecordDTO.getContent())
                .recordDate(loveRecordDTO.getRecordDate())
                .categoryCode(loveRecordDTO.getCategoryCode())
                .important(loveRecordDTO.getImportant())
                .sort(loveRecordDTO.getSort())
                .build();
    }
    public static LoveRecordImagePO convertToPo(LoveRecordImageDTO loveRecordImageDTO) {
        return LoveRecordImagePO.builder()
                .id(loveRecordImageDTO.getId())
                .recordId(loveRecordImageDTO.getRecordId())
                .imageId(loveRecordImageDTO.getImageId())
                .isCover(loveRecordImageDTO.getIsCover())
                .sort(loveRecordImageDTO.getSort())
                .build();
    }
    public static LoveMoodPO convertToPo(LoveMoodDTO loveMoodDTO) {
        return LoveMoodPO.builder()
                .id(loveMoodDTO.getId())
                .moodCode(loveMoodDTO.getMoodCode())
                .moodName(loveMoodDTO.getMoodName())
                .emoji(loveMoodDTO.getEmoji())
                .sort(loveMoodDTO.getSort())
                .enabled(loveMoodDTO.getEnabled())
                .build();
    }
    public static LoveRecordMoodPO convertToPo(LoveRecordMoodDTO loveMoodDTO) {
        return LoveRecordMoodPO.builder()
                .id(loveMoodDTO.getId())
                .recordId(loveMoodDTO.getRecordId())
                .moodId(loveMoodDTO.getMoodId())
                .sort(loveMoodDTO.getSort())
                .build();
    }
    public static LoveTagPO convertToPo(LoveTagDTO loveTagDTO) {
        return LoveTagPO.builder()
                .id(loveTagDTO.getId())
                .coupleId(loveTagDTO.getCoupleId())
                .creatorUserId(loveTagDTO.getCreatorUserId())
                .tagName(loveTagDTO.getTagName())
                .color(loveTagDTO.getColor())
                .useCount(loveTagDTO.getUseCount())
                .build();
    }
    public static LoveRecordTagPO convertToPo(LoveRecordTagDTO loveTagDTO) {
        return LoveRecordTagPO.builder()
                .id(loveTagDTO.getId())
                .recordId(loveTagDTO.getRecordId())
                .tagId(loveTagDTO.getTagId())
                .sort(loveTagDTO.getSort())
                .build();
    }
    public static LoveRecordPO convertToPo(UpdateLoveRecordDto updateLoveRecordDto) {
        return LoveRecordPO.builder()
                .id(updateLoveRecordDto.getRecordId())
                .coupleId(updateLoveRecordDto.getCoupleId())
                .locationId(updateLoveRecordDto.getLocationId())
                .title(updateLoveRecordDto.getTitle())
                .content(updateLoveRecordDto.getContent())
                .recordDate(updateLoveRecordDto.getRecordDate())
                .categoryCode(updateLoveRecordDto.getCategoryCode())
                .important(updateLoveRecordDto.getImportant())
                .sort(updateLoveRecordDto.getSort())
                .build();
    }

}
