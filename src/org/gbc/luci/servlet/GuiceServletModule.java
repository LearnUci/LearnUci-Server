package org.gbc.luci.servlet;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.gbc.luci.providers.IndexSoyParameterProvider;
import org.gbc.luci.providers.ProviderMap;

import com.google.inject.servlet.ServletModule;

@SuppressWarnings("serial")
public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    ProviderMap.put("indexPage", new IndexSoyParameterProvider());
    
    // Bind servlets
    serve("/").with(StaticPageServlet.class, new HashMap<String, String>() {{
      put("soyPath", "org.gbc.luci.index");
      put("soyData", "indexPage");
    }});
    serve("/add").with(AddPointServlet.class);
    serve("/update").with(UpdatePointServlet.class);
    serve("/query").with(QueryPointServlet.class);
  }
  
  /**
   * Utility function for asserting the existance of a parameter
   * @param req The request object to check
   * @param parameter The parameter to look for
   * @return The string value of the parameter
   * @throws ParameterNotFoundException if the parameter is not found
   */
  public static String require(HttpServletRequest req, String parameter)
      throws ParameterNotFoundException {
    String p = req.getParameter(parameter);
    if (p == null ) {
      throw new ParameterNotFoundException(parameter);
    }
    return p;
  }
  
  public static class ParameterNotFoundException extends Exception {
    public ParameterNotFoundException(String parameter) {
      super(String.format("Parameter %s expected", parameter));
    }
  }
  
  public static class InvalidParameterFormatException extends Exception {
    public InvalidParameterFormatException(String parameter) {
      super(String.format("Parameter %s was inproperly formatted", parameter));
    }
  }
}
