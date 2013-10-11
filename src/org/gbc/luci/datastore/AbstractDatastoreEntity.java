package org.gbc.luci.datastore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.PutException;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.StatusCode;
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
  
  protected static void deleteAllIndexes(String indexName) {
    try {
      IndexSpec spec = IndexSpec.newBuilder().setName(indexName).build();
      Index index = SearchServiceFactory.getSearchService().getIndex(spec);
      // looping because getRange by default returns up to 100 documents at a time
      while (true) {
        List<String> docIds = new ArrayList<String>();
        // Return a set of doc_ids.
        GetRequest request = GetRequest.newBuilder().setReturningIdsOnly(true).build();
        GetResponse<Document> response = index.getRange(request);
        if (response.getResults().isEmpty()) {
          break;
        }
        for (Document doc : response) {
          docIds.add(doc.getId());
        }
        index.delete(docIds);
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
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

  protected void indexEntity(String indexName, Document doc) {
    IndexSpec spec = IndexSpec.newBuilder().setName(indexName).build();
    Index index = SearchServiceFactory.getSearchService().getIndex(spec);
    try {
      index.put(doc);
    } catch (PutException e) {
      e.printStackTrace();
      if (StatusCode.TRANSIENT_ERROR.equals(e.getOperationResult().getCode())) {
        indexEntity(indexName, doc);
      }
    }
  }
  
  protected static List<Long> search(String indexName, String query) {
    List<Long> ids = new ArrayList<Long>();
    IndexSpec spec = IndexSpec.newBuilder().setName(indexName).build();
    Index index = SearchServiceFactory.getSearchService().getIndex(spec);
    Results<ScoredDocument> results = index.search(query);
    // Only name, abbr, and id are indexed, so we need to grab all ids and
    // retrieve the actual entities from the datastore
    for (ScoredDocument doc : results) {
      ids.add(Long.valueOf(doc.getOnlyField("id").getText()));
    }
    return ids;
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
