package com.example.lock.demo.util.uni;

import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 * 需要开启数据库的定时器  SET GLOBAL event_scheduler = 1;
 */
@Log4j2
@Component
public class UniLockUtil implements ApplicationContextAware {

    private static SqlSessionFactory sqlSessionFactory;

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
        String ip;
        String currentNodeId;

        // IP+线程ID
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            ip = localHost.getHostAddress();
            currentNodeId = ip + ":" + Thread.currentThread().getId();
        } catch (Exception e) {
            return false;
        }

        SqlSession sqlSession = sqlSessionFactory.openSession();
        UniLockMapper lockMapper = sqlSession.getMapper(UniLockMapper.class);
        UniLock lock = lockMapper.selectByKey(key);
        //  锁存在, 可重入操作
        if (lock != null) {
            if (!lock.getNodeId().equals(currentNodeId)) {
                sqlSession.close();
                return false;
            }
            lockMapper.update(key, lock.getCount() + 1);
        } else {
            try {
                lockMapper.insert(key, currentNodeId, 1);
            } catch (Exception e) {
                sqlSession.close();
                return false;
            }
        }
        sqlSession.close();
        return true;
    }

    public static void unlock(String key) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UniLockMapper lockMapper = sqlSession.getMapper(UniLockMapper.class);
        UniLock lock = lockMapper.selectByKey(key);
        lock.setCount(lock.getCount() - 1);
        if (lock.getCount() == 0) {
            lockMapper.delete(key);
        } else {
            lockMapper.update(key, lock.getCount());
        }
        sqlSession.close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
        sqlSessionFactory.getConfiguration().addMapper(UniLockMapper.class);
    }
}
