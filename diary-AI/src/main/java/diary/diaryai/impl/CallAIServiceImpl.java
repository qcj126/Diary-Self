package diary.diaryai.impl;

import diary.common.entity.ai.dto.AIInvokeDTO;
import diary.common.exception.ParamIllegalException;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.service.CallAIService;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.file.service.downloadservice.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CallAIServiceImpl implements CallAIService {
    private final DiaryAIMapper diaryAIMapper;
    private final DownloadService downloadService;
    private final AIFactory aiFactory;
    @Override
    public void callAI(AIInvokeDTO aiInvokeDTO) throws FileNotFoundException {
        if (aiInvokeDTO.getCode() == null || aiInvokeDTO.getImageIdUrls() == null || aiInvokeDTO.getImageIdUrls().isEmpty()) throw new ParamIllegalException("参数不能为空");

        Map<String, Object> batchDownloadImagesMap = downloadService.batchDownloadPhotos(aiInvokeDTO.getImageIdUrls());
        Map<Long, String> filePaths = (Map<Long, String>) batchDownloadImagesMap.get("successFiles");
        Map<Long, InputStream> data = new HashMap<>();
        for (Map.Entry<Long, String> entry : filePaths.entrySet()) {
            data.put(entry.getKey(), new FileInputStream(entry.getValue()));
        }
        // 对data进行过滤处理，先获取工厂的AI实现类，然后调用AI
        InvokeAIService aiService = aiFactory.getAIService(aiInvokeDTO.getCode());
        aiService.invokeAI(data);
    }
}