package com.alibaba.easyexcel.test.core.parameter;

import com.alibaba.excel.annotation.ExcelCollection;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author zhaoqiang
 * @Date 2022/5/15 10:07 PM
 * @Version
 */
@Getter
@Setter
@EqualsAndHashCode
public class Course {
    @ExcelProperty(index = 4)
    private String courseName;
    @ExcelCollection(index = 5)
    private List<Student> studetList;
}
