package diary.file.controller;


import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.file.entity.dto.IconDTO;
import diary.file.service.sys.sysaddservice.IconAddService;
import diary.file.service.sys.sysdeleteservice.IconDeleteService;
import diary.file.service.sys.sysqueryservice.IconQueryService;
import diary.file.service.sys.sysupdateservice.IconUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/file/sys")
@RequiredArgsConstructor
public class SysFileController {
    private IconAddService iconAddService;
    private IconQueryService iconQueryService;
    private IconDeleteService iconDeleteService;
    private IconUpdateService iconUpdateService;


    @OperLog(module = "系统数据", description = "上传系统图标", operationType = "UPLOAD", saveParams = false, saveResult = true)
    @PostMapping("/icon/add")
    public ApiResponse<?> addIcon(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute IconDTO iconDTO) {
        return iconAddService.addIcon(file, iconDTO);
    }

    @OperLog(module = "系统数据", description = "查询系统图标", operationType = "SELECT", saveResult = true)
    @PostMapping("/icon/query")
    public ApiResponse<?> queryIcons(@RequestBody(required = false) IconDTO iconDTO) {
        return iconQueryService.queryIcons(iconDTO);
    }

    @OperLog(module = "系统数据", description = "修改系统图标", operationType = "UPDATE", saveParams = false, saveResult = true)
    @PostMapping("/icon/update")
    public ApiResponse<?> updateIcon(@RequestParam(value = "file", required = false) MultipartFile file,
                                     @ModelAttribute IconDTO iconDTO) {
        return iconUpdateService.updateIcon(file, iconDTO);
    }

    @OperLog(module = "系统数据", description = "删除系统图标", operationType = "DELETE", saveResult = true)
    @PostMapping("/icon/delete")
    public ApiResponse<?> deleteIcon(@RequestBody IconDTO iconDTO) {
        return iconDeleteService.deleteIcon(iconDTO);
    }

    // 前端发起请求，获取静态图片
    @OperLog(module = "系统数据", description = "获取系统图标文件", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping(value = "/icon/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getIcon(@PathVariable String fileName) throws IOException {
        return iconQueryService.getIcon(fileName);
    }
}
