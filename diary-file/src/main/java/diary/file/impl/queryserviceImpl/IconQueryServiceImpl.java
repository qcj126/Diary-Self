package diary.file.impl.queryserviceImpl;

import diary.common.convert.file.PoConvertToVo;
import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.entity.file.vo.IconVO;
import diary.common.result.ApiResponse;
import diary.file.mapper.IconMapper;
import diary.file.service.queryservice.IconQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconQueryServiceImpl implements IconQueryService {
    private final IconMapper iconMapper;

    @Override
    public ApiResponse<?> queryIcons(IconDTO iconDTO) {
        try {
            IconDTO safeIconDTO = iconDTO == null ? new IconDTO() : iconDTO;
            List<IconPO> iconPOS = iconMapper.selectIcons(safeIconDTO);
            if (iconPOS == null) {
                return ApiResponse.queryFail();
            }
            List<IconVO> iconVOS = iconPOS.stream().map(PoConvertToVo::convertToIconVO).toList();
            return ApiResponse.success(iconVOS);
        } catch (RuntimeException e) {
            log.error("查询图标失败", e);
            return ApiResponse.queryFail();
        }
    }
}
