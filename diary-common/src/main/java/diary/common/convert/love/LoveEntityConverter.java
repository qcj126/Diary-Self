package diary.common.convert.love;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.love.dto.LoveAnniversaryDTO;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveLocationDTO;
import diary.common.entity.love.dto.LoveMenstrualCycleDTO;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.dto.LoveRecordImageDTO;
import diary.common.entity.love.dto.LoveRecordMoodDTO;
import diary.common.entity.love.dto.LoveRecordTagDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.entity.love.po.LoveAnniversaryPO;
import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.po.LoveLocationPO;
import diary.common.entity.love.po.LoveMenstrualCyclePO;
import diary.common.entity.love.po.LoveMoodPO;
import diary.common.entity.love.po.LoveRecordImagePO;
import diary.common.entity.love.po.LoveRecordMoodPO;
import diary.common.entity.love.po.LoveRecordPO;
import diary.common.entity.love.po.LoveRecordTagPO;
import diary.common.entity.love.po.LoveTagPO;
import diary.common.entity.love.vo.LoveAnniversaryVO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.entity.love.vo.LoveLocationVO;
import diary.common.entity.love.vo.LoveMenstrualCycleVO;
import diary.common.entity.love.vo.LoveMoodVO;
import diary.common.entity.love.vo.LoveRecordImageVO;
import diary.common.entity.love.vo.LoveRecordMoodVO;
import diary.common.entity.love.vo.LoveRecordTagVO;
import diary.common.entity.love.vo.LoveRecordVO;
import diary.common.entity.love.vo.LoveTagVO;

import java.util.List;

/**
 * 恋爱记录模块的实体转换器。
 *
 * <p>转换逻辑集中在这里，Service 不直接逐字段复制，避免同一字段在不同接口中的转换规则不一致。</p>
 */
