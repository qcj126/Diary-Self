package diary.config.consts;

/**
 * 时间线分类常量
 * 包含大类和子类的完整定义
 */
public class TimeLineCategoryConsts {
    
    // ==================== 日记类 ====================
    /** 普通日记 - 日常心情、碎碎念 */
    public static final String TIME_LINE_CATEGORY_DIARY = "diary";
    /** 普通日记 - 子类 */
    public static final String TIME_LINE_SUBCATEGORY_DIARY_NORMAL = "diary_normal";
    /** 悄悄话 - 仅对方可见的私密文字 */
    public static final String TIME_LINE_SUBCATEGORY_DIARY_WHISPER = "diary_whisper";
    /** 信件/情书 - 长篇表白或节日信 */
    public static final String TIME_LINE_SUBCATEGORY_DIARY_LETTER = "diary_letter";
    
    // ==================== 照片类 ====================
    /** 照片大类 */
    public static final String TIME_LINE_CATEGORY_PHOTO = "photo";
    /** 普通合照 - 日常合影 */
    public static final String TIME_LINE_SUBCATEGORY_PHOTO_GROUP = "photo_group";
    /** 自拍 - 两人自拍 */
    public static final String TIME_LINE_SUBCATEGORY_PHOTO_SELFIE = "photo_selfie";
    /** 风景/静物 - 一起看的风景、共同制作的食物 */
    public static final String TIME_LINE_SUBCATEGORY_PHOTO_SCENERY = "photo_scenery";
    /** 拍立得/胶片 - 特殊滤镜风格 */
    public static final String TIME_LINE_SUBCATEGORY_PHOTO_POLAROID = "photo_polaroid";
    
    // ==================== 纪念日类 ====================
    /** 纪念日大类 */
    public static final String TIME_LINE_CATEGORY_ANNIVERSARY = "anniversary";
    /** 在一起天数 - 100天、1周年等 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_DAYS = "anniversary_days";
    /** 恋爱里程碑 - 第一次牵手、第一次接吻、第一次说爱你 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_MILESTONE = "anniversary_milestone";
    /** 生日 - 对方生日 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_BIRTHDAY = "anniversary_birthday";
    /** 节日 - 情人节、七夕、圣诞节等 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_FESTIVAL = "anniversary_festival";
    /** 求婚/订婚 - 特殊时刻 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_PROPOSAL = "anniversary_proposal";
    /** 领证/婚礼 - 人生大事 */
    public static final String TIME_LINE_SUBCATEGORY_ANNIVERSARY_WEDDING = "anniversary_wedding";
    
    // ==================== 心愿完成类 ====================
    /** 心愿完成大类 */
    public static final String TIME_LINE_CATEGORY_WISH = "wish";
    /** 旅行心愿 - 一起去某个城市/景点 */
    public static final String TIME_LINE_SUBCATEGORY_WISH_TRAVEL = "wish_travel";
    /** 美食心愿 - 打卡某家餐厅、做一道菜 */
    public static final String TIME_LINE_SUBCATEGORY_WISH_FOOD = "wish_food";
    /** 体验心愿 - 滑雪、潜水、演唱会等 */
    public static final String TIME_LINE_SUBCATEGORY_WISH_EXPERIENCE = "wish_experience";
    /** 礼物心愿 - 收到想要的礼物 */
    public static final String TIME_LINE_SUBCATEGORY_WISH_GIFT = "wish_gift";
    
    // ==================== 礼物类 ====================
    /** 礼物大类 */
    public static final String TIME_LINE_CATEGORY_GIFT = "gift";
    /** 赠送礼物 - 我送给对方的 */
    public static final String TIME_LINE_SUBCATEGORY_GIFT_GIVE = "gift_give";
    /** 收到礼物 - 对方送给我的 */
    public static final String TIME_LINE_SUBCATEGORY_GIFT_RECEIVE = "gift_receive";
    /** 手工礼物 - 自制卡片、手工制品 */
    public static final String TIME_LINE_SUBCATEGORY_GIFT_HANDMADE = "gift_handmade";
    
