package diary.file.impl;

import diary.common.entity.file.dto.IconAddDTO;
import diary.common.entity.file.dto.IconDeleteDTO;
import diary.common.entity.file.dto.IconQueryDTO;
import diary.common.entity.file.dto.IconUpdateDTO;
import diary.common.entity.file.po.IconPO;
import diary.common.entity.file.vo.IconVO;
import diary.file.mapper.IconMapper;
import diary.file.service.IconService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IconServiceImpl implements IconService {
    private static final Path ICON_DIR = Paths.get("E:\\Diary-Front\\icon").toAbsolutePath().normalize();

    private final IconMapper iconMapper;

    @Override
    public IconVO addIcon(MultipartFile file, IconAddDTO iconAddDTO) {
        MyUtils.check()
                .notNull(file, "icon file")
                .notNull(iconAddDTO, "icon info")
                .notEmpty(iconAddDTO.getIconName(), "icon name")
                .notNull(iconAddDTO.getUserId(), "user id");

        if (MyUtils.isFileEmpty(file)) {
            throw new IllegalArgumentException("icon file cannot be empty");
        }

        Integer iconType = iconAddDTO.getIconType() == null ? getIconType(file) : iconAddDTO.getIconType();
        Integer iconSize = toIntFileSize(file.getSize());
        Integer iconPixel = resolveIconPixel(file, iconAddDTO.getIconPixel());
        SavedIconFile savedIconFile = saveIconFile(file);
        IconPO iconPO = new IconPO();
        iconPO.setIconName(iconAddDTO.getIconName());
        iconPO.setIconType(iconType);
        iconPO.setIconPath(savedIconFile.path());
        iconPO.setIconSize(iconSize);
        iconPO.setIconPixel(iconPixel);
        iconPO.setUserId(iconAddDTO.getUserId());

        try {
            Integer count = iconMapper.insertIcon(iconPO);
            if (count == null || count <= 0) {
                deleteFileQuietly(savedIconFile.path());
                return null;
            }
        } catch (RuntimeException e) {
            deleteFileQuietly(savedIconFile.path());
            throw e;
        }
        return toVO(iconPO);
    }

    @Override
    public List<IconVO> queryIcons(IconQueryDTO queryDTO) {
        IconQueryDTO safeQueryDTO = queryDTO == null ? new IconQueryDTO() : queryDTO;
        List<IconPO> iconPOS = iconMapper.selectIcons(safeQueryDTO);
        if (iconPOS == null) {
            return null;
        }
        return iconPOS.stream().map(this::toVO).toList();
    }

    @Override
    public Boolean updateIcon(MultipartFile file, IconUpdateDTO iconUpdateDTO) {
        MyUtils.check()
                .notNull(iconUpdateDTO, "icon info")
                .notNull(iconUpdateDTO.getId(), "icon id");

        boolean hasFile = !MyUtils.isFileEmpty(file);
        boolean hasUpdateField = !MyUtils.isEmpty(iconUpdateDTO.getIconName())
                || iconUpdateDTO.getIconType() != null
                || iconUpdateDTO.getIconPixel() != null
                || hasFile;
        if (!hasUpdateField) {
            throw new IllegalArgumentException("no icon fields to update");
        }

        IconPO oldIcon = iconMapper.selectIconById(iconUpdateDTO.getId());
        if (oldIcon == null) {
            return false;
        }

        SavedIconFile savedIconFile = null;
        IconPO iconPO = new IconPO();
        iconPO.setId(iconUpdateDTO.getId());
        iconPO.setUserId(iconUpdateDTO.getUserId());
        iconPO.setIconName(iconUpdateDTO.getIconName());
        iconPO.setIconType(iconUpdateDTO.getIconType());
        iconPO.setIconPixel(iconUpdateDTO.getIconPixel());

        if (hasFile) {
            Integer iconType = iconUpdateDTO.getIconType() == null ? getIconType(file) : iconUpdateDTO.getIconType();
            Integer iconSize = toIntFileSize(file.getSize());
            Integer iconPixel = resolveIconPixel(file, iconUpdateDTO.getIconPixel());
            savedIconFile = saveIconFile(file);
            iconPO.setIconPath(savedIconFile.path());
            iconPO.setIconSize(iconSize);
            iconPO.setIconType(iconType);
            iconPO.setIconPixel(iconPixel);
        }

        try {
            Integer count = iconMapper.updateIcon(iconPO);
            if (count == null || count <= 0) {
                if (savedIconFile != null) {
                    deleteFileQuietly(savedIconFile.path());
                }
                return false;
            }
        } catch (RuntimeException e) {
            if (savedIconFile != null) {
                deleteFileQuietly(savedIconFile.path());
            }
            throw e;
        }

        if (savedIconFile != null) {
            deleteFileQuietly(oldIcon.getIconPath());
        }
        return true;
    }

    @Override
    public Boolean deleteIcon(IconDeleteDTO deleteDTO) {
        MyUtils.check()
                .notNull(deleteDTO, "delete params")
                .notNull(deleteDTO.getId(), "icon id");

        IconPO oldIcon = iconMapper.selectIconById(deleteDTO.getId());
        if (oldIcon == null) {
            return false;
        }

        Integer count = iconMapper.deleteIcon(deleteDTO.getId(), deleteDTO.getUserId());
        if (count == null || count <= 0) {
            return false;
        }
        deleteFileQuietly(oldIcon.getIconPath());
        return true;
    }

    private SavedIconFile saveIconFile(MultipartFile file) {
        try {
            Files.createDirectories(ICON_DIR);
            String filename = buildStorageFilename(file.getOriginalFilename());
            Path targetPath = ICON_DIR.resolve(filename).normalize();
            if (!targetPath.startsWith(ICON_DIR)) {
                throw new IllegalArgumentException("illegal icon filename");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SavedIconFile(targetPath.toString());
        } catch (IOException e) {
            throw new RuntimeException("save icon file failed", e);
        }
    }

    private String buildStorageFilename(String originalFilename) {
        String filename = MyUtils.isEmpty(originalFilename) ? "icon" : Paths.get(originalFilename).getFileName().toString();
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = "";
        int dotIndex = sanitizedFilename.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = sanitizedFilename.substring(dotIndex);
            sanitizedFilename = sanitizedFilename.substring(0, dotIndex);
        }
        if (MyUtils.isEmpty(sanitizedFilename)) {
            sanitizedFilename = "icon";
        }
        return sanitizedFilename + "_" + UUID.randomUUID() + extension;
    }

    private Integer resolveIconPixel(MultipartFile file, Integer iconPixel) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                return Math.max(image.getWidth(), image.getHeight());
            }
        } catch (IOException e) {
            log.warn("Read icon pixel failed: {}", file.getOriginalFilename(), e);
        }
        MyUtils.check().notNull(iconPixel, "icon pixel");
        return iconPixel;
    }

    private Integer getIconType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String lowerFilename = filename == null ? "" : filename.toLowerCase();
        String contentType = file.getContentType();
        if (lowerFilename.endsWith(".svg") || "image/svg+xml".equalsIgnoreCase(contentType)) {
            return 2;
        }
        if (lowerFilename.endsWith(".png") || "image/png".equalsIgnoreCase(contentType)) {
            return 1;
        }
        return 3;
    }

    private Integer toIntFileSize(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("icon file is too large");
        }
        return (int) size;
    }

    private IconVO toVO(IconPO iconPO) {
        IconVO iconVO = new IconVO();
        iconVO.setId(iconPO.getId());
        iconVO.setIconName(iconPO.getIconName());
        iconVO.setIconType(iconPO.getIconType());
        iconVO.setIconPath(iconPO.getIconPath());
        iconVO.setIconSize(iconPO.getIconSize());
        iconVO.setIconPixel(iconPO.getIconPixel());
        iconVO.setUserId(iconPO.getUserId());
        iconVO.setCreateTime(iconPO.getCreateTime());
        iconVO.setUpdateTime(iconPO.getUpdateTime());
        return iconVO;
    }

    private void deleteFileQuietly(String iconPath) {
        if (MyUtils.isEmpty(iconPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(iconPath));
        } catch (IOException e) {
            log.warn("Delete icon file failed: {}", iconPath, e);
        }
    }

    private record SavedIconFile(String path) {
    }
}
