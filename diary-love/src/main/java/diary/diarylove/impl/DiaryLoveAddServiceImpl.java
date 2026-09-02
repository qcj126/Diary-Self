package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.*;
import diary.common.entity.love.po.LoveRecordImagePO;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static diary.common.convert.love.LargeDtoConvertToTinyDto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryLoveAddServiceImpl implements DiaryLoveAddService {
    private static final Set<String> RECORD_CATEGORIES = Set.of("DATE", "DAILY", "TRAVEL", "ANNIVERSARY");

    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addCouples(LoveCoupleDTO dto) {
        validateCouple(dto);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getStatus() == null) dto.setStatus(1);
        return addResult(diaryLoveMapper.insertLoveCouple(DtoConvertToPo.convertToPo(dto)), "添加情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto) {
        validateAnniversary(dto);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getRepeatType() == null) dto.setRepeatType((byte) 1);
        if (dto.getRemindDays() == null) dto.setRemindDays(7);
        if (dto.getPinned() == null) dto.setPinned(false);
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveAnniversary(DtoConvertToPo.convertToPo(dto)), "添加纪念日成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecord(AddLoveRecordDto dto) {

        validateRecord(dto);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getImportant() == null) dto.setImportant(false);
        if (dto.getSort() == null) dto.setSort(0);

        // 先构建对应的dto，再构建对应的po
        // 构建locationDto
        LoveLocationDTO locationDto = null;
        if (dto.getLocationId() == null && dto.getNewLocation() != null) {
            locationDto = convertLoveLocation(dto);
            validateLocation(locationDto);
        }
        // 构建recordDto
        Long locationId = locationDto == null ? dto.getLocationId() : locationDto.getId();
        dto.setLocationId(locationId);
        LoveRecordDTO loveRecordDTO = convertLoveRecord(dto);
        // 构建recordImageDto
        List<LoveRecordImageDTO> loveRecordImageDTOS = convertLoveRecordImage(dto, loveRecordDTO);

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

        if (locationCnt > 0 && recordCnt > 0 && recordImageCnt > 0) {
            return ApiResponse.success("添加记录成功");
        }
        log.info("添加记录失败: locationCnt={}, recordCnt={}, recordImageCnt={}", locationCnt, recordCnt, recordImageCnt);
        return ApiResponse.addFail();
    }

    void validateCouple(LoveCoupleDTO dto) {
        MyUtils.check().notNull(dto, "loveCoupleDTO")
                .notNull(dto.getOwnerUserId(), "ownerUserId")
                .notEmpty(dto.getPartnerName(), "partnerName")
                .notEmpty(dto.getStartDate(), "startDate");
    }

    void validateAnniversary(LoveAnniversaryDTO dto) {
        MyUtils.check().notNull(dto, "loveAnniversaryDTO")
                .notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getName(), "name")
                .notNull(dto.getEventDate(), "eventDate");
    }

    void validateLocation(LoveLocationDTO dto) {
        MyUtils.check().notNull(dto, "loveLocationDTO")
                .notNull(dto.getCoupleId(), "coupleId")
                .notEmpty(dto.getName(), "name");
        if ((dto.getLongitude() == null) != (dto.getLatitude() == null)) {
            throw new IllegalArgumentException("longitude 和 latitude 必须同时填写或同时为空");
        }
    }

    void validateRecord(AddLoveRecordDto dto) {
        MyUtils.check().notNull(dto, "loveRecordDTO")
                .notNull(dto.getCoupleId(), "coupleId")
                .notEmpty(dto.getTitle(), "title")
                .notNull(dto.getRecordDate(), "recordDate")
                .notEmpty(dto.getCategoryCode(), "categoryCode");
        if (!RECORD_CATEGORIES.contains(dto.getCategoryCode())) {
            throw new IllegalArgumentException("categoryCode 只能是 DATE、DAILY、TRAVEL 或 ANNIVERSARY");
        }
    }

    private ApiResponse<String> addResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.addFail();
    }
}
