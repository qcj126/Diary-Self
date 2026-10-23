package diary.diaryai.controller;

import diary.common.auth.CurrentUser;
import diary.common.result.ApiResponse;
import diary.diaryai.entity.ai.vo.AiSentimentTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskStatusVo;
import diary.diaryai.entity.ai.vo.AiTaskSummaryVo;
import diary.diaryai.service.AiTaskNutrientApplicationService;
import diary.diaryai.service.AiTaskQueryService;
import diary.diaryai.service.AiTaskSentimentApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryAIControllerTest {

    private static final Long USER_ID = 10001L;
    private static final Long TASK_ID = 20001L;

    @Mock
    private AiTaskNutrientApplicationService aiTaskNutrientApplicationService;
    @Mock
    private AiTaskSentimentApplicationService aiTaskSentimentApplicationService;
    @Mock
    private AiTaskQueryService aiTaskQueryService;
    @Mock
    private CurrentUser currentUser;
    @InjectMocks
    private DiaryAIController controller;

    @Test
    void shouldReturnTaskSummaryList() {
        List<AiTaskSummaryVo> summaries = List.of(AiTaskSummaryVo.builder()
                .taskId(TASK_ID)
                .taskType("LOVE_RECORD_SENTIMENT_ANALYSIS")
                .status("SUCCESS")
                .build());
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getAiTaskSummaryList(USER_ID, "SUCCESS", 2, "纪念日", 20, 1))
                .thenReturn(summaries);

        ApiResponse<List<AiTaskSummaryVo>> response = controller.list("SUCCESS", 2, "纪念日", 20, 1);

        assertEquals(summaries, response.getData());
        verify(aiTaskQueryService).getAiTaskSummaryList(USER_ID, "SUCCESS", 2, "纪念日", 20, 1);
    }

    @Test
    void shouldReturnNutrientTaskStatus() {
        AiTaskStatusVo status = AiTaskStatusVo.builder()
                .taskId(TASK_ID)
                .status("RUNNING")
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getNutrientTaskStatus(TASK_ID, USER_ID)).thenReturn(status);

        ResponseEntity<ApiResponse<AiTaskStatusVo>> response = controller.status(TASK_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(status, response.getBody().getData());
        verify(aiTaskQueryService).getNutrientTaskStatus(TASK_ID, USER_ID);
    }

    @Test
    void shouldReturnOkWhenNutrientTaskIsTerminal() {
        AiTaskResultVo result = AiTaskResultVo.builder()
                .taskId(TASK_ID)
                .status("SUCCESS")
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getNutrientTaskResult(TASK_ID, USER_ID)).thenReturn(result);

        ResponseEntity<ApiResponse<AiTaskResultVo>> response = controller.result(TASK_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody().getData());
    }

    @Test
    void shouldReturnLoveRecordSentimentTaskStatus() {
        AiTaskStatusVo status = AiTaskStatusVo.builder()
                .taskId(TASK_ID)
                .status("RUNNING")
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getLoveRecordSentimentTaskStatus(TASK_ID, USER_ID)).thenReturn(status);

        ResponseEntity<ApiResponse<AiTaskStatusVo>> response = controller.loveRecordSentimentStatus(TASK_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(status, response.getBody().getData());
        verify(aiTaskQueryService).getLoveRecordSentimentTaskStatus(TASK_ID, USER_ID);
    }

    @Test
    void shouldReturnOkWhenLoveRecordSentimentTaskIsTerminal() {
        AiSentimentTaskResultVo result = AiSentimentTaskResultVo.builder()
                .taskId(TASK_ID)
                .status("SUCCESS")
                .sentimentLabel("POSITIVE")
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getLoveRecordSentimentTaskResult(TASK_ID, USER_ID)).thenReturn(result);

        ResponseEntity<ApiResponse<AiSentimentTaskResultVo>> response = controller.loveRecordSentimentResult(TASK_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody().getData());
    }

    @Test
    void shouldReturnAcceptedWhenLoveRecordSentimentTaskIsNotTerminal() {
        AiSentimentTaskResultVo result = AiSentimentTaskResultVo.builder()
                .taskId(TASK_ID)
                .status("RUNNING")
                .build();
        when(currentUser.getUser()).thenReturn(USER_ID);
        when(aiTaskQueryService.getLoveRecordSentimentTaskResult(TASK_ID, USER_ID)).thenReturn(result);

        ResponseEntity<ApiResponse<AiSentimentTaskResultVo>> response = controller.loveRecordSentimentResult(TASK_ID);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(result, response.getBody().getData());
    }
}