public final class LoveEntityConverter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LoveEntityConverter() {
    }

    public static LoveCouplePO toPo(LoveCoupleDTO source) {
        return source == null ? null : LoveCouplePO.builder()
                .id(source.getId())
                .ownerUserId(source.getOwnerUserId())
                .partnerUserId(source.getPartnerUserId())
                .partnerName(source.getPartnerName())
                .startDate(source.getStartDate())
                .status(source.getStatus())
                .build();
    }

    public static LoveCoupleVO toVo(LoveCouplePO source) {
        return source == null ? null : LoveCoupleVO.builder()
                .id(source.getId())
                .ownerUserId(source.getOwnerUserId())
                .partnerUserId(source.getPartnerUserId())
                .partnerName(source.getPartnerName())
                .startDate(source.getStartDate())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveCoupleVO toVo(LoveCoupleDTO source) {
        return toVo(toPo(source));
    }

    public static LoveAnniversaryPO toPo(LoveAnniversaryDTO source) {
        return source == null ? null : LoveAnniversaryPO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .name(source.getName())
                .eventDate(source.getEventDate())
                .repeatType(source.getRepeatType())
                .remindDays(source.getRemindDays())
                .pinned(source.getPinned())
                .sort(source.getSort())
                .build();
    }

    public static LoveAnniversaryVO toVo(LoveAnniversaryPO source) {
        return source == null ? null : LoveAnniversaryVO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .name(source.getName())
                .eventDate(source.getEventDate())
                .repeatType(source.getRepeatType())
                .remindDays(source.getRemindDays())
                .pinned(source.getPinned())
                .sort(source.getSort())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveAnniversaryVO toVo(LoveAnniversaryDTO source) {
        return toVo(toPo(source));
    }

    public static LoveLocationPO toPo(LoveLocationDTO source) {
        return source == null ? null : LoveLocationPO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .name(source.getName())
                .address(source.getAddress())
                .longitude(source.getLongitude())
                .latitude(source.getLatitude())
                .cityCode(source.getCityCode())
                .cityName(source.getCityName())
                .build();
    }

    public static LoveLocationVO toVo(LoveLocationPO source) {
        return source == null ? null : LoveLocationVO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .name(source.getName())
                .address(source.getAddress())
                .longitude(source.getLongitude())
                .latitude(source.getLatitude())
                .cityCode(source.getCityCode())
                .cityName(source.getCityName())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveLocationVO toVo(LoveLocationDTO source) {
        return toVo(toPo(source));
    }

    public static LoveRecordPO toPo(LoveRecordDTO source) {
        return source == null ? null : LoveRecordPO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .locationId(source.getLocationId())
                .title(source.getTitle())
                .content(source.getContent())
                .recordDate(source.getRecordDate())
                .categoryCode(source.getCategoryCode())
                .important(source.getImportant())
                .sort(source.getSort())
                .build();
    }

    public static LoveRecordVO toVo(LoveRecordPO source) {
        return source == null ? null : LoveRecordVO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .locationId(source.getLocationId())
                .title(source.getTitle())
                .content(source.getContent())
                .recordDate(source.getRecordDate())
                .categoryCode(source.getCategoryCode())
                .important(source.getImportant())
                .sort(source.getSort())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveRecordVO toVo(LoveRecordDTO source) {
        return toVo(toPo(source));
    }

    public static LoveRecordImagePO toPo(LoveRecordImageDTO source) {
        return source == null ? null : LoveRecordImagePO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .imageId(source.getImageId())
                .isCover(source.getIsCover())
                .sort(source.getSort())
                .build();
    }

    public static LoveRecordImageVO toVo(LoveRecordImagePO source) {
        return source == null ? null : LoveRecordImageVO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .imageId(source.getImageId())
                .isCover(source.getIsCover())
                .sort(source.getSort())
                .createTime(source.getCreateTime())
                .build();
    }

    public static LoveRecordImageVO toVo(LoveRecordImageDTO source) {
        return toVo(toPo(source));
    }

    public static LoveMoodPO toPo(LoveMoodDTO source) {
        return source == null ? null : LoveMoodPO.builder()
                .id(source.getId())
                .moodCode(source.getMoodCode())
                .moodName(source.getMoodName())
                .emoji(source.getEmoji())
                .sort(source.getSort())
                .enabled(source.getEnabled())
                .build();
    }

    public static LoveMoodVO toVo(LoveMoodPO source) {
        return source == null ? null : LoveMoodVO.builder()
                .id(source.getId())
                .moodCode(source.getMoodCode())
                .moodName(source.getMoodName())
                .emoji(source.getEmoji())
                .sort(source.getSort())
                .enabled(source.getEnabled())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveMoodVO toVo(LoveMoodDTO source) {
        return toVo(toPo(source));
    }

    public static LoveRecordMoodPO toPo(LoveRecordMoodDTO source) {
        return source == null ? null : LoveRecordMoodPO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .moodId(source.getMoodId())
                .sort(source.getSort())
                .build();
    }

    public static LoveRecordMoodVO toVo(LoveRecordMoodPO source) {
        return source == null ? null : LoveRecordMoodVO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .moodId(source.getMoodId())
                .sort(source.getSort())
                .createTime(source.getCreateTime())
                .build();
    }

    public static LoveRecordMoodVO toVo(LoveRecordMoodDTO source) {
        return toVo(toPo(source));
    }

    public static LoveTagPO toPo(LoveTagDTO source) {
        return source == null ? null : LoveTagPO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .tagName(source.getTagName())
                .color(source.getColor())
                .useCount(source.getUseCount())
                .build();
    }

    public static LoveTagVO toVo(LoveTagPO source) {
        return source == null ? null : LoveTagVO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .creatorUserId(source.getCreatorUserId())
                .tagName(source.getTagName())
                .color(source.getColor())
                .useCount(source.getUseCount())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveTagVO toVo(LoveTagDTO source) {
        return toVo(toPo(source));
    }

    public static LoveRecordTagPO toPo(LoveRecordTagDTO source) {
        return source == null ? null : LoveRecordTagPO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .tagId(source.getTagId())
                .sort(source.getSort())
                .build();
    }

    public static LoveRecordTagVO toVo(LoveRecordTagPO source) {
        return source == null ? null : LoveRecordTagVO.builder()
                .id(source.getId())
                .recordId(source.getRecordId())
                .tagId(source.getTagId())
                .sort(source.getSort())
                .createTime(source.getCreateTime())
                .build();
    }

    public static LoveRecordTagVO toVo(LoveRecordTagDTO source) {
        return toVo(toPo(source));
    }

    public static LoveMenstrualCyclePO toPo(LoveMenstrualCycleDTO source) {
        return source == null ? null : LoveMenstrualCyclePO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .subjectUserId(source.getSubjectUserId())
                .recorderUserId(source.getRecorderUserId())
                .periodStartDate(source.getPeriodStartDate())
                .periodEndDate(source.getPeriodEndDate())
                .cycleLength(source.getCycleLength())
                .periodLength(source.getPeriodLength())
                .symptoms(writeSymptoms(source.getSymptoms()))
                .note(source.getNote())
                .privacyScope(source.getPrivacyScope())
                .build();
    }

    public static LoveMenstrualCycleVO toVo(LoveMenstrualCyclePO source) {
        return source == null ? null : LoveMenstrualCycleVO.builder()
                .id(source.getId())
                .coupleId(source.getCoupleId())
                .subjectUserId(source.getSubjectUserId())
                .recorderUserId(source.getRecorderUserId())
                .periodStartDate(source.getPeriodStartDate())
                .periodEndDate(source.getPeriodEndDate())
                .cycleLength(source.getCycleLength())
                .periodLength(source.getPeriodLength())
                .symptoms(readSymptoms(source.getSymptoms()))
                .note(source.getNote())
                .privacyScope(source.getPrivacyScope())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public static LoveMenstrualCycleVO toVo(LoveMenstrualCycleDTO source) {
        return toVo(toPo(source));
    }

    private static String writeSymptoms(List<String> symptoms) {
        if (symptoms == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(symptoms);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("symptoms 无法转换为 JSON", e);
        }
    }

    private static List<String> readSymptoms(String symptoms) {
        if (symptoms == null || symptoms.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(symptoms, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("symptoms JSON 格式不正确", e);
        }
    }
}
