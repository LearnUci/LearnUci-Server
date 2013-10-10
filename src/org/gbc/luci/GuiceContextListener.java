package org.gbc.luci;

import org.gbc.luci.servlet.GuiceServletModule;

import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceContextListener extends GuiceServletContextListener {
  @Override protected Injector getInjector() {
    return Guice.createInjector(new GuiceServletModule());
  }
}