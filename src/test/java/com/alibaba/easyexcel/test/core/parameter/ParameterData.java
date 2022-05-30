package com.alibaba.easyexcel.test.core.parameter;

import com.alibaba.excel.annotation.ExcelCollection;
import com.alibaba.excel.annotation.ExcelProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Jiaju Zhuang
 */
@Getter
@Setter
@EqualsAndHashCode
public class ParameterData {

    @ExcelProperty(index = 3)
    private String name;
    @ExcelCollection(index = 4)
    private List<Course> courseList;
    @ExcelCollection(index = 6)
    private List<ParameterData1> workList;
}
