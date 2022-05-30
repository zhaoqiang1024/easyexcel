package com.alibaba.excel.metadata.property;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import com.alibaba.excel.annotation.ExcelCollection;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.enums.HeadKindEnum;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.Holder;
import com.alibaba.excel.util.ClassUtils;
import com.alibaba.excel.util.FieldUtils;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.excel.write.metadata.holder.AbstractWriteHolder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the header attribute of excel
 *
 * @author jipengfei
 */
@Getter
@Setter
@EqualsAndHashCode
public class ExcelHeadProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelHeadProperty.class);
    /**
     * Custom class
     */
    private Class<?> headClazz;
    /**
     * The types of head
     */
    private HeadKindEnum headKind;
    /**
     * The number of rows in the line with the most rows
     */
    private int headRowNumber;
    /**
     * Configuration header information
     */
    private Map<Integer, Head> headMap;
    /**
     * Fields ignored
     */
    private Map<String, Field> ignoreMap;

    public ExcelHeadProperty(Holder holder, Class<?> headClazz, List<List<String>> head) {
        this.headClazz = headClazz;
        headMap = new TreeMap<>();
        ignoreMap = MapUtils.newHashMap();
        headKind = HeadKindEnum.NONE;
        headRowNumber = 0;
        if (head != null && !head.isEmpty()) {
            int headIndex = 0;
            for (int i = 0; i < head.size(); i++) {
                if (holder instanceof AbstractWriteHolder) {
                    if (((AbstractWriteHolder)holder).ignore(null, i)) {
                        continue;
                    }
                }
                headMap.put(headIndex, new Head(headIndex, null, null,null, head.get(i), Boolean.FALSE, Boolean.TRUE));
                headIndex++;
            }
            headKind = HeadKindEnum.STRING;
        }
        // convert headClazz to head
        initColumnProperties(holder);

        initHeadRowNumber();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The initialization sheet/table 'ExcelHeadProperty' is complete , head kind is {}", headKind);
        }
    }

    private void initHeadRowNumber() {
        headRowNumber = 0;
        for (Head head : headMap.values()) {
            List<String> list = head.getHeadNameList();
            if (list != null && list.size() > headRowNumber) {
                headRowNumber = list.size();
            }
        }
        for (Head head : headMap.values()) {
            List<String> list = head.getHeadNameList();
            if (list != null && !list.isEmpty() && list.size() < headRowNumber) {
                int lack = headRowNumber - list.size();
                int last = list.size() - 1;
                for (int i = 0; i < lack; i++) {
                    list.add(list.get(last));
                }
            }
        }
    }

    private void initColumnProperties(Holder holder) {
        if (headClazz == null) {
            return;
        }
        // Declared fields
        Map<Integer, Field> sortedAllFiledMap = MapUtils.newTreeMap();
        Map<Integer, Field> indexFiledMap = MapUtils.newTreeMap();

        boolean needIgnore = (holder instanceof AbstractWriteHolder) && (
            !CollectionUtils.isEmpty(((AbstractWriteHolder)holder).getExcludeColumnFieldNames()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getExcludeColumnIndexes()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getIncludeColumnFieldNames()) || !CollectionUtils
                .isEmpty(((AbstractWriteHolder)holder).getIncludeColumnIndexes()));

        //将class转换为Field，仅第一层
        Head parent = null;
        initHead(0,headClazz, parent, sortedAllFiledMap, indexFiledMap, needIgnore, holder);
        headKind = HeadKindEnum.CLASS;
    }

    private int initHead(int lastIndex,Class<?> headClazz, Head parentHead, Map<Integer, Field> sortedAllFiledMap, Map<Integer, Field> indexFiledMap, boolean needIgnore, Holder holder) {
        ClassUtils.declaredFields(headClazz, sortedAllFiledMap, indexFiledMap, ignoreMap, needIgnore, holder);
        Map<Integer, Head> newHeadMap = MapUtils.newTreeMap();
        Iterator<Map.Entry<Integer,Field>> iterator =sortedAllFiledMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Field> entry = iterator.next();
            if(lastIndex<entry.getKey()){
                lastIndex = entry.getKey();
            }
            int index = lastIndex;
            Head head = initOneColumnProperty(index, entry.getValue(), indexFiledMap.containsKey(entry.getKey()));
            newHeadMap.put(index, head);
            //输入holder不处理list结构
            if (holder instanceof AbstractWriteHolder) {
                continue;
            }
            //查询是否为list类型字段
            Class<?> collectionClass = this.getCollectionClass(entry.getValue());
            if (collectionClass != null) {
                head.setType("LIST");
                lastIndex = initHead(lastIndex,collectionClass, head, MapUtils.newTreeMap(), MapUtils.newTreeMap(), false, holder);
            }
            if (iterator.hasNext()) {
                lastIndex++;
            }
        }
        if (parentHead != null) {
            parentHead.setNextHead(newHeadMap);
        } else {
            headMap = newHeadMap;
        }
        return lastIndex;
    }


    /**
     * Initialization column property
     *
     * @param index
     * @param field
     * @param forceIndex
     * @return Ignore current field
     */
    private Head initOneColumnProperty(int index, Field field, Boolean forceIndex) {
        ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
        List<String> tmpHeadList = new ArrayList<String>();
        String fieldName = FieldUtils.resolveCglibFieldName(field);
        //无指定模板名称
        boolean notForceName = excelProperty == null || excelProperty.value().length <= 0
            || (excelProperty.value().length == 1 && StringUtils.isEmpty((excelProperty.value())[0]));
        if (headMap.containsKey(index)) {
            tmpHeadList.addAll(headMap.get(index).getHeadNameList());
        } else {
            if (notForceName) {
                tmpHeadList.add(fieldName);
            } else {
                Collections.addAll(tmpHeadList, excelProperty.value());
            }
        }
        Head head = new Head(index, field,getCollectionClass(field), fieldName, tmpHeadList, forceIndex, !notForceName);
        return head;
    }

    private Class<?> getCollectionClass(Field field) {
        ExcelCollection collection = field.getAnnotation(ExcelCollection.class);
        boolean listField = field.getClass().isAssignableFrom(List.class);
        Class<?> collectionClass = null;
        if (collection != null || listField) {
            Type gt = field.getGenericType();
            ParameterizedType pt = (ParameterizedType) gt;
            collectionClass = (Class) pt.getActualTypeArguments()[0];
        }
        return collectionClass;
    }

    public boolean hasHead() {
        return headKind != HeadKindEnum.NONE;
    }

}
