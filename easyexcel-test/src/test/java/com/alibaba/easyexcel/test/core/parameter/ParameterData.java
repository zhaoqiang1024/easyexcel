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

    @ExcelProperty(value = "姓名")
    private String name;
    @ExcelCollection
    private List<Course> courseList;
    @ExcelCollection
    private List<ParameterData1> workList;
}
