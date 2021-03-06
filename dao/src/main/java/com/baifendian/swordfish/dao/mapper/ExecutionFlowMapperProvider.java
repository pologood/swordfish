/*
 * Copyright (C) 2017 Baifendian Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baifendian.swordfish.dao.mapper;

import com.baifendian.swordfish.dao.enums.NotifyType;
import com.baifendian.swordfish.dao.enums.ScheduleStatus;
import com.baifendian.swordfish.dao.mapper.utils.EnumFieldUtil;
import com.baifendian.swordfish.dao.enums.ExecType;
import com.baifendian.swordfish.dao.enums.FlowStatus;
import com.baifendian.swordfish.dao.enums.FlowType;
import com.baifendian.swordfish.dao.model.ExecutionFlow;
import com.baifendian.swordfish.dao.model.MaintainQuery;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.sun.tools.javac.comp.Flow;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.SQL;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * workflow 执行的信息操作 <p>
 *
 * @author : wenting.wang
 * @date : 2016年8月30日
 */

public class ExecutionFlowMapperProvider {

  public static final String TABLE_NAME = "execution_flows";

  List<Integer> flowTypes = new ArrayList<>();

  List<Integer> flowTypesNoLong = new ArrayList<>();

  public ExecutionFlowMapperProvider() {
    flowTypes.add(FlowType.LONG.getType());
    flowTypes.add(FlowType.SHORT.getType());
    flowTypes.add(FlowType.ETL.getType());
    flowTypesNoLong.add(FlowType.SHORT.getType());
    flowTypesNoLong.add(FlowType.ETL.getType());
  }

  public String insert(Map<String, Object> parameter) {
    return new SQL() {
      {
        INSERT_INTO(TABLE_NAME);
        VALUES("flow_id", "#{executionFlow.flowId}");
        VALUES("worker", "#{executionFlow.worker}");
        VALUES("status", EnumFieldUtil.genFieldStr("executionFlow.status", FlowStatus.class));
        VALUES("submit_user", "#{executionFlow.submitUserId}");
        VALUES("proxy_user", "#{executionFlow.proxyUser}");
        VALUES("submit_time", "#{executionFlow.submitTime}");
        VALUES("schedule_time", "#{executionFlow.scheduleTime}");
        VALUES("start_time", "#{executionFlow.startTime}");
        VALUES("end_time", "#{executionFlow.endTime}");
        VALUES("workflow_data", "#{executionFlow.workflowData}");
        VALUES("type", EnumFieldUtil.genFieldStr("executionFlow.type", ExecType.class));
        VALUES("max_try_times", "#{executionFlow.maxTryTimes}");
        VALUES("timeout", "#{executionFlow.timeout}");
        VALUES("user_defined_params", "#{executionFlow.userDefinedParams}");
        VALUES("queue", "#{executionFlow.queue}");
        VALUES("extras", "#{executionFlow.extras}");
        VALUES("notify_type", EnumFieldUtil.genFieldStr("executionFlow.notifyType", NotifyType.class));
        VALUES("notify_mails", "#{executionFlow.notifyMails}");
      }
    }.toString();
  }

  public String update(Map<String, Object> parameter) {
    ExecutionFlow executionFlow = (ExecutionFlow) parameter.get("executionFlow");
    return new SQL() {
      {
        UPDATE(TABLE_NAME);
        if (executionFlow.getStatus() != null) {
          SET("status = " + EnumFieldUtil.genFieldStr("executionFlow.status", FlowStatus.class));
        }
        if (executionFlow.getStartTime() != null) {
          SET("start_time = #{executionFlow.startTime}");
        }
        if (executionFlow.getEndTime() != null) {
          SET("end_time = #{executionFlow.endTime}");
        }
        if (executionFlow.getWorker() != null) {
          SET("worker = #{executionFlow.worker}");
        }
        if (executionFlow.getMaxTryTimes() != null) {
          SET("max_try_times = #{executionFlow.maxTryTimes}");
        }
        if (executionFlow.getTimeout() != null) {
          SET("timeout = #{executionFlow.timeout}");
        }
        WHERE("id = #{executionFlow.id}");
      }
    }.toString();
  }

