package diary.file.impl.deleteserviceImpl;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.result.ApiResponse;
import diary.file.impl.IconFileSupport;
import diary.file.mapper.IconMapper;
import diary.file.service.deleteservice.IconDeleteService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconDeleteServiceImpl implements IconDeleteService {
    private final IconMapper iconMapper;

    private final IconFileSupport iconFileSupport;

    @Override
    public ApiResponse<?> deleteIcon(IconDTO iconDTO) {
        try {
            MyUtils.check()
                    .notNull(iconDTO, "删除参数")
                    .notNull(iconDTO.getId(), "图标ID");

            IconPO oldIcon = iconMapper.selectIconById(iconDTO.getId());
            if (oldIcon == null) {
                return ApiResponse.delFail();
            }

            Integer count = iconMapper.deleteIcon(iconDTO.getId(), iconDTO.getUserId());
            if (count == null || count <= 0) {
                return ApiResponse.delFail();
            }
            iconFileSupport.deleteFileQuietly(oldIcon.getIconPath());
            return ApiResponse.success("删除成功");
        } catch (RuntimeException e) {
            log.error("删除图标失败", e);
            return ApiResponse.delFail();
        }
    }
}
