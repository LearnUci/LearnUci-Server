package org.gbc.luci.providers;

import java.util.HashMap;
import java.util.Map;

public class ProviderMap {
  private static Map<String, SoyParameterProvider> providers =
      new HashMap<String, SoyParameterProvider>();
  
  private ProviderMap() { }
  
  public static Map<String, Object> get(String key) {
    return providers.get(key).get();
  }
  
  public static void put(String key, SoyParameterProvider provider) {
    providers.put(key, provider);
  }
}
