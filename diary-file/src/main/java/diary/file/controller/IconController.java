package diary.file.controller;

import diary.common.entity.file.dto.IconAddDTO;
import diary.common.entity.file.dto.IconDeleteDTO;
import diary.common.entity.file.dto.IconQueryDTO;
import diary.common.entity.file.dto.IconUpdateDTO;
import diary.common.result.ApiResponse;
import diary.file.service.IconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/icon")
@RequiredArgsConstructor
public class IconController {
    private final IconService iconService;

    @PostMapping("/add")
    public ApiResponse<?> addIcon(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute IconAddDTO iconAddDTO) {
        try {
            Object result = iconService.addIcon(file, iconAddDTO);
            if (result == null) {
                return ApiResponse.addFail();
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Add icon failed", e);
            return ApiResponse.addFail();
        }
    }

    @PostMapping("/query")
    public ApiResponse<?> queryIcons(@RequestBody(required = false) IconQueryDTO iconQueryDTO) {
        try {
            Object result = iconService.queryIcons(iconQueryDTO);
            if (result == null) {
                return ApiResponse.queryFail();
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Query icons failed", e);
            return ApiResponse.queryFail();
        }
    }

    @PostMapping("/update")
    public ApiResponse<?> updateIcon(@RequestParam(value = "file", required = false) MultipartFile file,
                                     @ModelAttribute IconUpdateDTO iconUpdateDTO) {
        try {
            Boolean result = iconService.updateIcon(file, iconUpdateDTO);
            if (!Boolean.TRUE.equals(result)) {
                return ApiResponse.updateFail();
            }
            return ApiResponse.success("update success");
        } catch (Exception e) {
            log.error("Update icon failed", e);
            return ApiResponse.updateFail();
        }
    }

    @PostMapping("/delete")
    public ApiResponse<?> deleteIcon(@RequestBody IconDeleteDTO iconDeleteDTO) {
        try {
            Boolean result = iconService.deleteIcon(iconDeleteDTO);
            if (!Boolean.TRUE.equals(result)) {
                return ApiResponse.delFail();
            }
            return ApiResponse.success("delete success");
        } catch (Exception e) {
            log.error("Delete icon failed", e);
            return ApiResponse.delFail();
        }
    }
}
