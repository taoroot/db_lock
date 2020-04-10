package com.example.lock.demo.util.zk;

import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * @author : zhiyi
 * Date: 2020/4/9
 */
@Log4j2
public class ZkLockUtil {

    // 映射表
    // 记录线程与锁信息的映射关系
    private final static ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();
    private final static CountDownLatch count = new CountDownLatch(1);

    // 锁信息
    private static class LockData {
        final Thread owningThread;
        final String lockPath;
        final AtomicInteger lockCount = new AtomicInteger(1); // 分布式锁重入次数

        private LockData(Thread owningThread, String lockPath) {
            this.owningThread = owningThread;
            this.lockPath = lockPath;
        }
    }


    private static ZooKeeper zooKeeper;

    public static ZooKeeper instance() {
        if (zooKeeper == null) {
            synchronized (ZkLockUtil.class) {
                if (zooKeeper == null) {
                    try {
                        zooKeeper = new ZooKeeper("127.0.0.1:2181", 1000, event -> {
                            if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                                count.countDown();
                            }
                        });
                        count.await();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return zooKeeper;
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

        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        // 已经获取, 可重入
        if (lockData != null) {
            lockData.lockCount.incrementAndGet();
            return true;
        }
        // zookeeper 获取锁
        String lockPath = getLock(key);
        if (lockPath != null) {
            LockData newLockData = new LockData(currentThread, lockPath);
            threadData.put(currentThread, newLockData);
            return true;
        }
        return false;
    }


    public static String getLock(String key) {
        ZooKeeper zk = instance();
        boolean isDone = false;     // 是否完成尝试获取分布式锁的操作
        String ourPath = null;

        try {
            // 创建临时顺序节点
            ourPath = zk.create("/" + key, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            while (!isDone) {
                // 查询并排序
                List<String> children = zk.getChildren("/", false)
                        .stream()
                        .filter(name -> name.contains(key))
                        .sorted(Comparator.comparing(lhs -> standardFixForSorting(lhs, key)))
                        .collect(Collectors.toList());
//                log.info("{}:{}", children, ourPath);

                int index = 0;
                for (String child : children) {
                    if (standardFixForSorting(ourPath, key).equals(standardFixForSorting(child, key))) {
                        break;
                    }
                    index++;
                }

                if (index == 0) {
                    isDone = true;
                } else {
                    index--;
                    Thread thread = Thread.currentThread();
                    zk.exists("/" + children.get(index), watchedEvent -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                            LockSupport.unpark(thread);
                        }
                    });
                    LockSupport.park();
                }
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return ourPath;
    }

    public static void unlock(String key) {
        // zookeeper 获取锁
        LockData lockData = threadData.get(Thread.currentThread());
        if (lockData != null) {
            if (lockData.lockCount.decrementAndGet() == 0) {
                try {
                    instance().delete(lockData.lockPath, 0);
                    threadData.remove(Thread.currentThread());
                } catch (InterruptedException | KeeperException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String standardFixForSorting(String str, String lockName) {
        int index = str.lastIndexOf(lockName);
        if (index >= 0) {
            index += lockName.length();
            return index <= str.length() ? str.substring(index) : "";
        }
        return str;
    }

}
