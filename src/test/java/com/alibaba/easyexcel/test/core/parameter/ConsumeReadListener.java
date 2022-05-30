package com.alibaba.easyexcel.test.core.parameter;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.IgnoreExceptionReadListener;
import com.alibaba.excel.read.metadata.holder.ReadHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.alibaba.excel.read.metadata.property.ExcelReadHeadProperty;
import com.alibaba.excel.util.BeanMapUtils;
import com.alibaba.excel.util.ClassUtils;
import com.alibaba.excel.util.ConverterUtils;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.fastjson.JSON;
import net.sf.cglib.beans.BeanMap;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Description
 * @Author zhaoqiang
 * @Date 2022/4/25 11:24 PM
 * @Version
 */
public class ConsumeReadListener<T> implements IgnoreExceptionReadListener<Map<Integer, ReadCellData<?>>> {

    private TreeMap<Integer, Map<Integer, ReadCellData<?>>> dataMaps = new TreeMap<>();
    private TreeMap<Integer, Map<Integer,CellExtra>> extraMap =new TreeMap<>();
    private List resultList = new ArrayList<>();

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> cellDataMap, AnalysisContext context) {
        if (context.readSheetHolder().getMaxDataHeadSize() == null
            || context.readSheetHolder().getMaxDataHeadSize() < CollectionUtils.size(cellDataMap)) {
            context.readSheetHolder().setMaxDataHeadSize(CollectionUtils.size(cellDataMap));
        }
    }

    @Override
    public void invoke(Map<Integer, ReadCellData<?>> cellDataMap, AnalysisContext context) {
        dataMaps.put(context.readSheetHolder().getRowIndex(),cellDataMap);
    }

    private Object buildStringList(Map<Integer, ReadCellData<?>> cellDataMap, ReadSheetHolder readSheetHolder,
                                   AnalysisContext context) {
        int index = 0;
        Map<Integer, String> map = MapUtils.newLinkedHashMapWithExpectedSize(cellDataMap.size());
        for (Map.Entry<Integer, ReadCellData<?>> entry : cellDataMap.entrySet()) {
            Integer key = entry.getKey();
            ReadCellData<?> cellData = entry.getValue();
            while (index < key) {
                map.put(index, null);
                index++;
            }
            index++;
            map.put(key,
                (String) ConverterUtils.convertToJavaObject(cellData, null, null, readSheetHolder.converterMap(),
                    context, context.readRowHolder().getRowIndex(), key));
        }
        // fix https://github.com/alibaba/easyexcel/issues/2014
        int headSize = calculateHeadSize(readSheetHolder);
        while (index < headSize) {
            map.put(index, null);
            index++;
        }
        return map;
    }

    private int calculateHeadSize(ReadSheetHolder readSheetHolder) {
        if (readSheetHolder.excelReadHeadProperty().getHeadMap().size() > 0) {
            return readSheetHolder.excelReadHeadProperty().getHeadMap().size();
        }
        if (readSheetHolder.getMaxDataHeadSize() != null) {
            return readSheetHolder.getMaxDataHeadSize();
        }
        return 0;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        int lastIndex = -1;
        for (Integer index : dataMaps.keySet()) {
            if (index <= lastIndex) {
                continue;
            }
            Map<Integer, ReadCellData<?>> data = dataMaps.get(index);
            Map<Integer, CellExtra> extra = extraMap.get(index);
            Object obj = null;
            if (extra!=null) {
                MergeData mergeData = this.merge(data, context, index, extra);
                //删除处理完成的
                extraMap.pollFirstEntry();
                obj = mergeData.getData();
                lastIndex = mergeData.getLastIndex();
            } else {
                obj= this.convertBean(data, context.readSheetHolder(), index, context);
            }
            resultList.add(obj);
        }
        System.out.println(JSON.toJSONString(resultList));
    }

    /**
     * 转换单行未merge对象
     * 该对象中存在list需要合并的结构此时list size为1
     * @param cellDataMap
     * @param readSheetHolder
     * @param rowIndex
     * @param context
     */
    private Object convertBean(Map<Integer, ReadCellData<?>> cellDataMap, ReadHolder readSheetHolder, int rowIndex, AnalysisContext context) {
        ExcelReadHeadProperty excelReadHeadProperty = readSheetHolder.excelReadHeadProperty();
        Map<Integer, Head> headMap = excelReadHeadProperty.getHeadMap();
        return buildUserModel(cellDataMap, readSheetHolder, context,rowIndex, excelReadHeadProperty.getHeadClazz(), headMap);
    }

    /**
     * 构建用户Bean对象
     * @param cellDataMap
     * @param readSheetHolder
     * @param context
     * @param clazz
     * @param headMap
     */
    private Object buildUserModel(Map<Integer, ReadCellData<?>> cellDataMap, ReadHolder readSheetHolder, AnalysisContext context,int rowIndex, Class<?> clazz, Map<Integer, Head> headMap) {
        Object resultModel;
        try {
            resultModel = clazz.newInstance();
        } catch (Exception e) {
            throw new ExcelDataConvertException(context.readRowHolder().getRowIndex(), 0,
                new ReadCellData<>(CellDataTypeEnum.EMPTY), null,
                "Can not instance class: " + clazz.getName(), e);
        }
        BeanMap dataMap = BeanMapUtils.create(resultModel);
        for (Map.Entry<Integer, Head> entry : headMap.entrySet()) {
            int cellIndex = entry.getKey();
            Head head = entry.getValue();
            if(head.getType().equals("LIST")){
                List list = new ArrayList();
                Object listResult = this.buildUserModel(cellDataMap, readSheetHolder, context, rowIndex, head.getCollectionClass(), head.getNextHead());
                list.add(listResult);
                dataMap.put(head.getFieldName(), list);
            }else{
                Object value = ConverterUtils.convertToJavaObject(cellDataMap.get(cellIndex), head.getField(),
                    ClassUtils.declaredExcelContentProperty(dataMap, readSheetHolder.excelReadHeadProperty().getHeadClazz(),
                        head.getFieldName()), readSheetHolder.converterMap(), context, rowIndex, cellIndex);
                if (value != null) {
                    dataMap.put(head.getFieldName(), value);
                }
            }
        }
        return resultModel;
    }

    private MergeData merge(Map<Integer, ReadCellData<?>> cellDataMap, AnalysisContext context,int rowIndex, Map<Integer, CellExtra> extra) {
        //循环Head配置
        ReadSheetHolder readSheetHolder = context.readSheetHolder();
        ExcelReadHeadProperty property = readSheetHolder.excelReadHeadProperty();
        Map<Integer,Head> headMap = property.getHeadMap();
        //如果当前列存在合并
        return this.buildMergeModel(cellDataMap, readSheetHolder, context, rowIndex, property.getHeadClazz(), headMap,extra);
        //判环Head配置中是否为LIST数据结构
        //如果为List则则读取extra中的列索引做记录
    }

    public static class MergeData{
        private int lastIndex;
        private Object data;

        public int getLastIndex() {
            return lastIndex;
        }

        public void setLastIndex(int lastIndex) {
            this.lastIndex = lastIndex;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
    /**
     * 构建合并的对象
     * @param cellDataMap
     * @param readSheetHolder
     * @param context
     * @param rowIndex
     * @param headClazz
     * @param headMap
     * @param cellExtraMap
     * @return
     */
    private MergeData buildMergeModel(Map<Integer, ReadCellData<?>> cellDataMap, ReadSheetHolder readSheetHolder,
                                   AnalysisContext context, int rowIndex, Class<?> headClazz, Map<Integer, Head> headMap, Map<Integer, CellExtra> cellExtraMap) {
        Object resultModel;
        try {
            resultModel = headClazz.newInstance();
        } catch (Exception e) {
            throw new ExcelDataConvertException(context.readRowHolder().getRowIndex(), 0,
                new ReadCellData<>(CellDataTypeEnum.EMPTY), null,
                "Can not instance class: " + headClazz.getName(), e);
        }
        BeanMap dataMap = BeanMapUtils.create(resultModel);
        MergeData mergeData = new MergeData();
        mergeData.setData(resultModel);
        //前一列index
        int firstColumnIndex =0;
        int mergeRowNum=0;
        for (Integer cellIndex : headMap.keySet()) {
            CellExtra extra = cellExtraMap.get(cellIndex);
            if (extra == null) {
                continue;
            }
            int num = extra.getLastRowIndex() - extra.getFirstRowIndex();
            if(mergeRowNum < num ){
                firstColumnIndex = extra.getFirstColumnIndex();
            }
            break;
        }
        mergeData.setLastIndex(rowIndex);
        for (Map.Entry<Integer, Head> entry : headMap.entrySet()) {
            int cellIndex = entry.getKey();
            Head head = entry.getValue();
            //如果当前head类型为List
            if(head.getType().equals("LIST")){
                //取出当前列的数据合并信息
                CellExtra extra = cellExtraMap.get(firstColumnIndex);
                List list = new ArrayList();
                int firstRowIndex = extra.getFirstRowIndex();
                int lastRowIndex = extra.getLastRowIndex();
                //如果当前行数据不在合并规则之内，
                //按当前行，只循环一次
                if (lastRowIndex < rowIndex) {
                    firstRowIndex = rowIndex;
                    lastRowIndex = rowIndex;
                }
                //循环行合并的行
                for(int i=firstRowIndex;i<=lastRowIndex;i++){
                    //上一行的合并规则
                    Map<Integer,CellExtra> nextCellExtraMap = cellExtraMap;
                    //当前行合并规则
                    if(extraMap.containsKey(i)){
                        nextCellExtraMap = extraMap.get(i);
                    }
                    MergeData listResult = this.buildMergeModel(dataMaps.get(i), readSheetHolder, context, i,
                        head.getCollectionClass(), head.getNextHead(), nextCellExtraMap);
                    list.add(listResult.getData());
                    i = listResult.getLastIndex();
                }
                mergeData.setLastIndex(lastRowIndex);
                dataMap.put(head.getFieldName(), list);
            }else{
                Object value = ConverterUtils.convertToJavaObject(cellDataMap.get(cellIndex), head.getField(),
                    ClassUtils.declaredExcelContentProperty(dataMap, headClazz,
                        head.getFieldName()), readSheetHolder.converterMap(), context, rowIndex, cellIndex);
                if (value != null) {
                    dataMap.put(head.getFieldName(), value);
                }
            }
        }
        return mergeData;
    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        System.out.println("有额外数据");
        Integer rowHeadNumber = context.readSheetHolder().getHeadRowNumber();
        if (extra.getRowIndex() <= rowHeadNumber) {
            return;
        }
        Map<Integer, CellExtra> cellMap = new TreeMap<>();
        if(extraMap.containsKey(extra.getRowIndex())){
            cellMap = extraMap.get(extra.getRowIndex());
        }
        cellMap.put(extra.getFirstColumnIndex(), extra);
        extraMap.put(extra.getRowIndex(), cellMap);
    }
}
