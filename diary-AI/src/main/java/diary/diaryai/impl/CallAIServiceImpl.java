package diary.diaryai.impl;

import diary.diaryai.factory.AIFactory;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.service.CallAIService;
import diary.diaryai.strategy.service.InvokeAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallAIServiceImpl implements CallAIService {
    private final DiaryAIMapper diaryAIMapper;

    private final AIFactory aiFactory;
    @Override
    public Object callAI(Integer code, Integer id, Integer type) {
        Object data = diaryAIMapper.getData();
        // 对data进行过滤处理，先获取工厂的AI实现类，然后调用AI
        InvokeAIService aiService = aiFactory.getAIService(code);
        return aiService.invokeAI(data);
    }
}