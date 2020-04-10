package com.example.lock.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.lock.demo.entity.Goods;
import com.example.lock.demo.entity.Order;
import com.example.lock.demo.mapper.GoodsMapper;
import com.example.lock.demo.mapper.OrderMapper;
import com.example.lock.demo.util.db.DBLockUtil;
import com.example.lock.demo.util.pes.PesLockUtil;
import com.example.lock.demo.util.uni.UniLockUtil;
import com.example.lock.demo.util.zk.ZkLockUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class DemoApplicationTests {

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private OrderMapper orderMapper;

    public final static int TOTAL = 1000; //库存总数
    public final static int GOOD_ID = 1; // 商品ID

    @Resource
    private CuratorFramework zkClient;

    // ZK-Curator 锁
    @Test
    public void CuratorLock() {
        InterProcessMutex lock = new InterProcessMutex(zkClient, "/lock/zkLock");
        testLock(() -> {
            try {
                lock.acquire();
                lock.acquire();
                buy(GOOD_ID);
                lock.release();
                lock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ZK 锁
    @Test
    public void ZkLock() {
        testLock(() -> {
            ZkLockUtil.lock("key");
            ZkLockUtil.lock("key");
            buy(GOOD_ID);
            ZkLockUtil.unlock("key");
            ZkLockUtil.unlock("key");
        });
    }


    // 数据库锁
    @Test
    public void DBlock() {
        String lockName = "dbLock";
        testLock(() -> {
            try {
                DBLockUtil.lock(lockName);
                DBLockUtil.lock(lockName);
                buy(GOOD_ID);
                DBLockUtil.unlock(lockName);
                DBLockUtil.unlock(lockName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 悲观锁
    @Test
    public void PesLock() {
        String lockName = "PesLock";
        testLock(() -> {
            try {
                PesLockUtil.lock(lockName);
                PesLockUtil.lock(lockName);
                buy(GOOD_ID);
                PesLockUtil.unlock(lockName);
                PesLockUtil.unlock(lockName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 需要开启数据库定时器: SET GLOBAL event_scheduler = 1;
    @Test
    public void UniLock() {
        String lockName = "optLock";
        testLock(() -> {
            try {
                UniLockUtil.lock(lockName);
                UniLockUtil.lock(lockName);
                buy(GOOD_ID);
                UniLockUtil.unlock(lockName);
                UniLockUtil.unlock(lockName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 初始化模拟数据
     */
    public void init() {
        // 清空订单表
        orderMapper.delete(Wrappers.emptyWrapper());
        // 删除商品表
        goodsMapper.delete(Wrappers.emptyWrapper());

        // 设置库存
        Goods goods = new Goods();
        goods.setId(GOOD_ID);
        goods.setTotal(TOTAL);
        goodsMapper.insert(goods);
    }

    /**
     * 模拟购物
     */
    public void buy(Integer id) {
        Goods goods = goodsMapper.selectById(id);
        Integer total = goods.getTotal();
        if (total == 0) {
            return;
        }

        // 下单
        goods.setTotal(total - 1);
        goods.updateById();
        Order order = new Order();
        order.setName(UUID.randomUUID().toString());
        order.insert();
    }

    /**
     * 并发测试
     */
    public void testLock(Runnable task) {
        init();

        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < TOTAL + 10; i++) { // 超卖10个单子
            executorService.submit(task);
        }

        executorService.shutdown();
        try {//等待直到所有任务完成
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;
        Goods goods = goodsMapper.selectById(GOOD_ID);
        Integer count = orderMapper.selectCount(Wrappers.emptyWrapper());
        System.out.println("time: " + time + ", goods.total:" + goods.getTotal() + ", order: " + count);
    }
}
