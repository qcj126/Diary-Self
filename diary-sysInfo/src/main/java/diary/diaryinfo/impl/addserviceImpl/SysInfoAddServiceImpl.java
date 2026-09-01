package diary.diaryinfo.impl.addserviceImpl;

import diary.common.convert.sysInfo.DtoConvertToPo;
import diary.common.entity.file.po.IconPO;
import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.po.IngredientPo;
import diary.common.exception.NullResultException;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.IconMapper;
import diary.diaryinfo.mapper.SysInfoMapper;
import diary.diaryinfo.service.addservice.SysInfoAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysInfoAddServiceImpl implements SysInfoAddService {
    private final IconMapper iconMapper;
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
        IconPO iconPO = iconMapper.selectIconById(ingredientReqDto.getIconId());
        if (iconPO == null) {
            throw new NullResultException("食材图标不存在, iconId: " + ingredientReqDto.getIconId());
        }

        IngredientPo ingredientPo = DtoConvertToPo.convertToIngredientPo(ingredientReqDto);
        ingredientPo.setId(MyUtils.getPrimaryKey());
        int ingredientCnt = sysInfoMapper.insertIngredient(ingredientPo);
        if (ingredientCnt > 0) return ApiResponse.success("添加成功");
        return ApiResponse.addFail();
    }
}
