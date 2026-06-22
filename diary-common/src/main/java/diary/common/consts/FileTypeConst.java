package diary.common.consts;

import java.util.Arrays;
import java.util.List;

public class FileTypeConst {
    // 内容类型前缀
    public static final String CONTENT_TYPE_IMAGE_PREFIX = "image/";
    public static final String CONTENT_TYPE_VIDEO_PREFIX = "video/";
    public static final String CONTENT_TYPE_AUDIO_PREFIX = "audio/";
    public static final String CONTENT_TYPE_APPLICATION_PREFIX = "application/";
    
    // 图片扩展名
    public static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg"
    );
    
    // 视频扩展名
    public static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "mpeg", "3gp", "m4v"
    );
    
    // 音频扩展名
    public static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
            "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "opus"
    );
    
    // 文档扩展名
    public static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"
    );

    // 图片分类名称
    public static final String DIET_IMAGE = "美食图片";
    public static final String RECIPE_IMAGE = "食谱图片";
    public static final String INGREDIENT_IMAGE = "食材图片";
    public static final String BEAN_MATCHING_IMAGE = "拼豆图片";
    public static final String GIFT_IMAGE = "礼物图片";
    public static final String SNACK_IMAGE = "零食图片";
    public static final String TEA_IMAGE = "奶茶图片";
    public static final String TRAVEL_IMAGE = "旅行图片";
    public static final String DAILY_IMAGE = "日常图片";
    public static final String EXERCISE_IMAGE = "锻炼图片";
    public static final String WALK_IMAGE = "散步图片";

    // 图片路径
    public static final String DIET_IMAGE_PATH = "food_path_";
    public static final String RECIPE_IMAGE_PATH = "cook_path_";
    public static final String INGREDIENT_IMAGE_PATH = "ingredient_path_";
    public static final String BEAN_MATCHING_IMAGE_PATH = "bean_matching_path_";
    public static final String GIFT_IMAGE_PATH = "gift_path_";
    public static final String SNACK_IMAGE_PATH = "snack_path_";
    public static final String TEA_IMAGE_PATH = "tea_path_";
    public static final String TRAVEL_IMAGE_PATH = "travel_path_";
    public static final String DAILY_IMAGE_PATH = "daily_path_";
    public static final String EXERCISE_IMAGE_PATH = "exercise_path_";
    public static final String WALK_IMAGE_PATH = "walk_path";
}
