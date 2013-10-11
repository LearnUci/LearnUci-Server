package org.gbc.luci.servlet;

import static org.gbc.luci.servlet.GuiceServletModule.require;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gbc.luci.datastore.MapPoint;
import org.gbc.luci.servlet.GuiceServletModule.ParameterNotFoundException;

import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.JsonArray;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class QueryPointServlet extends HttpServlet {
  // Abitrary designated weight to the type location, some are more preferred
  private static final Map<String, Float> typeFactors = new HashMap<String, Float>() {{
    put("building", 1f); // Buildings on campus
    put("restaurant", 1f); // Places to eat
    put("default", .8f); // Generally labs of some sort
    put("parkinglot", .75f); // Parking lots
    put("sportvenue", .7f); // Sport venue locations
    put("arts", .7f); // Art/theatre locations
    put("house", .6f); // residence
    put("bus", .5f); // bus stops
    put("zw", .3f); // zot wheels
    put("phone", .25f); // phones
  }};

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      JsonArray respArray = new JsonArray();
      
      String action = require(req, "action");
      String type = require(req, "type");
      String value = require(req, "value");
      
      // Only support specific lookups for now
      if (action.equals("MATCH")) {
        if (type.equals("id")) {
          respArray.add(MapPoint.load(Long.parseLong(value)).serialize());
        } else {
          respArray.add(
              getOnly(MapPoint.loadByFilter(new FilterPredicate(type, FilterOperator.EQUAL, value)))
              .serialize());
        }
      } else if (action.equals("SEARCH")) {
        PriorityQueue<QueryPair> pq = new PriorityQueue<QueryPair>(10, new Comparator<QueryPair>() {
          @Override
          public int compare(QueryPair p1, QueryPair p2) {
            return (int) (1000000 * (p2.matchFactor - p1.matchFactor));
          }
        });
        // After we get a list of points that match the query, run through matching filter to 
        // sort them, then return
        for (MapPoint point : MapPoint.load(value)) {
          pq.add(getMatchFactor(point, value));
        }
        for (int i = 0; i < 10; i ++) {
          QueryPair p = pq.poll();
          if (p == null || p.matchFactor <= .01f) {
            break;
          }
          respArray.add(p.point.serialize());
        }
      } else {
        throw new InvalidQueryActionException("Invalid action " + action);
      }
      resp.getWriter().println(respArray.toString());
    } catch (ParameterNotFoundException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } catch (InvalidQueryActionException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
  
  private <T> T getOnly(Iterable<T> iterable) {
    Iterator<T> iterator = iterable.iterator();
    T value = iterator.next();
    if (iterator.hasNext()) {
      throw new IllegalArgumentException("Expected only one element");
    }
    return value;
  }
  
  private static class InvalidQueryActionException extends Exception {
    public InvalidQueryActionException(String message) {
      super(message);
    }
  }
  
  // Something I made in 5 minutes, someone make it better
  private QueryPair getMatchFactor(MapPoint point, String query) {
    String[] parts = query.toLowerCase().split(" ");
    Set<String> names = new HashSet<String>();
    Set<String> abbrs = new HashSet<String>();
    Set<String> visitedNames = new HashSet<String>();
    Set<String> visitedAbbrs = new HashSet<String>();
    for (String part : point.getName().toLowerCase().split(" ")) {
      names.add(part);
    }
    for (String part : point.getAbbr().toLowerCase().split(" ")) {
      abbrs.add(part);
    }
    int nameSize = names.size();
    int abbrSize = abbrs.size();
    int queryHitCount = 0;
    float nameChunk = 0;
    float abbrChunk = 0;
    for (String part : parts) {
      boolean hit = false;
      for (String token : names) {
        if (visitedNames.contains(token)) {
          continue;
        }
        if (token.indexOf(part) >= 0) {
          float factor = part.length() / (float) token.length();
          if (factor > .5f) {
            nameChunk += factor;
            visitedNames.add(token);
            hit = true;
          }
        }
      }
      if (hit) {
        queryHitCount++;
        continue;
      }
      for (String token : abbrs) {
        if (visitedNames.contains(token)) {
          continue;
        }
        if (token.indexOf(part) >= 0) {
          float factor = part.length() / (float) token.length();
          if (factor > .3f) {
            abbrChunk += part.length() / (float) token.length();
            visitedAbbrs.add(token);
            hit = true;
          }
        }
      }
      if (hit) {
        queryHitCount++;
      }
    }
    // This does something...
    QueryPair pair = new QueryPair();
    pair.point = point;
    pair.matchFactor = ((float) queryHitCount / parts.length) * typeFactors.get(point.getType())
        * ((nameChunk * ((float) visitedNames.size() / nameSize))
        + (10.0f * abbrChunk * ((float) visitedAbbrs.size() / abbrSize)));
    return pair;
  }
  
  private static class QueryPair {
    public float matchFactor;
    public MapPoint point;
  }
}
