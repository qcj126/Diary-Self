package diary.diaryinfo.impl.queryserviceImpl;

import diary.common.convert.file.PoConvertToVo;
import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.entity.file.vo.IconVO;
import diary.common.exception.CustomException;
import diary.common.result.ApiResponse;

import diary.diaryinfo.mapper.IconMapper;

import diary.diaryinfo.service.queryservice.IconQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconQueryServiceImpl implements IconQueryService {
    private final IconMapper iconMapper;

    private static final Path ICON_DIR = Paths.get("E:\\Diary-Front\\icon").normalize();

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

    @Override
    public ResponseEntity<byte[]> getIcon(String fileName) {
        try {
            String decodedFileName = UriUtils.decode(fileName, StandardCharsets.UTF_8);
            Path iconPath = ICON_DIR.resolve(decodedFileName).normalize();
            if (!iconPath.startsWith(ICON_DIR) || !Files.isRegularFile(iconPath)) {
                iconPath = null;
            }
            if (iconPath == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(Files.readAllBytes(iconPath));
        } catch (IOException e) {
            log.error("获取图标失败", e);
            throw new CustomException("获取图标失败");
        }
    }
}
