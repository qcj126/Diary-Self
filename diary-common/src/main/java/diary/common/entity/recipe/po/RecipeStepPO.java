package diary.common.entity.recipe.po;

import lombok.Data;

@Data
public class RecipeStepPO {
    /** 步骤主键（雪花算法生成） */
    private Long stepId;

    /** 食谱ID */
    private Long recipeId;

    /** 步骤编号 */
    private Integer stepNumber;

    /** 步骤文字描述 */
    private String description;

    /** 步骤图片URL */
    private String imageUrl;

    /** 该步骤计时（分钟），NULL 表示无需计时 */
    private Integer timerMin;

    /** 排序 */
    private Integer sortOrder;

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
