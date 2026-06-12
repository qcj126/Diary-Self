package diary.common.entity.recipe.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecipeStepPO {
    private Long id;                      // 主键
    private Long recipeId;               // 食谱ID
    private Long userId;                 // 创建者用户ID
    private Integer stepNumber;           // 步骤编号
    private String description;           // 步骤描述
    private String imageUrl;             // 步骤图片URL
    private Integer timerMinute;          // 步骤计时（分钟）
    private Integer sort;                 // 排序
    private Integer deleted;              // 是否删除：0-否 1-是
    private LocalDateTime createTime;     // 创建时间
    private LocalDateTime updateTime;     // 更新时间

//    String apiKey = "17cea7a8-c064-49b9-b44e-f65ab17d8fd4";
//
//    String mode = "curl https://ark.cn-beijing.volces.com/api/v3/chat/completions \\\n" +
//            "  -H \"Content-Type: application/json\" \\\n" +
//            "  -H \"Authorization: Bearer " + apiKey + "\" \\\n" +
//            "  -d $'{\n" +
//            "    \"model\": \"doubao-1.5-vision-pro-250328\",\n" +
//            "    \"messages\": [\n" +
//            "        {\n" +
//            "            \"content\": [\n" +
//            "                {\n" +
//            "                    \"image_url\": {\n" +
//            "                        \"url\": \"https://ark-project.tos-cn-beijing.ivolces.com/images/view.jpeg\"\n" +
//            "                    },\n" +
//            "                    \"type\": \"image_url\"\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"text\": \"图片主要讲了什么?\",\n" +
//            "                    \"type\": \"text\"\n" +
//            "                }\n" +
//            "            ],\n" +
//            "            \"role\": \"user\"\n" +
//            "        }\n" +
//            "    ]\n" +
//            "}'";
}
