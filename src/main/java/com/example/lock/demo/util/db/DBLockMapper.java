package com.example.lock.demo.util.db;

import org.apache.ibatis.annotations.Select;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
public interface DBLockMapper {

    @Select("SELECT GET_LOCK(#{key}, 1);")
    Integer lock(String key);

    @Select("select RELEASE_LOCK(#{key});")
    Integer unlock(String key);
}
