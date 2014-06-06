package org.gbc.luci.providers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.gbc.luci.datastore.MapPoint;

import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;

public class SupportSoyParameterProvider implements SoyParameterProvider {
  @Override
  public Map<String, Object> get() {
    return new HashMap<String, Object>();
  }
}
