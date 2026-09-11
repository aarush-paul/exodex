package com.cse.exodex.datasets.catalogs;

import com.cse.exodex.datasets.StarRecord;

import java.util.Collection;

public interface StarCatalog {
  public Collection<StarRecord> getAllStars(final double maxLyDistance);
}
