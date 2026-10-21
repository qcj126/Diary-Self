package diary.file.impl.sys.sysdeleteserviceimpl;

import diary.common.exception.NullResultException;
import diary.common.result.ApiResponse;

import diary.file.entity.dto.IconDTO;
import diary.file.entity.po.IconPO;
import diary.file.mapper.IconMapper;
import diary.file.service.sys.sysdeleteservice.IconDeleteService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconDeleteServiceImpl implements IconDeleteService {
    private final IconMapper iconMapper;

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
                boolean deleted = deleteFile(iconPath);
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

    private boolean deleteFile(String iconPath) {
        try {
            File file = new File(iconPath);
            // 检查文件是否存在
            if (!file.exists()) {
                log.warn("文件不存在，路径: {}", iconPath);
                return false;
            }
            // 删除文件
            boolean deleted = file.delete();
            if (deleted) {
                log.info("文件删除成功: {}", iconPath);
            } else {
                log.error("文件删除失败: {}", iconPath);
            }
            return deleted;
        } catch (SecurityException e) {
            log.error("文件删除权限异常，路径: {}, 错误: {}", iconPath, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("文件删除异常，路径: {}, 错误: {}", iconPath, e.getMessage());
            return false;
        }
    }
}
