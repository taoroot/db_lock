package com.example.lock.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author : zhiyi
 * Date: 2020/4/7
 */
@Data
@TableName("tb_order")
@EqualsAndHashCode(callSuper = true)
public class Order extends Model<Goods> {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
}
