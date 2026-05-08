package diary.common.convert;

import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;

import java.time.LocalDateTime;

public class DtoConvertToPo {
    public static RecipePO convertToPO(RecipeReqDto recipeReqDto, long keyId) {
        RecipePO recipePO = new RecipePO();
        // 填充元素
        recipePO.setRecipeId(keyId);
        recipePO.setAuthorId(recipeReqDto.getAuthorId());
        recipePO.setTitle(recipeReqDto.getTitle());
        recipePO.setCoverImg(recipeReqDto.getCoverImg());
        recipePO.setDescription(recipeReqDto.getDescription());
        recipePO.setCategory(recipeReqDto.getCategory());
        recipePO.setMealType(recipeReqDto.getMealType());
        recipePO.setDifficulty(recipeReqDto.getDifficulty());
        recipePO.setCookingTime(recipeReqDto.getCookingTime());
        recipePO.setStory(recipeReqDto.getStory());
        recipePO.setIsAnniversary(recipeReqDto.getIsAnniversary());
        recipePO.setAnniversaryDate(recipeReqDto.getAnniversaryDate());
        recipePO.setStatus(recipeReqDto.getStatus()); // 已发布
        recipePO.setViewCount(0);
        recipePO.setLikeCount(0);
        recipePO.setCookCount(0);
        recipePO.setIsAnniversary(recipeReqDto.getIsAnniversary());
        recipePO.setAnniversaryDate(recipeReqDto.getAnniversaryDate());
        recipePO.setCreatedAt(LocalDateTime.now());
        recipePO.setUpdatedAt(LocalDateTime.now());
        return recipePO;
    }
}
