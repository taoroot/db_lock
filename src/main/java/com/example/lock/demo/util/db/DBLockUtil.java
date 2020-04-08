package com.example.lock.demo.util.db;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author : zhiyi
 * Date: 2020/4/8
 * 利用数据库的 GET_LOCK 和 RELEASE_LOCK 实现分布式锁机制
 */
@Log4j2
@Component
public class DBLockUtil implements ApplicationContextAware {
    private static SqlSessionFactory sqlSessionFactory;

    private static ThreadLocal<SqlSessionWrapper> sessionLocal = new ThreadLocal<>();

    @AllArgsConstructor
    private static class SqlSessionWrapper {
        int count;
        SqlSession sqlSession;
    }

    public static void lock(String key) {
        while (!tryLock(key)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean tryLock(String key) {
        SqlSessionWrapper sqlSessionWrapper = sessionLocal.get();
        if (sqlSessionWrapper == null) {
            SqlSession sqlSession = sqlSessionFactory.openSession();
            sqlSessionWrapper = new SqlSessionWrapper(0, sqlSession);
            sessionLocal.set(sqlSessionWrapper);

            Integer result = sqlSessionWrapper.sqlSession.getMapper(DBLockMapper.class).lock(key);
            if (result == null || result == 0) {
                sqlSession.close();
                sessionLocal.remove();
                return false;
            }
        }

        sqlSessionWrapper.count++;
        return true;
    }

    public static void unlock(String key) {
        SqlSessionWrapper sqlSessionWrapper = sessionLocal.get();
        sqlSessionWrapper.count--;
        if (sqlSessionWrapper.count == 0) {
            sqlSessionWrapper.sqlSession.getMapper(DBLockMapper.class).unlock(key);
            sqlSessionWrapper.sqlSession.close();
            sessionLocal.remove();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
        sqlSessionFactory.getConfiguration().addMapper(DBLockMapper.class);
    }
}
