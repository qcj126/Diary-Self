package diary.user.mapper;

import diary.common.entity.user.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User selectByEmail(@Param("identify") String identify);

    User selectByPhone(@Param("identify") String identify);

    User selectByUsername(@Param("identify") String identify);

    int userRegister(User user);

    void updatePassword(@Param("username") String username, @Param("encodePassword") String encodePassword);

    User selectByUserId(@Param("userId") Long userId);

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    Long selectRoleIdByCode(@Param("roleCode") String roleCode);

    int insertUserRole(@Param("id") Long id, @Param("userId") Long userId, @Param("roleId") Long roleId);

    int disableUserByUsername(@Param("username") String username);

    List<User> selectUsers();
}
