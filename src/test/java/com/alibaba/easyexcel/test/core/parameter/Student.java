package com.alibaba.easyexcel.test.core.parameter;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author zhaoqiang
 * @Date 2022/5/15 10:08 PM
 * @Version
 */
@Getter
@Setter
@EqualsAndHashCode
public class Student {
    @ExcelProperty("学生名称")
    private String studentName;
}
