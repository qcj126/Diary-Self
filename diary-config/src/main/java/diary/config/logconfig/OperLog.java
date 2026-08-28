package diary.config.logconfig;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)  // 运行时生效
@Documented
public @interface OperLog {
    /**
     * 操作模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 操作类型：新增、删除、修改、查询等
     */
    String operationType() default "OTHER";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否保存返回结果
     */
    boolean saveResult() default false;
}
