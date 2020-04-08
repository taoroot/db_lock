package com.example.lock.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品表
 *
 * @author zhiyi
 * @date 2020-03-24 10:17:50
 */
@Data
@TableName("tb_goods")
@EqualsAndHashCode(callSuper = true)
public class Goods extends Model<Goods> {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Integer id;
    private Integer total;
}
