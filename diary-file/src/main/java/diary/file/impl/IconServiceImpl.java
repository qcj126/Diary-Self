package diary.file.impl;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;
import diary.file.service.IconService;
import diary.file.service.addservice.IconAddService;
import diary.file.service.deleteservice.IconDeleteService;
import diary.file.service.queryservice.IconQueryService;
import diary.file.service.updateservice.IconUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class IconServiceImpl implements IconService {
    private final IconAddService iconAddService;

    private final IconQueryService iconQueryService;

    private final IconUpdateService iconUpdateService;

    private final IconDeleteService iconDeleteService;

    @Override
    public ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO) {
        return iconAddService.addIcon(file, iconDTO);
    }

    @Override
    public ApiResponse<?> queryIcons(IconDTO iconDTO) {
        return iconQueryService.queryIcons(iconDTO);
    }

    @Override
    public ApiResponse<?> updateIcon(MultipartFile file, IconDTO iconDTO) {
        return iconUpdateService.updateIcon(file, iconDTO);
    }

    @Override
    public ApiResponse<?> deleteIcon(IconDTO iconDTO) {
        return iconDeleteService.deleteIcon(iconDTO);
    }
}
