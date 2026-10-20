package diary.diaryinfo.impl.queryserviceImpl;

import diary.diaryinfo.entity.convert.DtoConvertToPo;
import diary.diaryinfo.entity.convert.DtoConvertToVo;
import diary.diaryinfo.entity.convert.PoConvertToVo;
import diary.diaryinfo.entity.dto.IngredientIconDto;
import diary.diaryinfo.entity.dto.IngredientReqDto;
import diary.diaryinfo.entity.po.CookWayPo;
import diary.diaryinfo.entity.po.IngredientPo;
import diary.diaryinfo.entity.vo.CookWayVo;
import diary.diaryinfo.entity.vo.IngredientCategoryVo;
import diary.diaryinfo.entity.vo.IngredientVo;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.SysInfoMapper;
import diary.diaryinfo.service.queryservice.SysInfoQueryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysInfoQueryServiceImpl implements SysInfoQueryService {
    private final SysInfoMapper sysInfoMapper;

    @Override
    public ApiResponse<List<IngredientCategoryVo>> getIngredientCategories(Integer isMain) {
        MyUtils.check().notNull(isMain, "isMain");

        List<IngredientPo> ingredientPos = emptyIfNull(
                sysInfoMapper.selectIngredientCategories(isMain));
        List<IngredientCategoryVo> result = ingredientPos.stream()
                .map(PoConvertToVo::convertToIngredientCategoryVo)
                .collect(Collectors.toCollection(ArrayList::new));

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<List<IngredientVo>> getIngredientsByCategory(IngredientReqDto ingredientReqDto) {
        MyUtils.check()
                .notNull(ingredientReqDto, "食材查询参数")
                .notEmpty(ingredientReqDto.getCategory(), "category")
                .notNull(ingredientReqDto.getIsMain(), "isMain");

        IngredientPo condition = DtoConvertToPo.convertToIngredientPo(ingredientReqDto);
        List<IngredientIconDto> ingredientIconDtos = sysInfoMapper.selectIngredientsByCategory(condition);
        List<IngredientVo> result = ingredientIconDtos.stream()
                .map(DtoConvertToVo::convertToVo)
                .collect(Collectors.toCollection(ArrayList::new));
        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<List<CookWayVo>> getCookWays() {

        List<CookWayPo> cookWayPos = emptyIfNull(sysInfoMapper.selectAllCookWays());
        List<CookWayVo> result = cookWayPos.stream()
                .map(PoConvertToVo::convertToCookWayVo)
                .collect(Collectors.toCollection(ArrayList::new));
        return ApiResponse.success(result);
    }

    private static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
