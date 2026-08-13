package diary.diaryai.repository;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

// 专门处理数据库事务的类
@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseServiceImpl {
    private final DiaryAiMapper diaryAiMapper;

    /**
     * @param taskId
     * @param model
     * @param result
     * @param temperature
     * @param userId
     * @param workerId 当前任务抢占者；用于阻止旧 Worker 提交结果
     * @param versionId Consumer 抢占成功后的乐观锁版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void processData(Long taskId, Object data, String model, Map<String, String> result, Double temperature, Long userId, String workerId, Integer versionId) {
        AiInvokeDTO aiInvokeDTO = (AiInvokeDTO) data;
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(userId)
                .temperature(String.valueOf(temperature))
                .model(model)
                .aiType(aiInvokeDTO.getAiType())
                .aiApplication(aiInvokeDTO.getAiApplication())
                .build();
        AiNutrientPO aiNutrientPO = AiNutrientPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(userId)
                .universalId(aiInvokeDTO.getUniversalId())
                .aiInfoId(aiInfoPO.getId())
                .calory(result.get("卡路里"))
                .protein(result.get("蛋白质"))
                .fat(result.get("脂肪"))
                .carbohydrate(result.get("碳水化合物"))
                .sugar(result.get("糖"))
                .sodium(result.get("钠"))
                .flag(aiInvokeDTO.getFlag())
                .aiTaskId(taskId)
                .build();
        AiTaskProcessDto aiTaskProcessDto = AiTaskProcessDto.builder()
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(aiInvokeDTO.getClientRequestId())
                .workerId(workerId)
                .versionId(versionId)
                .aiInfoId(aiInfoPO.getId())
                .finishTime(LocalDateTime.now())
                .build();
        int aiInfoCnt = diaryAiMapper.insertAiInfo(aiInfoPO);
        int aiNutrientCnt = diaryAiMapper.insertAiNutrient(aiNutrientPO);
        int aiTaskCnt = diaryAiMapper.markSuccessIfOwned(aiTaskProcessDto);

        /*
         * 以前任意一步失败后，会在同一个事务里把任务改回 PENDING 再抛异常；但抛异常会让该更新一起回滚，
         * 而且执行失败也不应回到“消息尚未发送”的 PENDING。现在三步必须都恰好影响一行，否则直接抛出，
         * 让 AiInfo、AiNutrient 和 SUCCESS 状态整体回滚，再由 Consumer 在事务外写 RETRY_WAIT/FAILED。
         * SUCCESS 更新还校验 workerId + versionId，旧 Worker 已失去租约时不会提交重复结果。
         */
        if (aiInfoCnt != 1 || aiNutrientCnt != 1 || aiTaskCnt != 1) {
            throw new IllegalStateException(
                    "AI结果事务提交失败: aiInfo=" + aiInfoCnt
                            + ", aiNutrient=" + aiNutrientCnt
                            + ", aiTask=" + aiTaskCnt
            );
        }
    }
}
