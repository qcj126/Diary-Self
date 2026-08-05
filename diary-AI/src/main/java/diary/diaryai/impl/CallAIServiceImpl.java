package diary.diaryai.impl;

import com.aliyun.core.utils.IOUtils;
import diary.common.entity.ai.dto.AIInvokeDTO;
import diary.common.exception.ParamIllegalException;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.service.CallAIService;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.file.service.downloadservice.DownloadService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CallAIServiceImpl implements CallAIService {
    private final DownloadService downloadService;
    private final AIFactory aiFactory;
    @Override
    public void callAI(AIInvokeDTO aiInvokeDTO) {
        MyUtils.check()
                .notNull(aiInvokeDTO.getAiType(), "AI类型")
                .notNull(aiInvokeDTO.getAiApplication(), "AI应用场景")
                .notNull(aiInvokeDTO.getFlag(), "AI调用标识")
                .notNull(aiInvokeDTO.getMaterials(), "食材列表")
                .listNotContainsEmpty(aiInvokeDTO.getMaterials(), "食材列表");
        // 对data进行过滤处理，先获取工厂的AI实现类，然后调用AI
        InvokeAIService aiService = aiFactory.getAIService(aiInvokeDTO.getAiType());
        aiService.getAiResultAndSave(aiInvokeDTO.getMaterials(), aiInvokeDTO.getAiApplication(), aiInvokeDTO.getAiType(), aiInvokeDTO.getFlag());
    }
}