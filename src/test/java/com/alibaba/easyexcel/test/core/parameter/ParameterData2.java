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
    @ExcelProperty(index = 7)
    private String experience;
    @ExcelProperty(index = 8)
    private String age;
}
