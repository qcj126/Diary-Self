package diary.common.convert.love;

import diary.common.entity.love.dto.*;
import diary.common.util.MyUtil;

import java.util.ArrayList;
import java.util.List;

public class LargeDtoConvertToTinyDto {
    // 构建location
    public static LoveLocationDTO convertLoveLocation(AddLoveRecordDto dto) {
        return LoveLocationDTO.builder()
                .id(MyUtil.getPrimaryKey())
                .coupleId(dto.getCoupleId())
                .name(dto.getNewLocation().getName())
                .address(dto.getNewLocation().getAddress())
                .longitude(dto.getNewLocation().getLongitude())
                .latitude(dto.getNewLocation().getLatitude())
                .cityCode(dto.getNewLocation().getCityCode())
                .cityName(dto.getNewLocation().getCityName())
                .build();
    }

    // 构建record
    public static LoveRecordDTO convertLoveRecord(AddLoveRecordDto dto) {
        return LoveRecordDTO.builder()
                .id(MyUtil.getPrimaryKey())
                .coupleId(dto.getCoupleId())
                .creatorUserId(10000L)
                .locationId(dto.getLocationId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .recordDate(dto.getRecordDate())
                .categoryCode(dto.getCategoryCode())
                .important(dto.getImportant())
                .sort(dto.getSort())
                .build();
    }

    // 构建recordImage
    public static List<LoveRecordImageDTO> convertLoveRecordImage(AddLoveRecordDto dto, LoveRecordDTO loveRecordDTO) {
        List<LoveRecordImageDTO> recordImageDTOS = new ArrayList<>();
        if (dto.getImages() == null) return recordImageDTOS;
        for (NewRecordImageDto image : dto.getImages()) {
            LoveRecordImageDTO recordImageDTO = LoveRecordImageDTO.builder()
                    .id(MyUtil.getPrimaryKey())
                    .recordId(loveRecordDTO.getId())
                    .imageId(image.getImageId())
                    .isCover(image.getIsCover() == null ? false : image.getIsCover())
                    .sort(image.getSort() == null ? 0 : image.getSort())
                    .build();
            recordImageDTOS.add(recordImageDTO);
        }
        return recordImageDTOS;
    }

    // 构建recordTag
    public static List<LoveTagDTO> convertLoveTag(AddLoveRecordDto dto, LoveRecordDTO loveRecordDTO) {
        List<LoveTagDTO> loveTagDTOS = new ArrayList<>();
        if (dto.getNewTags() == null) return loveTagDTOS;
        for (int index = 0; index < dto.getNewTags().size(); index++) {
            NewLoveTag source = dto.getNewTags().get(index);
            LoveTagDTO loveTagDTO = new LoveTagDTO();
            loveTagDTO.setId(MyUtil.getPrimaryKey());
            loveTagDTO.setRecordId(loveRecordDTO.getId());
            loveTagDTO.setCoupleId(dto.getCoupleId());
            loveTagDTO.setCreatorUserId(loveRecordDTO.getCreatorUserId());
            loveTagDTO.setTagName(source.getTagName());
            loveTagDTO.setColor(source.getColor());
            loveTagDTO.setSort(source.getSort() == null ? index : source.getSort());
            loveTagDTOS.add(loveTagDTO);
        }
        return loveTagDTOS;
    }
}
