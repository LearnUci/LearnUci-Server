package org.gbc.luci.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
public class StaticPageServlet extends HttpServlet {
  private static final Set<String> allowed = new HashSet<String>() {{
    add("ypma@uci.edu");
    add("yuhao93@gmail.com");
    add("etech@uci.edu");
  }};
  
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
    
    UserService userService = UserServiceFactory.getUserService();
    String url = req.getRequestURI();
    
    resp.setContentType("text/html");
    
    if (req.getUserPrincipal() == null) {
      resp.getWriter().println("<a href=\"" + userService.createLoginURL(url) + "\">Login</a>");
    } else if (!allowed.contains(req.getUserPrincipal().toString().toLowerCase())) {
      resp.getWriter().println("<h1>Sorry, " + req.getUserPrincipal().getName() + " is not a valid user!</h1><a href=\"" + userService.createLogoutURL(url) + "\">Logout</a>");
    } else {
      resp.getWriter().println(tofu.newRenderer(getServletConfig().getInitParameter("soyPath"))
          .setData(ProviderMap.get(getServletConfig().getInitParameter("soyData"))).render());
    }
  }
}
