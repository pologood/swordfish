/*
 * Create Author  : dsfan
 * Create Date    : 2016年10月25日
 * File Name      : WorkerServiceImpl.java
 */

package com.baifendian.swordfish.execserver.service;

import com.baifendian.swordfish.dao.DaoFactory;
import com.baifendian.swordfish.dao.FlowDao;
import com.baifendian.swordfish.dao.mysql.enums.FlowStatus;
import com.baifendian.swordfish.dao.mysql.model.ExecutionFlow;
import com.baifendian.swordfish.dao.mysql.model.Schedule;
import com.baifendian.swordfish.execserver.FlowRunnerManager;
import com.baifendian.swordfish.execserver.result.ResultHelper;
import com.baifendian.swordfish.execserver.rpc.IFace;
import com.baifendian.swordfish.execserver.rpc.RetInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * ExecService 实现
 * <p>
 * 
 * @author : dsfan
 * @date : 2016年10月25日
 */
public class ExecServiceImpl implements IFace {

    /** LOGGER */
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /** {@link FlowDao} */
    private final FlowDao flowDao;

    /** {@link FlowRunnerManager} */
    private final FlowRunnerManager flowRunnerManager;

    /**
     * constructor
     */
    public ExecServiceImpl() {
        this.flowDao = DaoFactory.getDaoInstance(FlowDao.class);
        this.flowRunnerManager = new FlowRunnerManager();
    }

    @Override
    public RetInfo execFlow(int projectId, long execId, String flowType) throws TException {
        if (StringUtils.isEmpty(flowType)) {
            return ResultHelper.createErrorResult("flowType 参数不能为空");
        }

        try {
            // 查询 ExecutionFlow
            ExecutionFlow executionFlow = flowDao.queryExecutionFlow(execId);
            if (executionFlow == null) {
                return ResultHelper.createErrorResult("execId 对应的记录不存在");
            }

            // 更新状态为 RUNNING
            flowDao.updateExecutionFlowStatus(execId, FlowStatus.RUNNING);

            // 提交任务运行
            flowRunnerManager.submitFlow(executionFlow);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return ResultHelper.createErrorResult(e.getMessage());
        }
        return ResultHelper.SUCCESS;
    }

    @Override
    public RetInfo scheduleExecFlow(int projectId, long execId, String flowType, long scheduleDate) throws TException {
        if (StringUtils.isEmpty(flowType)) {
            return ResultHelper.createErrorResult("flowType 参数不能为空");
        }

        try {
            // 查询 ExecutionFlow
            ExecutionFlow executionFlow = flowDao.queryExecutionFlow(execId);
            if (executionFlow == null) {
                return ResultHelper.createErrorResult("execId 对应的记录不存在");
            }

            // 查询 Schedule
            Schedule schedule = flowDao.querySchedule(executionFlow.getFlowId());
            if (schedule == null) {
                return ResultHelper.createErrorResult("对应的调度信息不存在");
            }

            // 更新状态为 RUNNING
            flowDao.updateExecutionFlowStatus(execId, FlowStatus.RUNNING);

            // 提交任务运行
            flowRunnerManager.submitFlow(executionFlow, schedule, new Date(scheduleDate));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return ResultHelper.createErrorResult(e.getMessage());
        }
        return ResultHelper.SUCCESS;
    }

    /**
     * 销毁资源
     * <p>
     */
    public void destory() {
        flowRunnerManager.destroy();
    }

    public static void main(String[] args) throws TException {
        ExecServiceImpl execService = new ExecServiceImpl();
        execService.scheduleExecFlow(1,1,"tt", 1111111111);
    }

}
