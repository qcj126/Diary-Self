package diary.common.entity.sysInfo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookWayPo {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private Integer sortOrder;
    private String createTime;
    private String updateTime;
}
