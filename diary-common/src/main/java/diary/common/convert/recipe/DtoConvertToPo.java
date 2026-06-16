package diary.common.convert.recipe;

import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;

import java.time.LocalDateTime;

public class DtoConvertToPo {
    public static RecipePO convertToPO(RecipeReqDto recipeReqDto, long keyId) {
        RecipePO recipePO = new RecipePO();
        // 填充元素
        recipePO.setId(keyId);
        recipePO.setUserId(recipeReqDto.getAuthorId());
        recipePO.setTitle(recipeReqDto.getTitle());
        recipePO.setCoverImg(recipeReqDto.getCoverImg());
        recipePO.setDescription(recipeReqDto.getDescription());
        recipePO.setCategory(recipeReqDto.getCategory());
        recipePO.setMealType(recipeReqDto.getMealType());
        recipePO.setDifficulty(recipeReqDto.getDifficulty());
        recipePO.setCookingTime(recipeReqDto.getCookingTime());
        recipePO.setStory(recipeReqDto.getStory());
        recipePO.setCategory(recipeReqDto.getCategory());
        recipePO.setSort(0);
        recipePO.setDeleted(0);
        recipePO.setCreateTime(LocalDateTime.now());
        recipePO.setUpdateTime(LocalDateTime.now());
        return recipePO;
    }
}
