package com.alibaba.druid.pool.vendor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.alibaba.druid.logging.Log;
import com.alibaba.druid.logging.LogFactory;
import com.alibaba.druid.pool.ValidConnectionChecker;
import com.alibaba.druid.util.DruidLoaderUtils;
import com.alibaba.druid.util.JdbcUtils;

public class MySqlValidConnectionChecker implements ValidConnectionChecker, Serializable {

    private static final long serialVersionUID    = 1L;
    private static final Log  LOG                 = LogFactory.getLog(MySqlValidConnectionChecker.class);

    private Class<?>          clazz;
    private Method            ping;
    private boolean           driverHasPingMethod = false;

    public MySqlValidConnectionChecker(){
        try {
            clazz = DruidLoaderUtils.loadClass("com.mysql.jdbc.Connection");
            ping = clazz.getMethod("ping");
            if (ping != null) {
                driverHasPingMethod = true;
            }
        } catch (Exception e) {
            LOG.warn("Cannot resolve com.mysq.jdbc.Connection.ping method.  Will use 'SELECT 1' instead.", e);
        }
    }

    public boolean isValidConnection(Connection c) {
        Connection conn = null;
        try {
            conn = (Connection) c.unwrap(clazz);
        } catch (SQLException e) {
            LOG.warn("Unexpected error in ping", e);
            return false;
        }

        // if there is a ping method then use it, otherwise just use a 'SELECT 1' statement
        if (conn != null && driverHasPingMethod) {
            try {
                ping.invoke(c);
                return true;
            } catch (Exception e) {
                LOG.warn("Unexpected error in ping", e);
                return false;
            }

        }

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.createStatement();
            rs = stmt.executeQuery("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        } catch (Exception e) {
            LOG.warn("Unexpected error in ping", e);
            return false;
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }

    }

}
