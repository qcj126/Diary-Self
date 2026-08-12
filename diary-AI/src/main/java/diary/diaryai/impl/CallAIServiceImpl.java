package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.service.CallAIService;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.file.service.downloadservice.DownloadService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallAIServiceImpl implements CallAIService {

    @Override
    public void callAI(AiInvokeDTO aiInvokeDTO) {

    }
}