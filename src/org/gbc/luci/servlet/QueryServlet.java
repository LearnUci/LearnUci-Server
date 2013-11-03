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
import org.gbc.luci.datastore.Tour;
import org.gbc.luci.servlet.GuiceServletModule.ParameterNotFoundException;

import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;

@SuppressWarnings("serial")
@Singleton
public class QueryServlet extends HttpServlet {
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
      // Only support specific lookups for now
      if (action.equals("MATCH")) {
        String value = require(req, "value");
        String type = require(req, "type");
        if (type.equals("id")) {
          respArray.add(MapPoint.load(Long.parseLong(value)).serialize());
        } else {
          respArray.add(
              getOnly(MapPoint.loadByFilter(new FilterPredicate(type, FilterOperator.EQUAL, value)))
              .serialize());
        }
      } else if (action.equals("SEARCH")) {
        String value = require(req, "value");
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
      } else if (action.equals("TOURS")) {
        for (Tour t : Tour.loadAll()) {
          respArray.add(t.serialize());
        }
      } else if (action.equals("EXPAND_TOUR")) {
        String value = require(req, "value");
        Tour t = Tour.load(Long.parseLong(value));
        for (JsonElement e : t.getPoints()) {
          respArray.add(MapPoint.load(e.getAsLong()).serialize());
        }
      } else if (action.equals("PROXIMITY")) {
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(require(req, "value"));
        final float latitude = obj.get("latitude").getAsFloat();
        final float longitude = obj.get("longitude").getAsFloat();
        
        // Build a priority queue that sorts based on distance away from the given latitude and
        // longitude
        PriorityQueue<MapPoint> pq = new PriorityQueue<MapPoint>(10, new Comparator<MapPoint>() {
          @Override
          public int compare(MapPoint p1, MapPoint p2) {
            return getDistance(p1) - getDistance(p2);
          }
          
          // Calculate the great circle distance between the given point and the latitude and
          // longitude of the user
          private int getDistance(MapPoint p) {
            float lat = Float.valueOf(p.getLat());
            float lng = Float.valueOf(p.getLng());
            double dLat = rad(lat - latitude);
            double dLng = rad(lng - longitude);
            double dist = 1000000 * Math.asin(Math.sqrt(sinsq(dLat / 2) + cos(rad(latitude))
                * cos(rad(lat)) * sinsq(dLng / 2)));
            return (int) dist;
          }
          
          private double rad(double deg) {
            return Math.toRadians(deg);
          }
          
          private double sinsq(double angle) {
            return Math.sin(angle) * Math.sin(angle);
          }
          
          private double cos(double angle) {
            return Math.cos(angle);
          }
        });
        
        // Add all points into the priority queue
        for (MapPoint point : MapPoint.loadAll()) {
          pq.add(point);
        }
        
        // Pop off the ten closest ones
        for (int i = 0; i < 10; i++) {
          respArray.add(pq.poll().serialize());
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
