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
public class ParameterData1 {
    @ExcelProperty(index = 6)
    private String workName;
    @ExcelCollection(index = 7)
    private List<ParameterData2> workList;
}
