package diary.diaryinfo.impl.deleteserviceImpl;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.exception.NullResultException;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.IconMapper;
import diary.diaryinfo.service.IconDeleteService;
import diary.utils.commonutil.MyUtils;
import diary.utils.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconDeleteServiceImpl implements IconDeleteService {
    private final IconMapper iconMapper;
    private final FileUtil fileUtil;
    @Override
    public ApiResponse<?> deleteIcon(IconDTO iconDTO) {
        try {
            MyUtils.check()
                    .notNull(iconDTO, "删除参数")
                    .notNull(iconDTO.getId(), "图标ID");

            IconPO oldIcon = iconMapper.selectIconById(iconDTO.getId());
            if (oldIcon == null) {
                throw new NullResultException("未找到此图标");
            }

            // 删除文件
            String iconPath = oldIcon.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                boolean deleted = fileUtil.deleteFile(iconPath);
                if (!deleted) {
                    log.warn("文件删除失败，路径: {}", iconPath);
                    return ApiResponse.delFail();
                }
            }
            Integer count = iconMapper.deleteIcon(iconDTO.getId(), iconDTO.getUserId());
            if (count == null || count <= 0) {
                log.warn("文件删除失败，id: {}", iconDTO.getId());
                return ApiResponse.delFail();
            }
            return ApiResponse.success("删除成功");
        } catch (RuntimeException e) {
            log.error("删除图标失败", e);
            return ApiResponse.delFail();
        }
    }
}
