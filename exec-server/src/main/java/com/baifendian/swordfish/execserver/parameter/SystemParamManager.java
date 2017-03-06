/*
 * Create Author  : dsfan
 * Create Date    : 2016年11月25日
 * File Name      : SystemParam.java
 */

package com.baifendian.swordfish.execserver.parameter;

import com.baifendian.swordfish.common.utils.BFDDateUtils;
import com.baifendian.swordfish.dao.hadoop.hdfs.HdfsPathManager;
import com.baifendian.swordfish.dao.mysql.enums.FlowRunType;
import com.baifendian.swordfish.dao.mysql.model.ExecutionFlow;
import org.apache.commons.lang.time.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统参数管理
 * <p>
 * 
 * @author : dsfan
 * @date : 2016年11月25日
 */
public class SystemParamManager {

    /** yyyyMMdd */
    public static final String DATE_FORMAT = "yyyyMMdd";

    /** yyyyMMddHHmmss */
    public static final String TIME_FORMAT = "yyyyMMddHHmmss";

    /** 日常调度实例定时的定时时间日期的前一天，格式为 yyyyMMdd */
    private static final String BIZ_DATE = "dw.system.bizdate";

    /** 日常调度实例定时的定时时间日期，格式为 yyyymmdd，取值为 ${dw.system.bizdate} + 1 */
    private static final String BIZ_CUR_DATE = "dw.system.bizcurdate";

    /** ${dw.system.cyctime}，格式 yyyyMMddHHmmss，表示的是日常调度实例定时时间（年月日时分秒） */
    public static final String CYC_TIME = "dw.system.cyctime";

    /** 调度时刻的时间，格式为 yyyyMMddHHmmss */
    private static final String RUN_TIME = "dw.system.runtime";

    /** 当前项目的 HDFS 路径 */
    private static final String FILES = "dw.system.files";

    /** 当前 workflow 的名称 */
    private static final String FLOWNAME = "dw.system.flowname";

    /**
     * 构建系统参数值
     * <p>
     *
     * @param executionFlow
     * @param scheduleDate
     * @param addDate
     * @return 系统参数
     */
    public static Map<String, String> buildSystemParam(ExecutionFlow executionFlow, Date scheduleDate, Date addDate) {
        FlowRunType runType = executionFlow.getType();
        Date bizDate;
        Date execStartTime = new Date(executionFlow.getStartTime()*1000);
        switch (runType) {
            case DIRECT_RUN:
                bizDate = DateUtils.addDays(execStartTime, -1); // 运行日期的前一天
                break;

            case DISPATCH:
                bizDate = DateUtils.addDays(scheduleDate, -1); // 调度日期的前一天
                break;

            case ADD_DATA:
                bizDate = addDate; // 补数据的当天
                break;

            default:
                bizDate = DateUtils.addDays(execStartTime, -1); // 运行日期的前一天
        }

        Date bizCurDate = DateUtils.addDays(bizDate, 1); // bizDate + 1 天
        Date runTime = execStartTime;

        Map<String, String> valueMap = new HashMap<>();

        valueMap.put(BIZ_DATE, formatDate(bizDate));
        valueMap.put(BIZ_CUR_DATE, formatDate(bizCurDate));
        valueMap.put(CYC_TIME, formatTime(bizCurDate));
        valueMap.put(RUN_TIME, formatTime(runTime));
        valueMap.put(FILES, HdfsPathManager.genNodeHdfsPath(executionFlow.getProjectName()));
        valueMap.put(FLOWNAME, executionFlow.getFlowName());

        return valueMap;
    }

    /**
     * 格式化日期字符串（格式："yyyyMMdd"）
     * <p>
     *
     * @param date
     * @return 日期字符串
     */
    private static String formatDate(Date date) {
        return BFDDateUtils.format(date, DATE_FORMAT);
    }

    /**
     * 格式化时间字符串（格式："yyyyMMddHHmmss"）
     * <p>
     *
     * @param date
     * @return 时间字符串
     */
    private static String formatTime(Date date) {
        return BFDDateUtils.format(date, TIME_FORMAT);
    }
}
