package diary.diaryinfo.impl;

import diary.common.consts.RedisKeyConst;
import diary.common.convert.sysInfo.DtoConvertToPo;
import diary.common.convert.sysInfo.DtoConvertToVo;
import diary.common.convert.sysInfo.PoConvertToVo;
import diary.common.entity.sysInfo.dto.IngredientIconDto;
import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.po.CookWayPo;
import diary.common.entity.sysInfo.po.IngredientPo;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.SysInfoMapper;
import diary.diaryinfo.service.SysInfoCacheService;
import diary.diaryinfo.service.SysInfoQueryService;
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
    private final SysInfoCacheService sysInfoCacheService;

    @Override
    public ApiResponse<List<IngredientCategoryVo>> getIngredientCategories() {
        String cacheKey = RedisKeyConst.SYS_INFO_INGREDIENT_CATEGORY_KEY;
        List<IngredientCategoryVo> categories = sysInfoCacheService
                .getList(cacheKey, IngredientCategoryVo.class)
                .orElseGet(() -> {
                    List<IngredientPo> ingredientPos = emptyIfNull(sysInfoMapper.selectIngredientCategories());
                    List<IngredientCategoryVo> result = ingredientPos.stream()
                            .map(PoConvertToVo::convertToIngredientCategoryVo)
                            .collect(Collectors.toCollection(ArrayList::new));
                    sysInfoCacheService.putList(cacheKey, result);
                    return result;
                });
        return ApiResponse.success(categories);
    }

    @Override
    public ApiResponse<List<IngredientVo>> getIngredientsByCategory(IngredientReqDto ingredientReqDto) {
        MyUtils.check()
                .notNull(ingredientReqDto, "食材查询参数")
                .notEmpty(ingredientReqDto.getCategory(), "category")
                .notNull(ingredientReqDto.getIsMain(), "isMain");

        IngredientPo condition = DtoConvertToPo.convertToIngredientPo(ingredientReqDto);
        String cacheKey = RedisKeyConst.SYS_INFO_INGREDIENT_PREFIX + condition.getCategory();
        List<IngredientVo> ingredients = sysInfoCacheService
                .getList(cacheKey, IngredientVo.class)
                .orElseGet(() -> {
                    List<IngredientIconDto> ingredientIconDtos = emptyIfNull(
                            sysInfoMapper.selectIngredientsByCategory(condition));
                    List<IngredientVo> result = ingredientIconDtos.stream()
                            .map(DtoConvertToVo::convertToVo)
                            .collect(Collectors.toCollection(ArrayList::new));
                    sysInfoCacheService.putList(cacheKey, result);
                    return result;
                });
        return ApiResponse.success(ingredients);
    }

    @Override
    public ApiResponse<List<CookWayVo>> getCookWays() {
        String cacheKey = RedisKeyConst.SYS_INFO_COOK_WAY_KEY;
        List<CookWayVo> cookWays = sysInfoCacheService
                .getList(cacheKey, CookWayVo.class)
                .orElseGet(() -> {
                    List<CookWayPo> cookWayPos = emptyIfNull(sysInfoMapper.selectAllCookWays());
                    List<CookWayVo> result = cookWayPos.stream()
                            .map(PoConvertToVo::convertToCookWayVo)
                            .collect(Collectors.toCollection(ArrayList::new));
                    sysInfoCacheService.putList(cacheKey, result);
                    return result;
                });
        return ApiResponse.success(cookWays);
    }

    private static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
