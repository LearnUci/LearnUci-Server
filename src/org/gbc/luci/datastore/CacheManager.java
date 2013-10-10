package org.gbc.luci.datastore;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class CacheManager {
  private static final String MAP_POINT_LIST_KEY = "LIST";
  private static final String MAP_POINT = "MAP_POINT_";
  
  private CacheManager() { }
  private static final MemcacheService service = MemcacheServiceFactory.getMemcacheService();
  
  public static boolean hasMapPointList() {
    return service.contains(MAP_POINT_LIST_KEY);
  }
  
  public static boolean hasMapPoint(long id) {
    return service.contains(MAP_POINT + id);
  }
  
  public static MapPoint getMapPoint(long id) {
    if (!hasMapPoint(id)) {
      return null;
    }
    return (MapPoint) service.get(id);
  }
  
  public static List<MapPoint> allMapPoints() {
    List<MapPoint> list = new ArrayList<MapPoint>();
    String[] keys = ((String) service.get(MAP_POINT_LIST_KEY)).split(" ");
    for (String key : keys) {
      list.add(getMapPoint(Long.valueOf(key)));
    }
    return list;
  }
  
  public static void addMapPoints(List<MapPoint> points) {
    if (points.size() == 0) {
      return;
    }
    StringBuilder builder = new StringBuilder();
    builder.append(points.get(0).getId());
    
  }
}
