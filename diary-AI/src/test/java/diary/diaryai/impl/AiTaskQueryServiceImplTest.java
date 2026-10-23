package diary.diaryai.impl;

import diary.common.exception.ParamIllegalException;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiSentimentTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskStatusVo;
import diary.diaryai.entity.ai.vo.AiTaskSummaryVo;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AiTaskQueryServiceImplTest {

    private static final Long USER_ID = 10001L;
    private static final Long TASK_ID = 20001L;

    @Mock
    private DiaryAiMapper diaryAiMapper;
    @InjectMocks
    private AiTaskQueryServiceImpl service;

    @Test
    void shouldQueryTaskSummaryListWithoutResultTypeDependency() {
        List<AiTaskSummaryVo> expected = List.of(AiTaskSummaryVo.builder()
                .taskId(TASK_ID)
                .taskType(AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name())
                .status("SUCCESS")
                .build());
        when(diaryAiMapper.selectAiTaskSummaryList(USER_ID, "SUCCESS", 2, "纪念日", 50, 100))
                .thenReturn(expected);

        List<AiTaskSummaryVo> actual = service.getAiTaskSummaryList(
                USER_ID, "SUCCESS", 2, "纪念日", 50, 2);

        assertEquals(expected, actual);
        verify(diaryAiMapper).selectAiTaskSummaryList(USER_ID, "SUCCESS", 2, "纪念日", 50, 100);
    }

    @Test
    void shouldRejectInvalidTaskSummaryPagination() {
        assertThrows(ParamIllegalException.class,
                () -> service.getAiTaskSummaryList(USER_ID, null, null, null, 101, 0));
        assertThrows(ParamIllegalException.class,
                () -> service.getAiTaskSummaryList(USER_ID, null, null, null, 50, -1));
        assertThrows(ParamIllegalException.class,
                () -> service.getAiTaskSummaryList(USER_ID, null, null, null, 100, Integer.MAX_VALUE));
    }

    @Test
    void shouldReturnStatusForRecipeNutrientTask() {
        AiTaskPO task = AiTaskPO.builder()
                .id(TASK_ID)
                .taskType(AiTaskTypeEnum.RECIPE_NUTRIENT_ANALYSIS.name())
                .status("RUNNING")
                .build();
        when(diaryAiMapper.selectAiTaskByTaskIdAndUserId(TASK_ID, USER_ID)).thenReturn(task);

        AiTaskStatusVo result = service.getNutrientTaskStatus(TASK_ID, USER_ID);

        assertEquals(TASK_ID, result.getTaskId());
        assertEquals("RUNNING", result.getStatus());
    }

    @Test
    void shouldRejectLoveRecordSentimentTaskFromNutrientQuery() {
        AiTaskPO task = AiTaskPO.builder()
                .id(TASK_ID)
                .taskType(AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name())
                .build();
        when(diaryAiMapper.selectAiTaskByTaskIdAndUserId(TASK_ID, USER_ID)).thenReturn(task);

        assertThrows(ParamIllegalException.class,
                () -> service.getNutrientTaskStatus(TASK_ID, USER_ID));
    }

    @Test
    void shouldReturnStatusForLoveRecordSentimentTask() {
        AiTaskPO task = AiTaskPO.builder()
                .id(TASK_ID)
                .taskType(AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name())
                .status("RUNNING")
                .build();
        when(diaryAiMapper.selectAiTaskByTaskIdAndUserId(TASK_ID, USER_ID)).thenReturn(task);

        AiTaskStatusVo result = service.getLoveRecordSentimentTaskStatus(TASK_ID, USER_ID);

        assertEquals(TASK_ID, result.getTaskId());
        assertEquals("RUNNING", result.getStatus());
    }

    @Test
    void shouldRejectNutrientTaskFromLoveRecordSentimentQuery() {
        AiTaskPO task = AiTaskPO.builder()
                .id(TASK_ID)
                .taskType(AiTaskTypeEnum.RECIPE_NUTRIENT_ANALYSIS.name())
                .build();
        when(diaryAiMapper.selectAiTaskByTaskIdAndUserId(TASK_ID, USER_ID)).thenReturn(task);

        assertThrows(ParamIllegalException.class,
                () -> service.getLoveRecordSentimentTaskStatus(TASK_ID, USER_ID));
    }

    @Test
    void shouldReturnPendingLoveRecordSentimentTaskWithoutResultFields() {
        AiTaskPO task = AiTaskPO.builder()
                .id(TASK_ID)
                .taskType(AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name())
                .build();
        AiSentimentTaskResultVo result = AiSentimentTaskResultVo.builder()
                .taskId(TASK_ID)
                .status("PENDING")
                .build();
        when(diaryAiMapper.selectAiTaskByTaskIdAndUserId(TASK_ID, USER_ID)).thenReturn(task);
        when(diaryAiMapper.selectLoveRecordSentimentTaskResult(TASK_ID, USER_ID)).thenReturn(result);

        AiSentimentTaskResultVo actual = service.getLoveRecordSentimentTaskResult(TASK_ID, USER_ID);

        assertEquals(result, actual);
    }
}
