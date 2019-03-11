/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.postgresql;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@SuppressWarnings("serial")
@WebServlet(name = "PostgreSQL",
    description = "PostgreSQL: Write timestamps of visitors to PostgreSQL",
    urlPatterns = "/")
public class PostgreSqlServlet extends HttpServlet {


  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      ServletException {

    final String createTableSql = "CREATE TABLE IF NOT EXISTS visits ( "
        + "visit_id SERIAL NOT NULL, ts timestamp NOT NULL, "
        + "PRIMARY KEY (visit_id) );";
    final String createVisitSql = "INSERT INTO visits (ts) VALUES (?);";
    final String selectSql = "SELECT ts FROM visits ORDER BY ts DESC "
        + "LIMIT 10;";

    String path = req.getRequestURI();
    if (path.startsWith("/favicon.ico")) {
      return; // ignore the request for favicon.ico
    }

    PrintWriter out = resp.getWriter();
    resp.setContentType("text/plain");

    Stopwatch stopwatch = Stopwatch.createStarted();
    // Extract the pool from the Servlet Context, reusing the one that was created
    // in the ContextListener when the application was started
    DataSource pool = (DataSource) req.getServletContext().getAttribute("my-pool");
    try (Connection conn = pool.getConnection()) {
      PreparedStatement statementCreateVisit = conn.prepareStatement(createVisitSql);
      conn.createStatement().executeUpdate(createTableSql);
      statementCreateVisit.setTimestamp(1, new Timestamp(new Date().getTime()));
      statementCreateVisit.executeUpdate();

      try (ResultSet rs = conn.prepareStatement(selectSql).executeQuery()) {
        stopwatch.stop();
        out.print("Last 10 visits:\n");
        while (rs.next()) {
          String timeStamp = rs.getString("ts");
          out.println("Visited at time: " + timeStamp);
        }
      }
    } catch (SQLException e) {
      throw new ServletException("SQL error", e);
    }
    out.println("Query time (ms):" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }
}
