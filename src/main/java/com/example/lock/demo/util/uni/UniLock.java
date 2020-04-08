package com.example.lock.demo.util.uni;

import lombok.Data;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
@Data
public class UniLock {
    private String key;
    private String nodeId;
    private Integer count;
}
