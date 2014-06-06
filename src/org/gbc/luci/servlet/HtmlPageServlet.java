package org.gbc.luci.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gbc.luci.providers.ProviderMap;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;

@SuppressWarnings("serial")
@Singleton
public class HtmlPageServlet extends HttpServlet {
  private SoyTofu tofu;
  @Inject SoyFileSet.Builder builder;
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (tofu == null) {
      File soyDir = new File("soy");
      for (File soyFile : soyDir.listFiles()) {
        builder.add(soyFile);
      }
      tofu = builder.build().compileToTofu();
    }
    
    resp.setContentType("text/html");
    resp.getWriter().println(tofu.newRenderer(getServletConfig().getInitParameter("soyPath"))
        .setData(ProviderMap.get(getServletConfig().getInitParameter("soyData"))).render());
  }
}
