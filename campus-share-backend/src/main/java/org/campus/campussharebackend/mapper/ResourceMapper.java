package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.campus.campussharebackend.entity.Resource;

@Mapper
public interface ResourceMapper {

    /**
     * 按共同锁顺序先锁定资源行，再锁定预约行。
     */
    @Select("SELECT * FROM resource WHERE id = #{id} FOR UPDATE")
    Resource findByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM resource WHERE id = #{id}")
    Resource findById(@Param("id") Long id);
}