  public String select(MaintainQuery maintainQuery) {
    StringBuilder sb = new StringBuilder();
    sb.append("select a.*, b.name as flow_name, c.name as submit_user_name ");
    sb.append("from execution_flows as a ");
    sb.append("inner join project_flows as b on a.flow_id = b.id and b.project_id = #{maintainQuery.projectId} ");
    sb.append("inner join user as c on a.submit_user = c.id where 1=1 ");
    if (maintainQuery.getMyself()) {
      sb.append("and a.submit_user = #{maintainQuery.userId} ");
    }
    if (maintainQuery.getStartTime() != null) {
      sb.append("and a.start_time >= #{maintainQuery.startTime} ");
    }
    if (maintainQuery.getEndTime() != null) {
      sb.append("and a.start_time <= #{maintainQuery.endTime} ");
    }
    if (maintainQuery.getExecId() != null) {
      sb.append("and a.id = #{maintainQuery.execId} ");
    }
    if (CollectionUtils.isNotEmpty(maintainQuery.getTaskStatus())) {
      sb.append("and a.status in (");
      int size = maintainQuery.getTaskStatus().size();
      String fm = EnumFieldUtil.genFieldSpecialStr("maintainQuery.taskStatus[{0}]", FlowStatus.class);
      MessageFormat mf = new MessageFormat(fm);
      for (int i = 0; i < size; i++) {
        sb.append(mf.format(new Object[]{i}));
        if (i < size - 1) {
          sb.append(",");
        }
      }
      sb.append(") ");
    }
    if (CollectionUtils.isNotEmpty(maintainQuery.getFlowTypes())) {
      List<Integer> types = new ArrayList<>();
      for (FlowType flowType : maintainQuery.getFlowTypes()) {
        types.add(flowType.getType());
      }
      sb.append("and b.type in (");
      sb.append(StringUtils.join(types, ","));
      sb.append(") ");
    }
    if (maintainQuery.getName() != null) {
      // sb.append("and b.name like '%${maintainQuery.name}%' ");
      sb.append("and b.name like CONCAT(CONCAT('%', #{maintainQuery.name}), '%') ");
    }
    int start = (maintainQuery.getStart() - 1) * maintainQuery.getLength();

    String limit = String.format("ORDER BY a.start_time desc LIMIT %s , %s", start, maintainQuery.getLength());
    sb.append(limit);
    return sb.toString();
  }

  public String selectAllNoFinishFlow() {
    return new SQL() {
      {
        SELECT("id, flow_id, worker, status ");
        FROM(TABLE_NAME);
        WHERE("status <=" + FlowStatus.RUNNING.getType());
      }
    }.toString();
  }

  public String selectNoFinishFlow(Map<String, Object> paramter) {
    return new SQL() {
      {
        SELECT("id, flow_id, worker ");
        FROM(TABLE_NAME);
        WHERE("status <=" + FlowStatus.RUNNING.getType());
        WHERE("worker = #{worker}");
      }
    }.toString();
  }

  public String selectCount(MaintainQuery maintainQuery) {

    StringBuilder sb = new StringBuilder();
    sb.append("select count(1) ");
    sb.append("from execution_flows as a ");
    sb.append("inner join project_flows as b on a.flow_id = b.id and b.project_id = #{maintainQuery.projectId} ");
    sb.append("inner join user as c on a.submit_user = c.id where 1=1 ");
    if (maintainQuery.getMyself()) {
      sb.append("and a.submit_user = #{maintainQuery.userId} ");
    }
    if (maintainQuery.getStartTime() != null) {
      sb.append("and a.start_time >= #{maintainQuery.startTime} ");
    }
    if (maintainQuery.getEndTime() != null) {
      sb.append("and a.start_time <= #{maintainQuery.endTime} ");
    }
    if (maintainQuery.getExecId() != null) {
      sb.append("and a.id = #{maintainQuery.execId} ");
    }
    if (maintainQuery.getTaskStatus() != null) {
      sb.append("and a.status in (");
      int size = maintainQuery.getTaskStatus().size();
      String fm = EnumFieldUtil.genFieldSpecialStr("maintainQuery.taskStatus[{0}]", FlowStatus.class);
      MessageFormat mf = new MessageFormat(fm);
      for (int i = 0; i < size; i++) {
        sb.append(mf.format(new Object[]{i}));
        if (i < size - 1) {
          sb.append(",");
        }
      }
      sb.append(") ");
    }
    if (CollectionUtils.isNotEmpty(maintainQuery.getFlowTypes())) {
      List<Integer> types = new ArrayList<>();
      for (FlowType flowType : maintainQuery.getFlowTypes()) {
        types.add(flowType.getType());
      }
      sb.append("and b.type in (");
      sb.append(StringUtils.join(types, ","));
      sb.append(") ");
    }
    if (maintainQuery.getName() != null) {
      sb.append("and b.name like CONCAT(CONCAT('%', #{maintainQuery.name}), '%') ");
    }
    return sb.toString();
  }

