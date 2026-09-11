package com.cse.exodex;

import javax.servlet.DispatcherType;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.Semaphore;

import com.cse.exodex.api.StarCatalogServlet;
import com.cse.exodex.datasets.StellarLibrary;
import com.cse.exodex.datasets.catalogs.HYGDatabase;
import com.cse.exodex.datasets.catalogs.NasaExoplanetCatalog;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.webapp.WebAppContext;

public class WebServer implements Runnable {
  public static final int DEFAULT_PORT = 42315;


  private final Semaphore shutdownLock = new Semaphore(0);

  public WebServer(){}

  public final void shutdown() {
    shutdownLock.release();
  }

  @Override
  public void run() {
    try {

      Server uiServer = new Server(DEFAULT_PORT);
      final URL warUrl = uiServer.getClass().getClassLoader().getResource("com/cse/exodex/www");
      final String warUrlString = warUrl.toExternalForm();

      HYGDatabase hygDatabase = new HYGDatabase();
      NasaExoplanetCatalog planetCatalog = new NasaExoplanetCatalog(new StellarLibrary(hygDatabase.getAllStars(75.0)));


      WebAppContext context = new WebAppContext(warUrlString, "/");
      context.addServlet(new ServletHolder(new StarCatalogServlet(hygDatabase, planetCatalog)), "/star_catalog");
      context.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

      uiServer.setHandler(context);

      uiServer.start();

      shutdownLock.acquire();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws InterruptedException {
    DOMConfigurator.configure(WebServer.class.getResource("/com/cse/exodex/log4j.xml"));

    WebServer server = new WebServer();
    Thread thread1 = new Thread(server);

    thread1.start();
    thread1.join();
  }
}
