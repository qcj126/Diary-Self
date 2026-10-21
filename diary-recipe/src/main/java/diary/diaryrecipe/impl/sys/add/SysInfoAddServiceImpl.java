package diary.diaryrecipe.impl.sys.add;

import diary.common.exception.NullResultException;
import diary.common.result.ApiResponse;
import diary.diaryrecipe.entity.convert.sys.DtoConvertToPo;
import diary.diaryrecipe.entity.dto.IngredientReqDto;
import diary.diaryrecipe.entity.po.IngredientPo;
import diary.diaryrecipe.mapper.SysInfoMapper;
import diary.diaryrecipe.service.sys.add.SysInfoAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysInfoAddServiceImpl implements SysInfoAddService {
    private final SysInfoMapper sysInfoMapper;
    @Override
    public ApiResponse<String> addIngredient(IngredientReqDto ingredientReqDto) {
        MyUtils.check()
                .notNull(ingredientReqDto, "食材参数不能为空")
                .notNull(ingredientReqDto.getCategory(), "食材类别不能为空")
                .notNull(ingredientReqDto.getCategoryName(), "食材中文类别不能为空")
                .notNull(ingredientReqDto.getName(), "食材名称不能为空")
                .notNull(ingredientReqDto.getIsMain(), "是否主料不能为空")
                .notNull(ingredientReqDto.getIconId(), "食材图标不能为空");
//        IconPO iconPO = iconMapper.selectIconById(ingredientReqDto.getIconId());
//        if (iconPO == null) {
//            throw new NullResultException("食材图标不存在, iconId: " + ingredientReqDto.getIconId());
//        }

        IngredientPo ingredientPo = DtoConvertToPo.convertToIngredientPo(ingredientReqDto);
        ingredientPo.setId(MyUtils.getPrimaryKey());
        int ingredientCnt = sysInfoMapper.insertIngredient(ingredientPo);
        if (ingredientCnt > 0) return ApiResponse.success("添加成功");
        return ApiResponse.addFail();
    }
}
