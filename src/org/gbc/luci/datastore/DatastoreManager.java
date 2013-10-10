package org.gbc.luci.datastore;

import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

class DatastoreManager {
  private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  
  public static Key putEntity(Entity entity) {
    return datastore.put(entity);
  }
  
  public static Entity getEntity(Key key) {
    try {
      return datastore.get(key);
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public static List<Entity> query(Query query) {
    return datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
  }
}