    // ==================== 旅行类 ====================
    /** 旅行大类 */
    public static final String TIME_LINE_CATEGORY_TRAVEL = "travel";
    /** 短途旅行 - 周末周边游 */
    public static final String TIME_LINE_SUBCATEGORY_TRAVEL_SHORT = "travel_short";
    /** 长途旅行 - 跨省/出国 */
    public static final String TIME_LINE_SUBCATEGORY_TRAVEL_LONG = "travel_long";
    /** 露营/户外 - 帐篷、登山 */
    public static final String TIME_LINE_SUBCATEGORY_TRAVEL_CAMPING = "travel_camping";
    /** 酒店/民宿 - 住宿体验 */
    public static final String TIME_LINE_SUBCATEGORY_TRAVEL_HOTEL = "travel_hotel";
    
    // ==================== 美食类 ====================
    /** 美食大类 */
    public static final String TIME_LINE_CATEGORY_FOOD = "food";
    /** 餐厅打卡 - 某家网红店 */
    public static final String TIME_LINE_SUBCATEGORY_FOOD_RESTAURANT = "food_restaurant";
    /** 自制美食 - 一起做饭、烘焙 */
    public static final String TIME_LINE_SUBCATEGORY_FOOD_HOMEMADE = "food_homemade";
    /** 外卖/零食 - 宅家美食 */
    public static final String TIME_LINE_SUBCATEGORY_FOOD_SNACK = "food_snack";
    /** 甜品/奶茶 - 下午茶时光 */
    public static final String TIME_LINE_SUBCATEGORY_FOOD_DESSERT = "food_dessert";
    
    // ==================== 娱乐类 ====================
    /** 娱乐大类 */
    public static final String TIME_LINE_CATEGORY_ENTERTAINMENT = "entertainment";
    /** 电影 - 一起看的电影 */
    public static final String TIME_LINE_SUBCATEGORY_ENTERTAINMENT_MOVIE = "entertainment_movie";
    /** 音乐/演唱会 - 共同听的歌、现场 */
    public static final String TIME_LINE_SUBCATEGORY_ENTERTAINMENT_MUSIC = "entertainment_music";
    /** 游戏 - 双人游戏、电竞 */
    public static final String TIME_LINE_SUBCATEGORY_ENTERTAINMENT_GAME = "entertainment_game";
    /** 阅读/学习 - 一起看书、备考 */
    public static final String TIME_LINE_SUBCATEGORY_ENTERTAINMENT_STUDY = "entertainment_study";
    
    // ==================== 日常小事类 ====================
    /** 日常小事大类 */
    public static final String TIME_LINE_CATEGORY_DAILY = "daily";
    /** 散步/遛弯 - 饭后一起散步 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_WALK = "daily_walk";
    /** 购物 - 逛超市、商场 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_SHOPPING = "daily_shopping";
    /** 家务 - 一起打扫、做饭 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_CHORE = "daily_chore";
    /** 视频通话 - 异地恋日常 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_VIDEO_CALL = "daily_video_call";
    /** 睡觉/起床 - 同床共枕或早安晚安 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_SLEEP = "daily_sleep";
    /** 吵架/和好 - 矛盾与和解 */
    public static final String TIME_LINE_SUBCATEGORY_DAILY_FIGHT = "daily_fight";
    
    // ==================== 里程碑类 ====================
    /** 里程碑大类 */
    public static final String TIME_LINE_CATEGORY_MILESTONE = "milestone";
    /** 复合 - 重新在一起 */
    public static final String TIME_LINE_SUBCATEGORY_MILESTONE_RECONCILE = "milestone_reconcile";
    /** 见家长 - 第一次见父母 */
    public static final String TIME_LINE_SUBCATEGORY_MILESTONE_MEET_PARENTS = "milestone_meet_parents";
    /** 同居 - 开始同居生活 */
    public static final String TIME_LINE_SUBCATEGORY_MILESTONE_COHABIT = "milestone_cohabit";
    /** 异国/异地结束 - 结束距离 */
    public static final String TIME_LINE_SUBCATEGORY_MILESTONE_END_DISTANCE = "milestone_end_distance";
}
