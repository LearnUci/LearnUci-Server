package org.gbc.luci.servlet;

import static org.gbc.luci.servlet.GuiceServletModule.require;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.gbc.luci.datastore.MapPoint;
import org.gbc.luci.servlet.GuiceServletModule.ParameterNotFoundException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class AddPointServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      String point = require(req, "point");
      JsonParser parser = new JsonParser();
      JsonObject obj = (JsonObject) parser.parse(point);
      MapPoint mapPoint = new MapPoint.Builder()
          .setName(stringOrNull(obj, "name"))
          .setLat(stringOrNull(obj, "lat"))
          .setLng(stringOrNull(obj, "lng"))
          .setAbbr(stringOrNull(obj, "abbr"))
          .setType(stringOrNull(obj, "type"))
          .setImg(byteOrNull(obj, "img"))
          .setDescription(stringOrNull(obj, "desc"))
          .build();
      mapPoint.save();
      resp.setContentType("text/plain");
      resp.getWriter().println("ok");
    } catch (JsonSyntaxException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } catch (ParameterNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
  
  private String stringOrNull(JsonObject obj, String property) {
    if (obj.has(property)) {
      return obj.get(property).getAsString();
    }
    return null;
  }
  
  private byte[] byteOrNull(JsonObject obj, String property) {
    if (obj.has(property)) {
      return Base64.decodeBase64(obj.get(property).getAsString());
    }
    return null;
  }
}
