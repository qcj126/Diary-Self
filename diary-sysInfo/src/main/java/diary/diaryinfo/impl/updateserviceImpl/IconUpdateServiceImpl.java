package diary.diaryinfo.impl.updateserviceImpl;

import diary.common.convert.file.DtoConvertToPo;
import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.result.ApiResponse;

import diary.diaryinfo.mapper.IconMapper;
import diary.diaryinfo.service.IconUpdateService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconUpdateServiceImpl implements IconUpdateService {
    private final IconMapper iconMapper;

    @Override
    public ApiResponse<?> updateIcon(MultipartFile file, IconDTO iconDTO) {
        try {
            MyUtils.check()
                    .notNull(iconDTO, "图标信息")
                    .notNull(iconDTO.getId(), "图标ID");

            boolean hasFile = !MyUtils.isFileEmpty(file);
            boolean hasUpdateField = !MyUtils.isEmpty(iconDTO.getIconName())
                    || iconDTO.getIconType() != null
                    || iconDTO.getIconPixel() != null
                    || hasFile;
            if (!hasUpdateField) {
                throw new IllegalArgumentException("没有需要修改的图标信息");
            }

            IconPO oldIcon = iconMapper.selectIconById(iconDTO.getId());
            if (oldIcon == null) {
                return ApiResponse.updateFail();
            }

            IconPO iconPO = DtoConvertToPo.iconDtoConvertToPO(iconDTO);
//            if (hasFile) {
//                Integer iconType = iconDTO.getIconType() == null ? iconFileSupport.getIconType(file) : iconDTO.getIconType();
//                Integer iconSize = iconFileSupport.toIntFileSize(file.getSize());
//                Integer iconPixel = iconFileSupport.resolveIconPixel(file, iconDTO.getIconPixel());
//                savedIconFile = iconFileSupport.saveIconFile(file);
//                iconPO.setIconPath(savedIconFile.path());
//                iconPO.setIconSize(iconSize);
//                iconPO.setIconType(iconType);
//                iconPO.setIconPixel(iconPixel);
//            }

//            Integer count = iconMapper.updateIcon(iconPO);
//            if (count == null || count <= 0) {
//                if (savedIconFile != null) {
//                    iconFileSupport.deleteFileQuietly(savedIconFile.path());
//                }
//                return ApiResponse.updateFail();
//            }
//
//            if (savedIconFile != null) {
//                iconFileSupport.deleteFileQuietly(oldIcon.getIconPath());
//            }
            return ApiResponse.success("修改成功");
        } catch (RuntimeException e) {
            log.error("修改图标失败", e);
            return ApiResponse.updateFail();
        }
    }
}