  public String selectByExecId(Map<String, Object> parameter) {
    String sql = new SQL() {
      {
        SELECT("a.*");
        SELECT("b.name as flow_name");
        SELECT("b.project_id as project_id");
        SELECT("b.owner as owner_id");
        SELECT("c.name as project_name");
        SELECT("u.name as submit_user_name");
        FROM("execution_flows a");
        INNER_JOIN("project_flows b on a.flow_id = b.id");
        INNER_JOIN("project c on b.project_id = c.id");
        INNER_JOIN("user u on a.submit_user = u.id");
        WHERE("a.id = #{execId}");
      }
    }.toString();

    return new SQL() {
      {
        SELECT("*");
        SELECT("u.name as owner_name");
        FROM("(" + sql + ") t");
        JOIN("user u on t.owner_id = u.id");
      }
    }.toString();
  }

  public String selectByFlowIdAndTimes(Map<String, Object> parameter) {
    StringBuilder sb = new StringBuilder();
    String inExpr = "(" + ExecType.DIRECT.ordinal() + "," + ExecType.COMPLEMENT_DATA.ordinal() + ")";
    sb.append("SELECT id, flow_id, worker, type, status, schedule_time FROM execution_flows WHERE flow_id = #{flowId} AND type IN " + inExpr + " AND ");
    sb.append("schedule_time = (SELECT MIN(schedule_time) FROM execution_flows WHERE flow_id = #{flowId} AND type IN" + inExpr
            + " AND schedule_time >= #{startDate} AND schedule_time < #{endDate})");

    return sb.toString();
  }

  public String selectByFlowIdAndTimesAndStatusLimit(Map<String, Object> parameter) {
    List<FlowStatus> flowStatuses = (List<FlowStatus>) parameter.get("status");

    List<String> workflowList = (List<String>) parameter.get("workflowList");

    List<String> workflowList2 = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(workflowList)){
      for (String workflow:workflowList){
        workflowList2.add("\""+workflow+"\"");
      }
    }


