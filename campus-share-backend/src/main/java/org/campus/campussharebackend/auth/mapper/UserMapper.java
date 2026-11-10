package org.campus.campussharebackend.auth.mapper;

import org.campus.campussharebackend.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, account, password_hash, display_name, role, enabled
            FROM app_user
            WHERE account = #{account}
            """)
    UserAccount findByAccount(String account);
}
