package org.gbc.luci.providers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.gbc.luci.datastore.MapPoint;

import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;

public class IndexSoyParameterProvider implements SoyParameterProvider {
  @Override
  public Map<String, Object> get() {
    Map<String, Object> map = new HashMap<String, Object>();
    SoyListData data = new SoyListData();
    MapPoint[] points = MapPoint.loadAll().toArray(new MapPoint[0]);
    Arrays.sort(points, new Comparator<MapPoint>() {
      @Override
      public int compare(MapPoint o1, MapPoint o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    
    for (MapPoint point : points) {
      SoyMapData mapData = new SoyMapData();
      mapData.put("id", point.getId().toString());
      mapData.put("text", point.getName());
      data.add(mapData);
    }
    
    map.put("points", data);
    return map;
  }
}
