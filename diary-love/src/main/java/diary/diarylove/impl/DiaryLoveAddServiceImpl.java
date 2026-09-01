package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.love.dto.*;
import diary.common.entity.love.po.LoveRecordImagePO;
import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.properties.LoveRecordProperties;
import diary.diarylove.service.DiaryLoveAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static diary.common.consts.MqTaskConst.AI_TASK_AGGREGATE_TYPE;
import static diary.common.consts.MqTaskConst.OUTBOX_EVENT_ID;
import static diary.common.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;
import static diary.common.convert.love.LargeDtoConvertToTinyDto.*;
import static diary.utils.commonutil.MyUtils.writeJson;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryLoveAddServiceImpl implements DiaryLoveAddService {
    private static final Set<String> RECORD_CATEGORIES = Set.of("DATE", "DAILY", "TRAVEL", "ANNIVERSARY");

    private final LoveRecordProperties properties;
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addCouples(LoveCoupleDTO dto) {
        validateCouple(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getStatus() == null) dto.setStatus(1);
        return addResult(diaryLoveMapper.insertLoveCouple(DtoConvertToPo.convertToPo(dto)), "添加情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto) {
        validateAnniversary(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getRepeatType() == null) dto.setRepeatType((byte) 1);
        if (dto.getRemindDays() == null) dto.setRemindDays(7);
        if (dto.getPinned() == null) dto.setPinned(false);
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveAnniversary(DtoConvertToPo.convertToPo(dto)), "添加纪念日成功");
    }

    // 将记录、图片、心情、标签和地点关联合并为一个事务用例，并在同一事务写 Outbox，方便后续异步处理：
    // 消费者异步维护：
    //- 周/月记录数量
    //- 约会、旅行等分类统计
    //- 年度回顾数据
    //- 地点访问次数、城市足迹
    //- 相册封面和图片数量
    //- 心情趋势
    //- 标签使用次数 love_tag.use_count
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecord(AddLoveRecordDto dto) {

        validateRecord(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getImportant() == null) dto.setImportant(false);
        if (dto.getSort() == null) dto.setSort(0);

        // 先构建对应的dto，再构建对应的po
        // 构建locationDto
        LoveLocationDTO locationDto = null;
        if (dto.getLocationId() == null && dto.getNewLocation() != null) {
            locationDto = convertLoveLocation(dto);
        }
        // 构建recordDto
        Long locationId = locationDto == null ? dto.getLocationId() : locationDto.getId();
        dto.setLocationId(locationId);
        LoveRecordDTO loveRecordDTO = convertLoveRecord(dto);
        // 构建recordImageDto
        List<LoveRecordImageDTO> loveRecordImageDTOS = convertLoveRecordImage(dto, loveRecordDTO);

        // 构建mqoutboxpo
        LocalDateTime now = LocalDateTime.now();
        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();
        LoveRecordMessageDto message = LoveRecordMessageDto.builder()
                .eventId(eventId)
                .recordId(loveRecordDTO.getId())
                .userId(10000L)
                .clientRequestId(dto.getClientRequestId())
                .taskType(properties.getRocketmq().getTaskTag())
                .eventType(OutboxEventTypeEnum.AI_TASK_CREATED.name())
                .taskStatus(AiTaskStatusEnum.PENDING.name())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AI_TASK_AGGREGATE_TYPE)
                .aggregateId(loveRecordDTO.getId())
                .eventType(OutboxEventTypeEnum.AI_TASK_CREATED.name())
                .topic(properties.getRocketmq().getTaskTopic())
                .tag(properties.getRocketmq().getTaskTag())
                .messageKey(loveRecordDTO.getId().toString())
                .payload(writeJson(message, "恋爱记录消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();

        int outboxCnt = diaryLoveMapper.insertOutbox(outbox);
        // 将dto转为po，然后插入数据库
        int locationCnt = 1;
        if (locationDto != null) {
            locationCnt = diaryLoveMapper.insertLoveLocation(DtoConvertToPo.convertToPo(locationDto));
        }
        int recordCnt = diaryLoveMapper.insertLoveRecord(DtoConvertToPo.convertToPo(loveRecordDTO));

        List<LoveRecordImagePO> recordImagePOList = new ArrayList<>();
        for (LoveRecordImageDTO loveRecordImageDTO : loveRecordImageDTOS) {
            recordImagePOList.add(DtoConvertToPo.convertToPo(loveRecordImageDTO));
        }
        int recordImageCnt = diaryLoveMapper.insertLoveRecordImage(recordImagePOList);

        if (locationCnt > 0 && recordCnt > 0 && recordImageCnt > 0 && outboxCnt > 0) {
            return ApiResponse.success("添加记录成功");
        }
        log.info("添加记录失败: locationCnt={}, recordCnt={}, recordImageCnt={}, outboxCnt={}", locationCnt, recordCnt, recordImageCnt, outboxCnt);
        return ApiResponse.addFail();
    }

    // mood和tag后续移入sysInfo模块


    static void validateCouple(LoveCoupleDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveCoupleDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getOwnerUserId(), "ownerUserId")
                .notEmpty(dto.getPartnerName(), "partnerName")
                .notEmpty(dto.getStartDate(), "startDate");
    }

    static void validateAnniversary(LoveAnniversaryDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveAnniversaryDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getName(), "name")
                .notNull(dto.getEventDate(), "eventDate");
    }

    static void validateLocation(LoveLocationDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveLocationDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId").notEmpty(dto.getName(), "name");
        if ((dto.getLongitude() == null) != (dto.getLatitude() == null)) {
            throw new IllegalArgumentException("longitude 和 latitude 必须同时填写或同时为空");
        }
    }

    static void validateRecord(AddLoveRecordDto dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notEmpty(dto.getTitle(), "title")
                .notNull(dto.getRecordDate(), "recordDate")
                .notEmpty(dto.getCategoryCode(), "categoryCode");
        if (!RECORD_CATEGORIES.contains(dto.getCategoryCode())) {
            throw new IllegalArgumentException("categoryCode 只能是 DATE、DAILY、TRAVEL 或 ANNIVERSARY");
        }
    }

    static void validateRecordImage(LoveRecordImageDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordImageDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getImageId(), "imageId");
    }

    static void validateMood(LoveMoodDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveMoodDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notEmpty(dto.getMoodCode(), "moodCode").notEmpty(dto.getMoodName(), "moodName");
    }

    static void validateRecordMood(LoveRecordMoodDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordMoodDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getMoodId(), "moodId");
    }

    static void validateTag(LoveTagDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveTagDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getTagName(), "tagName");
    }

    static void validateRecordTag(LoveRecordTagDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordTagDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getTagId(), "tagId");
    }

    private ApiResponse<String> addResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.addFail();
    }
}
