package diary.diaryrecipe.entity.convert;

import diary.diaryrecipe.entity.po.RecipeIngredientPO;
import diary.diaryrecipe.entity.po.RecipePO;
import diary.diaryrecipe.entity.po.RecipeStepPO;
import diary.diaryrecipe.entity.vo.IngredientVO;
import diary.diaryrecipe.entity.vo.RecipeVO;
import diary.diaryrecipe.entity.vo.StepVO;

public class PoConvertToVo {

    /**
     * RecipePO 转 RecipeVO
     */
    public static RecipeVO convertToRecipeVO(RecipePO po) {
        RecipeVO vo = new RecipeVO();
        vo.setRecipeId(po.getId());
        vo.setTitle(po.getTitle());
        vo.setImageId(po.getImageId());
        vo.setDescription(po.getDescription());
        vo.setCategoryNum(po.getCategoryNum());
        vo.setMealType(po.getMealType());
        vo.setDifficulty(po.getDifficulty());
        vo.setCookWay(po.getCookWay());
        vo.setCookingTime(po.getCookingTime());
        vo.setStory(po.getStory());
        vo.setFamilyMember(po.getFamilyMember());
        vo.setRedHeart(po.getRedHeart());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    public static IngredientVO convertToIngredientVO(RecipeIngredientPO po) {
        if (po == null) {
            return null;
        }
        IngredientVO vo = new IngredientVO();
        vo.setIngredientId(po.getId());
        vo.setName(po.getName());
        vo.setQuantity(po.getQuantity());
        vo.setIsMain(po.getIsMain());
        return vo;
    }

    public static StepVO convertToStepVO(RecipeStepPO po) {
        if (po == null) {
            return null;
        }
        StepVO vo = new StepVO();
        vo.setStepId(po.getId());
        vo.setStepNumber(po.getStepNumber());
        vo.setDescription(po.getDescription());
        vo.setTimerMin(po.getTimerMinute());
        return vo;
    }
}
