package org.gbc.luci.servlet;

import static org.gbc.luci.servlet.GuiceServletModule.require;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gbc.luci.datastore.MapPoint;
import org.gbc.luci.servlet.GuiceServletModule.ParameterNotFoundException;

import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class DeletePointServlet extends HttpServlet {
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      String key = require(req, "key");
      MapPoint.deleteAll(key);
    } catch (ParameterNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
