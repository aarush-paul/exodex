package com.cse.exodex.datasets.catalogs;

import java.util.Collection;

import com.cse.exodex.datasets.PlanetData;
import com.cse.exodex.datasets.StarRecord;
import com.google.common.collect.Multimap;

public interface ExoplanetCatalog {
  public Multimap<Integer, PlanetData> getAllPlanetsByStarID(Collection<StarRecord> stars);
}
