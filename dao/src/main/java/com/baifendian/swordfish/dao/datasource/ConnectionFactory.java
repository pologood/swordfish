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
package com.baifendian.swordfish.dao.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * author: smile8
 * date:   2017/3/16
 * desc:
 */
public class ConnectionFactory {
  private static Logger logger = LoggerFactory.getLogger(ConnectionFactory.class.getName());

  private static final Properties PROPERTIES = new Properties();

  private static SqlSessionFactory sqlSessionFactory;

  static {
    try {
      File dataSourceFile = ResourceUtils.getFile("classpath:dao/data_source.properties");
      InputStream is = new FileInputStream(dataSourceFile);
      PROPERTIES.load(is);
    } catch (IOException e) {
      logger.error("Catch an exception", e);
    }
  }

  /**
   * 得到数据源
   *
   * @return
   */
  public static DataSource getDataSource() {
    DruidDataSource druidDataSource = new DruidDataSource();

    druidDataSource.setDriverClassName(PROPERTIES.getProperty("driver-class-name"));
    druidDataSource.setUrl(PROPERTIES.getProperty("url"));
    druidDataSource.setUsername(PROPERTIES.getProperty("username"));
    druidDataSource.setPassword(PROPERTIES.getProperty("password"));
    druidDataSource.setInitialSize(Integer.parseInt(PROPERTIES.getProperty("initialSize")));
    druidDataSource.setMinIdle(Integer.parseInt(PROPERTIES.getProperty("minIdle")));
    druidDataSource.setMaxActive(Integer.parseInt(PROPERTIES.getProperty("maxActive")));
    druidDataSource.setMaxWait(Integer.parseInt(PROPERTIES.getProperty("maxWait")));
    druidDataSource.setTimeBetweenEvictionRunsMillis(Long.parseLong(PROPERTIES.getProperty("timeBetweenEvictionRunsMillis")));
    druidDataSource.setMinEvictableIdleTimeMillis(Long.parseLong(PROPERTIES.getProperty("minEvictableIdleTimeMillis")));
    druidDataSource.setValidationQuery(PROPERTIES.getProperty("validationQuery"));
    druidDataSource.setTestWhileIdle(Boolean.parseBoolean(PROPERTIES.getProperty("testWhileIdle")));
    druidDataSource.setTestOnBorrow(Boolean.parseBoolean(PROPERTIES.getProperty("testOnBorrow")));
    druidDataSource.setTestOnReturn(Boolean.parseBoolean(PROPERTIES.getProperty("testOnReturn")));
    druidDataSource.setPoolPreparedStatements(Boolean.parseBoolean(PROPERTIES.getProperty("poolPreparedStatements")));
    druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(Integer.parseInt(PROPERTIES.getProperty("maxPoolPreparedStatementPerConnectionSize")));

    return druidDataSource;
  }

  /**
   * 构建 sql session factory
   *
   * @return
   */
  public static SqlSessionFactory getSqlSessionFactory() {
    if (sqlSessionFactory == null) {
      synchronized (ConnectionFactory.class) {
        if (sqlSessionFactory == null) {
          DataSource dataSource = getDataSource();
          TransactionFactory transactionFactory = new JdbcTransactionFactory();

          Environment environment = new Environment("development", transactionFactory, dataSource);

          Configuration configuration = new Configuration(environment);
          configuration.setLazyLoadingEnabled(true);
          configuration.addMappers("com.baifendian.mapper");

          SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
          sqlSessionFactory = builder.build(configuration);
        }
      }
    }

    return sqlSessionFactory;
  }
}