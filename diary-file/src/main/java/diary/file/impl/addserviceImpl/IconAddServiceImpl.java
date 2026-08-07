package diary.file.impl.addserviceImpl;

import diary.common.convert.file.DtoConvertToPo;
import diary.common.convert.file.PoConvertToVo;
import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.result.ApiResponse;
import diary.file.impl.IconFileSupport;
import diary.file.mapper.IconMapper;
import diary.file.service.addservice.IconAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconAddServiceImpl implements IconAddService {
    private final IconMapper iconMapper;

    private final IconFileSupport iconFileSupport;

    @Override
    public ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO) {
        IconFileSupport.SavedIconFile savedIconFile = null;
        try {
            MyUtils.check()
                    .notNull(file, "图标文件")
                    .notNull(iconDTO, "图标信息")
                    .notEmpty(iconDTO.getIconName(), "图标名称")
                    .notNull(iconDTO.getUserId(), "用户ID");

            if (MyUtils.isFileEmpty(file)) {
                throw new IllegalArgumentException("图标文件不能为空");
            }

            Integer iconType = iconDTO.getIconType() == null ? iconFileSupport.getIconType(file) : iconDTO.getIconType();
            Integer iconSize = iconFileSupport.toIntFileSize(file.getSize());
            Integer iconPixel = iconFileSupport.resolveIconPixel(file, iconDTO.getIconPixel());
            savedIconFile = iconFileSupport.saveIconFile(file);
            iconDTO.setIconPath(savedIconFile.path());
            iconDTO.setIconSize(iconSize);
            iconDTO.setIconPixel(iconPixel);
            iconDTO.setIconType(iconType);
            IconPO iconPO = DtoConvertToPo.iconDtoConvertToPO(iconDTO);
            Integer count = iconMapper.insertIcon(iconPO);
            if (count == null || count <= 0) {
                iconFileSupport.deleteFileQuietly(savedIconFile.path());
                return ApiResponse.addFail();
            }
            return ApiResponse.success(PoConvertToVo.convertToIconVO(iconPO));
        } catch (RuntimeException e) {
            iconFileSupport.deleteFileQuietly(savedIconFile == null ? null : savedIconFile.path());
            log.error("添加图标失败", e);
            return ApiResponse.addFail();
        }
    }
}
