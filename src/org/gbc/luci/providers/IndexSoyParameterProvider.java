package org.gbc.luci.providers;

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
    for (MapPoint point : MapPoint.loadAll()) {
      SoyMapData mapData = new SoyMapData();
      mapData.put("id", point.getId().toString());
      mapData.put("text", point.getName());
      data.add(mapData);
    }
    
    map.put("points", data);
    return map;
  }
}
