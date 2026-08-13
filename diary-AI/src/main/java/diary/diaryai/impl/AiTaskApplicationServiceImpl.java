package diary.diaryai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTaskMessageProducer;
import diary.utils.commonutil.MyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskApplicationServiceImpl implements AiTaskApplicationService {
    private final AiTaskMessageProducer rocketMqHandlerService;
    private final DiaryAiMapper diaryAiMapper;
    private final ObjectMapper objectMapper;
    @Override
    public AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO) {
        // 校验参数、根据clientRequestId 做幂等判断
        MyUtils.check()
                .notNull(aiInvokeDTO, "aiInvokeDTO")
                .notNull(aiInvokeDTO.getClientRequestId(), "clientRequestId")
                .notEmpty(aiInvokeDTO.getClientRequestId(), "clientRequestId")
                .notNull(aiInvokeDTO.getAiType(), "aiType")
                .notNull(aiInvokeDTO.getAiApplication(), "aiApplication")
                .notNull(aiInvokeDTO.getFlag(), "flag")
                .notEmpty(aiInvokeDTO.getFlag(), "flag")
                .notNull(aiInvokeDTO.getMaterials(), "materials")
                .stringKeyMapNotContainsEmpty(aiInvokeDTO.getMaterials(), "materials")
                .notNull(aiInvokeDTO.getUniversalId(), "universalId");

        Long userId = 10000L;
        Long taskId = MyUtils.getPrimaryKey();
        try {
            String inputSnapshot = objectMapper.writeValueAsString(aiInvokeDTO);
            // 根据taskId创建任务
            AiTaskPO aiTaskPO = AiTaskPO.builder()
                    .id(taskId)
                    .userId(userId)
                    .clientRequestId(aiInvokeDTO.getClientRequestId())
                    .taskType("QWEN_PLUS_NUTRIENT")
                    .status("PENDING")
                    .inputSnapshot(inputSnapshot)
                    .attemptCount(0)
                    .maxAttempts(3)
                    .workerId(null)
                    .leaseUntil(null)
                    .aiInfoId(null)
                    .errorCode(null)
                    .errorMessage(null)
                    .createTime(LocalDateTime.now())
                    .queueTime(null)
                    .startTime(null)
                    .finishTime(null)
                    .versionId(0)
                    .build();
            // 保存任务
            diaryAiMapper.insertAiTask(aiTaskPO);
            // 调用RocketMQ发送任务
            AiTaskMessageDto aiTaskMessageDto = AiTaskMessageDto.builder()
                    .eventId("evt-" + MyUtils.getPrimaryKey())
                    .taskId(taskId)
                    .userId(10000L)
                    .clientRequestId(aiInvokeDTO.getClientRequestId())
                    .taskType("QWEN_PLUS_NUTRIENT")
                    .schemaVersion(1)
                    .occurTime(LocalDateTime.now())
                    .traceId(MDC.get("traceId"))
                    .build();
            SendReceipt send = rocketMqHandlerService.send(aiTaskMessageDto);
            MessageId messageId = send.getMessageId();
            log.info("Message sent, messageId: {}", messageId);
            // 构建条件请求体
            AiTaskProcessDto aiTaskProcessDto = AiTaskProcessDto.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .clientRequestId(aiInvokeDTO.getClientRequestId())
                    .status("QUEUED")
                    .build();
            diaryAiMapper.updateAiTaskStatus(aiTaskProcessDto);
            return AiTaskSubmitVo.builder()
                    .status("QUEUED")
                    .taskId(taskId)
                    .message("AI分析任务正在处理中")
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            AiTaskProcessDto aiTaskProcessDto = AiTaskProcessDto.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .clientRequestId(aiInvokeDTO.getClientRequestId())
                    .status("PENDING")
                    .build();
            diaryAiMapper.updateAiTaskStatus(aiTaskProcessDto);
            throw new RuntimeException(e);
        }
    }
}
