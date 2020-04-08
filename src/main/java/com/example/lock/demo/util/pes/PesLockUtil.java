package com.example.lock.demo.util.pes;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
@Log4j2
@Component
public class PesLockUtil implements ApplicationContextAware {

    private static SqlSessionFactory sqlSessionFactory;

    private static DataSourceTransactionManager tcm;

    private static ThreadLocal<Map<String, TransactionStatusWrapper>> transactionLocal = new ThreadLocal<>();

    @AllArgsConstructor
    private static class TransactionStatusWrapper {
        int count;
        SqlSession sqlSession;
        TransactionStatus transactionStatus;
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
        Map<String, TransactionStatusWrapper> map = transactionLocal.get();

        if (map == null) {
            map = new HashMap<>();
            transactionLocal.set(map);
        }

        TransactionStatusWrapper transactionWrapper = map.get(key);
        if (transactionWrapper == null) {
            DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
            definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

            SqlSession sqlSession = sqlSessionFactory.openSession();
            transactionWrapper = new TransactionStatusWrapper(0, sqlSession, tcm.getTransaction(definition));
            map.put(key, transactionWrapper);
        }

        PesLockMapper mapper = transactionWrapper.sqlSession.getMapper(PesLockMapper.class);
        Integer mysqlLock = mapper.selectForUpdate(key);

        if (mysqlLock == null || mysqlLock == 0) {
            try {
                mapper.insert(key);
            } catch (Exception e) {
                return false;
            }
        }

        transactionWrapper.count++;
        return true;
    }

    public static void unlock(String key) {
        Map<String, TransactionStatusWrapper> map = transactionLocal.get();
        TransactionStatusWrapper transactionWrapper = map.get(key);
        transactionWrapper.count--;
        if (transactionWrapper.count == 0) {
            tcm.commit(transactionWrapper.transactionStatus);
            transactionWrapper.sqlSession.close();
            map.remove(key);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
        sqlSessionFactory.getConfiguration().addMapper(PesLockMapper.class);
        tcm = applicationContext.getBean("transactionManager", DataSourceTransactionManager.class);
    }
}
