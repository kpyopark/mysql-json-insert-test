package com.elevenquest.jdbctest;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSource {
  private static HikariConfig config = new HikariConfig();
  private static HikariDataSource ds;

  static {
    String jdbcurl = System.getProperty("jdbc-url");
    String dbuser = System.getProperty("dbuser");
    String dbpass = System.getProperty("dbpass");

    config.setJdbcUrl(jdbcurl);
    config.setUsername(dbuser);
    config.setPassword(dbpass);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    ds = new HikariDataSource(config);
  }

  private DataSource() {
  }

  public static Connection getConnection() throws SQLException {
    return ds.getConnection();
  }
}
