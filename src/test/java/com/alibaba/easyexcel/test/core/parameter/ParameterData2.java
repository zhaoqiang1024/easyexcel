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
public class ParameterData2 {
    @ExcelProperty("经历")
    private String experience;
    @ExcelProperty("年龄")
    private String age;
}
