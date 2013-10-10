package org.gbc.luci.datastore;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.gson.JsonObject;

abstract class AbstractDatastoreEntity {
  private Map<String, Object> keyValues = new HashMap<String, Object>();
  protected Long id;
  
  protected abstract boolean canPut(String key, Object c);
  protected abstract String getKind();
  
  protected void put(String key, Object value) {
    if (canPut(key, value)) {
      keyValues.put(key, value);
    } else {
      throw new IllegalArgumentException("Tried to put in invalid key " + key);
    }
  }
  
  protected Object get(String key) {
    return keyValues.get(key);
  }
  
  protected String getString(String key) {
    return (String) get(key);
  }
  
  protected Blob getBlob(String key) {
    return (Blob) get(key);
  }
  
  protected Text getText(String key) {
    return (Text) get(key);
  }
  
  protected <T extends AbstractDatastoreEntity> void loadBuilder(Builder<T> builder) {
    for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
      builder.put(entry.getKey(), entry.getValue());
    }
    builder.setId(id);
  }
  
  protected static <T extends AbstractDatastoreEntity> T load(Entity entity, T instance) {
    for (Map.Entry<String, Object> entry : entity.getProperties().entrySet()) {
      instance.put(entry.getKey(), entry.getValue());
    }
    instance.id = entity.getKey().getId();
    return instance;
  }
  
  protected static <T extends AbstractDatastoreEntity> T load(long key, T instance) {
    return load(DatastoreManager.getEntity(KeyFactory.createKey(instance.getKind(), key)),
        instance);
  }

  
  /**
   * Saves the entity to the datastore.
   */
  public void save() {
    Entity entity = null;
    if (id == null) {
      entity = new Entity(getKind()); 
    } else {
      entity = new Entity(getKind(), id);
    }
    for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
      entity.setProperty(entry.getKey(), entry.getValue());
    }
    id = DatastoreManager.putEntity(entity).getId();
  }
  
  /**
   * Returns this object as a json object, ready for transport.
   * @return Json Object.
   */
  public JsonObject serialize() {
    JsonObject obj = new JsonObject();
    for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Blob) {
        obj.addProperty(entry.getKey(), Base64.encodeBase64String(((Blob) value).getBytes()));
      } else if (value instanceof Text) {
        obj.addProperty(entry.getKey(), ((Text) value).getValue());
      } else {
        obj.addProperty(entry.getKey(), entry.getValue().toString());
      }
    }
    return obj;
  }
  
  protected abstract static class Builder<T extends AbstractDatastoreEntity> {
    private Map<String, Object> dataValues = new HashMap<String, Object>();
    private Long id = null;
    
    protected void put(String key, Object value) {
      dataValues.put(key, value);
    }
    
    protected abstract T getDefaultInstance();
    
    public Builder<T> setId(long id) {
      this.id = id;
      return this;
    }
    
    public T build() {
      T instance = getDefaultInstance();
      for (Map.Entry<String, Object> entry : dataValues.entrySet()) {
        instance.put(entry.getKey(), entry.getValue());
      }
      instance.id = id;
      return instance;
    }
  }
}
