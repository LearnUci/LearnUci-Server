package org.gbc.luci.datastore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@SuppressWarnings("serial")
public class Tour extends AbstractDatastoreEntity {
  private static final String KIND = "Tour";
  private static final Set<String> keys = new HashSet<String>() {{
    add("name");
    add("desc");
    add("points");
    add("img");
  }};
  
  private Tour() { }
  
  public String getName() {
    return getString("name");
  }
  
  public String getDesc() {
    return getText("desc").getValue();
  }
  
  public JsonArray getPoints() {
    JsonParser parser = new JsonParser();
    return (JsonArray) parser.parse(getText("points").getValue());
  }
  
  public Blob getImage() {
    return getBlob("img");
  }
  
  public Builder builder() {
    Builder builder = new Builder();
    loadBuilder(builder);
    return builder;
  }
  
  public static List<Tour> loadAll() {
    List<Entity> entities = DatastoreManager.query(new Query(KIND));
    List<Tour> tours = new ArrayList<Tour>();
    for (Entity entity : entities) {
      tours.add(load(entity));
    }
    return tours;
  }
  
  public static Tour load(long key) {
    return load(key, new Tour());
  }
  
  public static Tour load(Entity entity) {
    return load(entity, new Tour());
  }
  
  @Override
  protected boolean canPut(String key, Object c) {
    return keys.contains(key);
  }

  @Override
  protected String getKind() {
    return KIND;
  }
  
  public static class Builder extends AbstractDatastoreEntity.Builder<Tour> {
    public Builder setName(String name) {
      put("name", name);
      return this;
    }
    
    public Builder setDesc(String desc) {
      put("desc", new Text(desc));
      return this;
    }
    
    public Builder setPoints(JsonArray points) {
      put("points", new Text(points.toString()));
      return this;
    }
    
    public Builder setImage(byte[] bytes) {
      if (bytes != null) {
        put("img", new Blob(bytes));
      }
      return this;
    }
    
    @Override
    protected Tour getDefaultInstance() {
      return new Tour();
    }
  }
}
