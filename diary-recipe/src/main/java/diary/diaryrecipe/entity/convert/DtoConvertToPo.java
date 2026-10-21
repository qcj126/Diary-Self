package diary.diaryrecipe.entity.convert;

import diary.diaryrecipe.entity.dto.RecipeCategoryDto;
import diary.diaryrecipe.entity.dto.RecipeReqDto;
import diary.diaryrecipe.entity.po.RecipeCategoryPO;
import diary.diaryrecipe.entity.po.RecipePO;

import java.time.LocalDateTime;

public class DtoConvertToPo {
    public static RecipePO recipeReqDtoConvertToPO(RecipeReqDto recipeReqDto) {
        LocalDateTime now = LocalDateTime.now();
        RecipePO recipePO = new RecipePO();
        recipePO.setId(recipeReqDto.getRecipeId());
        recipePO.setUserId(recipeReqDto.getAuthorId());
        recipePO.setTitle(recipeReqDto.getTitle());
        recipePO.setImageId(recipeReqDto.getImageId());
        recipePO.setDescription(recipeReqDto.getDescription());
        recipePO.setCategoryNum(recipeReqDto.getCategoryNum());
        recipePO.setMealType(recipeReqDto.getMealType());
        recipePO.setDifficulty(recipeReqDto.getDifficulty());
        recipePO.setCookWay(recipeReqDto.getCookWay());
        recipePO.setCookingTime(recipeReqDto.getCookingTime());
        recipePO.setStory(recipeReqDto.getStory() == null ? "" : recipeReqDto.getStory());
        recipePO.setFamilyMember(recipeReqDto.getFamilyMember());
        recipePO.setSort(0);
        recipePO.setCreateTime(now);
        recipePO.setUpdateTime(now);
        return recipePO;
    }

    public static RecipeCategoryPO recipeCategoryDtoConvertToPO(RecipeCategoryDto recipeCategoryDto) {
        return RecipeCategoryPO.builder()
                .id(recipeCategoryDto.getCategoryId())
                .userId(recipeCategoryDto.getUserId())
                .categoryName(recipeCategoryDto.getCategoryName())
                .categoryNum(recipeCategoryDto.getCategoryNum())
                .iconId(recipeCategoryDto.getIconId())
                .build();
    }
}
