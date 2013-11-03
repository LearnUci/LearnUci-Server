package org.gbc.luci.datastore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;

@SuppressWarnings("serial")
public class MapPoint extends AbstractDatastoreEntity {
  private static final String KIND = "MapPoint";
  private static final Set<String> keySet = new HashSet<String>() {{
    add("name");
    add("lat");
    add("lng");
    add("abbr");
    add("type");
    add("img");
    add("desc");
  }};
  
  private MapPoint() { }
  
  public String getName() {
    return getString("name");
  }
  
  public String getLat() {
    return getString("lat");
  }
  
  public String getLng() {
    return getString("lng");
  }
  
  public String getAbbr() {
    return getString("abbr");
  }
  
  public String getType() {
    return getString("type");
  }
  
  public Blob getImg() {
    return getBlob("img");
  }
  
  public Text getDesc() {
    return getText("desc");
  }
  
  @Override
  public void save() {
    // Index the document if id is null, aka isn't in datastore yet
    boolean indexDoc = (id == null);
    // Run save first, so we have an id
    super.save();
    if (indexDoc) {
      Document doc = Document.newBuilder()
          .addField(Field.newBuilder().setName("name").setText(getName()))
          .addField(Field.newBuilder().setName("abbr").setText(getAbbr()))
          .addField(Field.newBuilder().setName("id").setText(String.valueOf(id)))
          .build();
      indexEntity(KIND, doc);
    }
  }
  
  public Builder builder() {
    Builder builder = new Builder();
    loadBuilder(builder);
    return builder;
  }
  
  public static void deleteAll(String key) {
    DatastoreManager.deleteAll(key, KIND);
    deleteAllIndexes(KIND);
  }
  
  /**
   * Load by performing a keyword search
   * @param query The keywords to search by
   * @return A List of MapPoint objects that match the keywords
   */
  public static List<MapPoint> load(String query) {
    List<Long> ids = search(KIND, query);
    // For each id given by the Search api, load a MapPoint by that id
    List<MapPoint> points = new ArrayList<MapPoint>();
    for (Long id : ids) {
      points.add(load(id));
    }
    return points;
  }
  
  public static List<MapPoint> loadByFilter(Filter filter) {
    List<Entity> entities = DatastoreManager.query(new Query(KIND).setFilter(filter));
    List<MapPoint> points = new ArrayList<MapPoint>();
    for (Entity entity : entities) {
      points.add(MapPoint.load(entity));
    }
    return points;
  }
  
  public static List<MapPoint> loadAll() {
    List<Entity> entities = DatastoreManager.query(new Query(KIND));
    List<MapPoint> points = new ArrayList<MapPoint>();
    for (Entity entity : entities) {
      points.add(MapPoint.load(entity));
    }
    return points;
  }
  
  public static MapPoint load(long key) {
    return load(key, new MapPoint());
  }
  
  public static MapPoint load(Entity entity) {
    return load(entity, new MapPoint());
  }
  
  public static class Builder extends AbstractDatastoreEntity.Builder<MapPoint> {
    public Builder setName(String name) {
      put("name", name);
      return this;
    }
    
    public Builder setLat(String lat) {
      put("lat", lat);
      return this;
    }
    
    public Builder setLng(String lng) {
      put("lng", lng);
      return this;
    }
    
    public Builder setAbbr(String abbr) {
      put("abbr", abbr);
      return this;
    }
    
    public Builder setType(String type) {
      put("type", type);
      return this;
    }
    
    public Builder setImg(byte[] img) {
      if (img != null) {
        put("img", new Blob(img));
      }
      return this;
    }
    
    public Builder setDescription(String desc) {
      if (desc != null) {
        put("desc", new Text(desc));
      }
      return this;
    }
    
    @Override
    public MapPoint getDefaultInstance() {
      return new MapPoint();
    }
  }

  @Override
  protected boolean canPut(String key, Object c) {
    return keySet.contains(key);
  }
  
  @Override
  protected String getKind() {
    return KIND;
  }
}
