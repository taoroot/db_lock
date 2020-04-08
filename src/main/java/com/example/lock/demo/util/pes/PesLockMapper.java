package com.example.lock.demo.util.pes;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
public interface PesLockMapper {

    @Select("select count(*) from tb_pes_lock where `key` = #{key} for update;")
    Integer selectForUpdate(String key);

    @Insert("insert into tb_pes_lock(`key`) values (#{key});")
    void insert(@Param("key") String key);
}