    List<String> flowStatusStrList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(flowStatuses)) {
      for (FlowStatus status : flowStatuses) {
        flowStatusStrList.add(status.getType().toString());
      }
    }

    String where = String.join(",", flowStatusStrList);

    String sql = new SQL() {
      {
        SELECT("e_f.*");
        SELECT("p_f.name as flow_name");
        SELECT("p.name as project_name");
        SELECT("u.name as owner");
        FROM("execution_flows e_f");
        JOIN("project_flows p_f on e_f.flow_id = p_f.id");
        JOIN("project p on p_f.project_id = p.id");
        JOIN("user u on p_f.owner = u.id");
        WHERE("p.name = #{projectName}");
        if (CollectionUtils.isNotEmpty(workflowList)) {
          WHERE("p_f.name in ("+String.join(",",workflowList2)+")");
        }
        WHERE("schedule_time >= #{startDate}");
        WHERE("schedule_time <= #{endDate}");
        if (CollectionUtils.isNotEmpty(flowStatuses)){
          WHERE("`status` in (" + where + ") ");
        }


      }
    }.toString();

    String sql2 = new SQL() {
      {
        SELECT("e_f.*");
        SELECT("u.name as submit_user_name");
        FROM("(" + sql + ") e_f");
        JOIN("user u on e_f.submit_user = u.id");
      }
    }.toString()+" order by schedule_time DESC limit #{start},#{limit}";
    return sql2;
  }

  public String sumByFlowIdAndTimesAndStatus(Map<String, Object> parameter) {
    List<FlowStatus> flowStatuses = (List<FlowStatus>) parameter.get("status");

    List<String> workflowList = (List<String>) parameter.get("workflowList");

    List<String> workflowList2 = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(workflowList)){
      for (String workflow:workflowList){
        workflowList2.add("\""+workflow+"\"");
      }
    }


    List<String> flowStatusStrList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(flowStatuses)) {
      for (FlowStatus status : flowStatuses) {
        flowStatusStrList.add(status.getType().toString());
      }
    }

    String where = String.join(",", flowStatusStrList);

    return new SQL() {
      {
        SELECT("count(0)");
        FROM("execution_flows e_f");
        JOIN("project_flows p_f on e_f.flow_id = p_f.id");
        JOIN("project p on p_f.project_id = p.id");
        WHERE("p.name = #{projectName}");
        if (CollectionUtils.isNotEmpty(workflowList)) {
          WHERE("p_f.name in ("+String.join(",",workflowList2)+")");
        }
        WHERE("schedule_time >= #{startDate}");
        WHERE("schedule_time < #{endDate}");
        if (CollectionUtils.isNotEmpty(flowStatuses)){
          WHERE("`status` in (" + where + ") ");
        }


      }
    }.toString();
  }

  public String selectByFlowIdAndTime(Map<String, Object> parameter) {
    StringBuilder sb = new StringBuilder();
    String inExpr = "(" + ExecType.DIRECT.ordinal() + "," + ExecType.COMPLEMENT_DATA.ordinal() + ")";
    sb.append("SELECT id, flow_id, worker, type, status, schedule_time FROM execution_flows WHERE flow_id = #{flowId} AND type IN " + inExpr + " AND ");
    sb.append("schedule_time = #{scheduleTime}");

    return sb.toString();
  }

  public String selectNewestExeFlow(Set<Integer> flowIds) {
    StringBuilder sb = new StringBuilder();
    sb.append("select a.id, a.flow_id, a.worker, a.status from execution_flows as a,");

    sb.append("(select max(id) as id from execution_flows ");
    sb.append("where flow_id in (0");
    for (Integer flowId : flowIds) {
      sb.append(",");
      sb.append(flowId);
    }
    sb.append(") ");
    sb.append("group by flow_id) as b ");
    sb.append("where a.id = b.id");
    return sb.toString();
  }

  public String deleteByExecId(Map<String, Object> parameter) {
    return new SQL() {
      {
        DELETE_FROM(TABLE_NAME);
        WHERE("id = #{execId}");
      }
    }.toString();
  }

  public String selectFlowStatus(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("a.status, count(a.status) as num");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id");
        WHERE("b.project_id = #{projectId}");
        WHERE("a.start_time >= #{queryDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("b.type in (" + StringUtils.join(flowTypes, ",") + ")");
        GROUP_BY("a.status");
      }
    }.toString();
  }

  public String selectUserFlowStatus(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("a.status, count(a.status) as num");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id");
        WHERE("b.project_id = #{projectId}");
        WHERE("a.submit_user = #{userId}");
        WHERE("a.start_time >= #{queryDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("b.type in (" + StringUtils.join(flowTypes, ",") + ")");
        GROUP_BY("a.status");
      }
    }.toString();
  }

  public String selectDayFlowStatus(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("a.status, count(a.status) as num, DATE_FORMAT(a. start_time,'%Y-%m-%d')as day");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id");
        WHERE("b.project_id = #{projectId}");
        WHERE("a.start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("a.start_time < #{endDate}");
        WHERE("b.type in (" + StringUtils.join(flowTypes, ",") + ")");
        GROUP_BY("a.status, day");
      }
    }.toString();
  }

  public String selectFlowHourAvgTime(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("DATE_FORMAT(start_time, '%k')as hour");
        SELECT("avg(UNIX_TIMESTAMP(end_time)-UNIX_TIMESTAMP(start_time)) as duration");
        SELECT("count(0) as num");
        FROM(TABLE_NAME);
        WHERE("flow_id = #{flowId}");
        WHERE("start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("start_time < #{endDate}");
        WHERE("end_time is not NULL");
        GROUP_BY("hour");
      }
    }.toString();
  }

  public String selectFlowDayAvgTime(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("DATE_FORMAT(start_time, '%Y-%m-%d')as day");
        SELECT("avg(UNIX_TIMESTAMP(end_time)-UNIX_TIMESTAMP(start_time)) as duration");
        SELECT("count(0) as num");
        FROM(TABLE_NAME);
        WHERE("flow_id = #{flowId}");
        WHERE("start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("start_time < #{endDate}");
        WHERE("end_time is not NULL");
        GROUP_BY("day");
      }
    }.toString();
  }

  public String selectHourFlowStatus(Map<String, Object> parameter) {
    return new SQL() {
      {
        SELECT("a.status, count(a.status) as num, DATE_FORMAT(start_time,'%k')as hour");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id");
        WHERE("b.project_id = #{projectId}");
        WHERE("a.start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("a.start_time < #{endDate}");
        WHERE("b.type in (" + StringUtils.join(flowTypes, ",") + ")");
        GROUP_BY("a.status, hour");
      }
    }.toString();
  }

  public String selectFlowTopTimes(Map<String, Object> parameter) {
    String sqlTemp = new SQL() {
      {
        SELECT("a.id, a.flow_id, a.status, a.submit_user, a.start_time, a.end_time");
        SELECT("IFNULL(UNIX_TIMESTAMP(a.end_time)-UNIX_TIMESTAMP(a.start_time)," + " UNIX_TIMESTAMP()-UNIX_TIMESTAMP(a.start_time)) as duration");
        SELECT("b.type as flow_type, b.name as flow_name");
        SELECT("c.name as submit_user_name");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id and b.project_id = #{projectId}");
        INNER_JOIN("user as c on a.submit_user = c.id");
        WHERE("a.start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("a.start_time < #{endDate}");
        WHERE("b.type in (" + StringUtils.join(flowTypesNoLong, ",") + ")");
        ORDER_BY("duration desc");
      }
    }.toString();

    String sql = String.format("%s LIMIT 0, #{num}", sqlTemp);
    return sql;
  }

  public String selectFlowErrorNum(Map<String, Object> parameter) {

    String sqlTemp = new SQL() {
      {
        SELECT("a.flow_id, count(a.flow_id) as num");
        SELECT("b.type as flow_type, b.name as flow_name");
        SELECT("c.name as submit_user_name");
        FROM("execution_flows as a");
        INNER_JOIN("project_flows as b on a.flow_id = b.id and b.project_id = #{projectId}");
        INNER_JOIN("user as c on a.submit_user = c.id");
        WHERE("a.start_time >= #{startDate}"); // should be >=, (add by
        // qifeng.dai)
        WHERE("a.start_time < #{endDate}");
        WHERE("a.status = " + FlowStatus.FAILED.getType());
        WHERE("b.type in (" + StringUtils.join(flowTypes, ",") + ")");
        GROUP_BY("a.flow_id");
        ORDER_BY("num desc");
      }
    }.toString();

    String sql = String.format("%s LIMIT 0, #{num}", sqlTemp);
    return sql;
  }

  public String selectStateByProject(Map<String,Object> parameter) {
    return new SQL(){
      {
        SELECT("str_to_date(DATE_FORMAT(e_f.schedule_time,'%Y%m%d'),'%Y%m%d') as day,\n" +
                "SUM(case e_f.status when 0 then 1 else 0 end) as INIT,\n" +
                "SUM(case e_f.status when 1 then 1 else 0 end) as WAITING_DEP,\n" +
                "SUM(case e_f.status when 2 then 1 else 0 end) as WAITING_RES,\n" +
                "SUM(case e_f.status when 3 then 1 else 0 end) as RUNNING,\n" +
                "SUM(case e_f.status when 4 then 1 else 0 end) as SUCCESS,\n" +
                "SUM(case e_f.status when 5 then 1 else 0 end) as `KILL`,\n" +
                "SUM(case e_f.status when 6 then 1 else 0 end) as `FAILED`,\n" +
                "SUM(case e_f.status when 7 then 1 else 0 end) as `DEP_FAILED`");
        FROM("execution_flows e_f");
        JOIN("project_flows p_f on e_f.flow_id = p_f.id");
        WHERE("e_f.schedule_time >= #{startDate} AND e_f.schedule_time <= #{endDate}");
        WHERE("p_f.project_id = #{projectId}");
        GROUP_BY("day");
      }
    }.toString();
  }

  public String selectConsumesByProject(Map<String,Object> parameter) {
    return new SQL(){
      {
        SELECT("timestampdiff(SECOND,start_time,end_time) as consume");
        SELECT("e_f.*");
        SELECT("p_f.name as flow_name");
        SELECT("u.name as owner_name");
        FROM("execution_flows e_f");
        JOIN("project_flows p_f on e_f.flow_id = p_f.id");
        JOIN("user u on p_f.owner = u.id");
        WHERE("str_to_date(DATE_FORMAT(e_f.schedule_time,'%Y%m%d'),'%Y%m%d') = #{date}");
        ORDER_BY("consume DESC");
      }
    }.toString()+" limit #{top}";
  }

  public String selectErrorsByProject(Map<String,Object> parameter) {
    return new SQL(){
      {
        SELECT("count(0) as times");
        SELECT("p_f.name as flow_name");
        SELECT("u.name as owner_name");
        SELECT("p.name as projectName");
        FROM("execution_flows e_f");
        JOIN("project_flows p_f on e_f.flow_id = p_f.id");
        JOIN("user u on p_f.owner = u.id");
        JOIN("project p on p_f.project_id = p.id");
        WHERE("str_to_date(DATE_FORMAT(e_f.schedule_time,'%Y%m%d'),'%Y%m%d') = #{date}");
        WHERE("e_f.status in ("+FlowStatus.FAILED.getType()+","+FlowStatus.DEP_FAILED.getType()+")");
        GROUP_BY("e_f.flow_id");
        ORDER_BY("times DESC");
      }
    }.toString()+" limit #{top}";
  }
}
