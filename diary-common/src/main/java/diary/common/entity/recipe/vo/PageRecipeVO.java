package diary.common.entity.recipe.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应结果
 */
@Data
public class PageRecipeVO<T> {
    private Long total;          // 总记录数
    private Long pages;          // 总页数
    private Long current;        // 当前页码
    private Long size;           // 每页大小
    private List<T> records;     // 数据列表

    /**
     * 将 MyBatis-Plus 的 IPage 转换为统一的 PageRespDto
     */
    public static <T> PageRecipeVO<T> of(IPage<T> page) {
        PageRecipeVO<T> dto = new PageRecipeVO<>();
        dto.setTotal(page.getTotal());
        dto.setPages(page.getPages());
        dto.setCurrent(page.getCurrent());
        dto.setSize(page.getSize());
        dto.setRecords(page.getRecords());
        return dto;
    }
}
