package org.gbc.luci.servlet;

import static org.gbc.luci.servlet.GuiceServletModule.require;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.gbc.luci.datastore.Tour;
import org.gbc.luci.servlet.GuiceServletModule.ParameterNotFoundException;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class AddTourServlet extends HttpServlet  {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      JsonParser parser = new JsonParser();
      String name = require(req, "name");
      String desc = require(req, "desc");
      JsonArray points = (JsonArray) parser.parse(require(req, "points"));
      byte[] bytes = Base64.decodeBase64(require(req, "img"));
      new Tour.Builder()
          .setName(name)
          .setDesc(desc)
          .setPoints(points)
          .setImage(bytes)
          .build().save();
      resp.getWriter().println("OK");
    } catch (ParameterNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
