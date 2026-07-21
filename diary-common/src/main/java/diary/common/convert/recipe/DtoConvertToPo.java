package diary.common.convert.recipe;

import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;

import java.time.LocalDateTime;

public class DtoConvertToPo {
    public static RecipePO recipeReqDtoConvertToPO(RecipeReqDto recipeReqDto) {
        LocalDateTime now = LocalDateTime.now();
        RecipePO recipePO = new RecipePO();
        recipePO.setId(recipeReqDto.getId());
        recipePO.setUserId(recipeReqDto.getAuthorId());
        recipePO.setTitle(recipeReqDto.getTitle());
        recipePO.setImageId(recipeReqDto.getImageId());
        recipePO.setDescription(recipeReqDto.getDescription());
        recipePO.setCategory(recipeReqDto.getCategory());
        recipePO.setMealType(recipeReqDto.getMealType());
        recipePO.setDifficulty(recipeReqDto.getDifficulty());
        recipePO.setCookingTime(recipeReqDto.getCookingTime());
        recipePO.setStory(recipeReqDto.getStory() == null ? "" : recipeReqDto.getStory());
        recipePO.setSort(0);
        recipePO.setDeleted(0);
        recipePO.setCreateTime(now);
        recipePO.setUpdateTime(now);
        return recipePO;
    }
}
