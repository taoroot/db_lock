package com.example.lock.demo.util.uni;

import org.apache.ibatis.annotations.*;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
public interface UniLockMapper {

    @Select("select * from tb_uni_lock where `key` = #{key};")
    UniLock selectByKey(String key);

    @Delete("delete from tb_uni_lock where `key` = #{key}")
    void delete(String key);

    @Insert("insert into tb_uni_lock(`key`, node_id, `count`) values (#{key}, #{nodeId}, #{count});")
    void insert(@Param("key") String key, @Param("nodeId") String nodeId, @Param("count") Integer count);

    @Update("update tb_uni_lock set `count` = #{count} where `key` = #{key}")
    void update(@Param("key") String key, @Param("count") Integer count);
}
