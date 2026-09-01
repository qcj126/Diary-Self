package diary.diaryinfo.impl.addserviceImpl;

import diary.common.convert.file.DtoConvertToPo;
import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.result.ApiResponse;

import diary.diaryinfo.mapper.IconMapper;
import diary.diaryinfo.service.addservice.IconAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconAddServiceImpl implements IconAddService {
    private final IconMapper iconMapper;

    private static final Path ICON_DIR = Paths.get("E:\\Diary-Front\\icon").normalize();

    @Override
    public ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO) {
        Path savedIconPath = null;
        try {
            MyUtils.check()
                    .notNull(file, "图标文件")
                    .notNull(iconDTO, "图标信息")
                    .notEmpty(iconDTO.getIconName(), "图标名称")
                    .notEmpty(file.getContentType(), "文件类型")
                    .notNull(file.getContentType(), "文件类型")
                    .notNull(iconDTO.getUserId(), "用户ID");

            if (MyUtils.isFileEmpty(file)) {
                throw new IllegalArgumentException("图标文件不能为空");
            }
            if (!"image/png".equals(file.getContentType())) {
                throw new IllegalArgumentException("图标文件必须是png格式");
            }

            String originalFilename = file.getOriginalFilename();
            MyUtils.check().notEmpty(originalFilename, "文件名");
            String fileName = Paths.get(originalFilename).getFileName().toString();
            savedIconPath = ICON_DIR.resolve(fileName).normalize();
            if (!savedIconPath.startsWith(ICON_DIR)) {
                throw new IllegalArgumentException("文件名不合法");
            }

            Files.createDirectories(ICON_DIR);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, savedIconPath);
            }

            Integer iconType = 1;
            Long iconSize = file.getSize();
            Integer iconPixel = iconDTO.getIconPixel();
            iconDTO.setId(MyUtils.getPrimaryKey());
            iconDTO.setIconPath(savedIconPath.toString());
            iconDTO.setIconSize(Integer.parseInt(String.valueOf(iconSize)));
            iconDTO.setIconPixel(iconPixel);
            iconDTO.setIconType(iconType);
            IconPO iconPO = DtoConvertToPo.iconDtoConvertToPO(iconDTO);
            Integer count = iconMapper.insertIcon(iconPO);
            if (count <= 0) {
                Files.deleteIfExists(savedIconPath);
                return ApiResponse.addFail();
            }
            return ApiResponse.success("图标添加成功");
        } catch (IOException e) {
            log.error("保存图标文件失败，请检查文件后缀，文件名称", e);
            return ApiResponse.addFail();
        }
    }
}
