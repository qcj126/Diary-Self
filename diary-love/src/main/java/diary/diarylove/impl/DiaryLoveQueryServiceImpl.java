package diary.diarylove.impl;

import diary.common.convert.love.PoConvertToVo;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.po.LoveRecordPO;
import diary.common.entity.love.vo.*;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveQueryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryLoveQueryServiceImpl implements DiaryLoveQueryService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    public ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO query) {
        MyUtils.check().notNull(query, "loveCoupleDTO");
        if (query.getId() == null && query.getOwnerUserId() == null) {
            throw new IllegalArgumentException("id 和 ownerUserId 至少填写一个");
        }
        if (query.getStatus() == null) query.setStatus(1);
        LoveCouplePO po = diaryLoveMapper.selectLoveCouple(query);
        return po == null ? ApiResponse.queryFail() : ApiResponse.success(PoConvertToVo.convertToVo(po));
    }

    @Override
    public ApiResponse<List<LoveAnniversaryVO>> queryAnniversaries(Long coupleId) {
        MyUtils.check().notNull(coupleId, "coupleId");
        return ApiResponse.success(diaryLoveMapper.selectLoveAnniversariesByCoupleId(coupleId).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }

    @Override
    public ApiResponse<List<LoveLocationVO>> queryLocations(Long coupleId) {
        MyUtils.check().notNull(coupleId, "coupleId");
        return ApiResponse.success(diaryLoveMapper.selectLoveLocationsByCoupleId(coupleId).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }

    @Override
    public ApiResponse<LoveRecordVO> queryRecord(Long id) {
        MyUtils.check().notNull(id, "id");
        LoveRecordPO po = diaryLoveMapper.selectLoveRecordById(id);
        return po == null ? ApiResponse.queryFail() : ApiResponse.success(PoConvertToVo.convertToVo(po));
    }

    @Override
    public ApiResponse<List<LoveRecordVO>> queryRecords(LoveRecordDTO query) {
        MyUtils.check().notNull(query, "loveRecordDTO").notNull(query.getCoupleId(), "coupleId");

        /*
         * TODO 数据量增加后改为 record_date + id 游标分页。
         * TODO 接入 Redis 时使用 Cache Aside，列表缓存采用 couple 版本号，并给 TTL 增加随机抖动防止集中失效。
         *      首页多块数据组装后可使用有界线程池并行查询；普通列表查询无需为了“多线程”而拆分 SQL。
         */
        return ApiResponse.success(diaryLoveMapper.selectLoveRecords(query).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }

    @Override
    public ApiResponse<List<LoveRecordImageVO>> queryRecordImages(Long recordId) {
        MyUtils.check().notNull(recordId, "recordId");
        return ApiResponse.success(diaryLoveMapper.selectLoveRecordImagesByRecordId(recordId).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }
}
