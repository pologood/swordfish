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
package com.baifendian.swordfish.common.datasource.hbase;

import com.baifendian.swordfish.common.datasource.DataSourceHandler;
import com.baifendian.swordfish.dao.enums.DbType;
import com.baifendian.swordfish.dao.utils.json.JsonUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HBaseHandler implements DataSourceHandler {

  private static final Logger logger = LoggerFactory.getLogger(HBaseHandler.class);

  private DbType dbType;

  private HBaseParam param;

  public HBaseHandler(DbType dbType, String paramStr){
    param = JsonUtil.parseObject(paramStr, HBaseParam.class);
  }

  public void isConnectable() throws IOException {
    Connection con = null;
    try{
      Configuration config = HBaseConfiguration.create();
      config.set("hbase.zookeeper.quorum", param.getZkQuorum());
      if(!StringUtils.isEmpty(param.getZkZnodeParent())){
        config.set("zookeeper.znode.parent", param.getZkZnodeParent());
      }
      if(param.getZkPort() != null && param.getZkPort() != 0 ){
        config.set("hbase.zookeeper.property.clientPort", param.getZkPort().toString());
      }
      con = ConnectionFactory.createConnection(config);
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
