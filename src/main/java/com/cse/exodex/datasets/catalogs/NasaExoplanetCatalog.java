package com.cse.exodex.datasets.catalogs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.cse.exodex.datasets.PlanetData;
import com.cse.exodex.datasets.StarRecord;
import com.cse.exodex.datasets.StarIdentifiers;
import com.cse.exodex.datasets.AstroConvert;
import com.cse.exodex.datasets.ExternalLinks;
import com.cse.exodex.datasets.StellarLibrary;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NasaExoplanetCatalog implements ExoplanetCatalog {
  private static final Logger LOG = LoggerFactory.getLogger(NasaExoplanetCatalog.class);

  private final Multimap<Integer, PlanetData> allPlanetsByStarID = LinkedListMultimap.create();
  private final Map<String, StarRecord> syntheticStarsByHost = new HashMap<String, StarRecord>();

  public NasaExoplanetCatalog(StellarLibrary library) throws IOException {
    InputStream stream = NasaExoplanetCatalog.class.getClassLoader().getResourceAsStream("main_data.csv");
    if (stream == null) {
      throw new IOException("Could not find main_data.csv on the classpath");
    }

    BufferedReader nasaPlanets = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    String headerLine;
    do {
      headerLine = nasaPlanets.readLine();
    } while (headerLine != null && headerLine.startsWith("#"));
    if (headerLine == null) {
      throw new IOException("main_data.csv does not contain a header");
    }

    Map<String, Integer> columns = columnIndexes(parseCsvLine(headerLine));
    int planetCount = 0;
    int unmatchedCount = 0;
    String next;
    while ((next = nasaPlanets.readLine()) != null) {
      if (next.trim().isEmpty() || next.startsWith("#")) {
        continue;
      }

      String[] line = parseCsvLine(next);
      String starName = value(line, columns, "hostname");
      if (starName.isEmpty()) {
        continue;
      }

      StarRecord starRecord = findStar(library, starName,
          value(line, columns, "hip_name"), value(line, columns, "hd_name"));
      if (starRecord == null) {
        starRecord = syntheticStarsByHost.get(starName);
        if (starRecord == null) {
          starRecord = createSyntheticStar(starName, line, columns);
          syntheticStarsByHost.put(starName, starRecord);
        }
        unmatchedCount++;
      }

      String massRaw = firstValue(line, columns, "pl_massj", "pl_bmassj");
      String radiusRaw = firstValue(line, columns, "pl_radj");
      Unit radiusUnit = Unit.RADIUS_JUP;
      if (radiusRaw.isEmpty()) {
        radiusRaw = firstValue(line, columns, "pl_rade");
        radiusUnit = Unit.RADIUS_EARTH;
      }

      allPlanetsByStarID.put(starRecord.getPrimaryId(), new PlanetData(
          parseInteger(value(line, columns, "rowid")),
          starRecord.getPrimaryId(),
          new PlanetData.PlanetName(starName, value(line, columns, "pl_letter"),
              value(line, columns, "pl_name")),
          ObjectValue.value(value(line, columns, "pl_orbsmax"), Unit.AU, Unit.LY, PlanetDefaults.DEFAULT_SEMI_MAJOR_AXIS),
          ObjectValue.value(value(line, columns, "pl_orbeccen"), Unit.NONE, Unit.NONE, PlanetDefaults.DEFAULT_ECCENTRICITY),
          ObjectValue.value(value(line, columns, "pl_orbper"), Unit.DAY, Unit.DAY, PlanetDefaults.DEFAULT_ORBITAL_PERIOD),
          ObjectValue.value(value(line, columns, "pl_orbincl"), Unit.DEGREE_GEOM, Unit.DEGREE_GEOM, PlanetDefaults.DEFAULT_INCLINATION),
          ObjectValue.value(massRaw, Unit.MASS_JUP, Unit.KG, PlanetDefaults.DEFAULT_MASS),
          ObjectValue.value(radiusRaw, radiusUnit, Unit.LY, PlanetDefaults.DEFAULT_RADIUS),
          ObjectValue.value(value(line, columns, "pl_dens"), Unit.G_PER_CC, Unit.G_PER_CC, PlanetDefaults.DENSITY),
          ObjectValue.value("", Unit.DEGREE_GEOM, Unit.DEGREE_GEOM, PlanetDefaults.DEFAULT_LONG_ASCENDING),
          ObjectValue.value("", Unit.DEGREE_GEOM, Unit.DEGREE_GEOM, PlanetDefaults.DEFAULT_ARGUMENT_PERHELION),
          ObjectValue.value("", Unit.DEGREE_GEOM, Unit.DEGREE_GEOM, PlanetDefaults.DEFAULT_AXIAL_TILT)
      ));
      planetCount++;
    }
    nasaPlanets.close();
    LOG.info("Loaded " + planetCount + " exoplanets from main_data.csv; unmatched hosts: " + unmatchedCount);

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Earth"),
        new ObjectValue(1.5812e-5, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.0167086, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(365.256363, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(0.0, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(5.97237e24, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(6.7341e-10, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(5.514, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(0.0, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(0.0, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(23.4392811, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Mercury"),
        new ObjectValue(6.120989e-6, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.205630, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(87.969, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(7.005, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(3.3011e23, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(2.5787e-10, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(5.427, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(48.331, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(29.124, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(0.034, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Venus"),
        new ObjectValue(1.14376e-5, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.006772, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(224.701, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(3.39458, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(4.8675e24, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(6.39675765e-10, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(5.243, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(76.680, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(54.884, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(2.64, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Mars"),
        new ObjectValue(2.4093185478e-5, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.0934, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(686.971, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(1.850, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(6.4171e23, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(3.582704327e-10, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(3.9335, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(49.558, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(286.502, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(25.19, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Jupiter"),
        new ObjectValue(8.22661511e-5, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.048498, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(4332.59, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(1.303, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(1.8986e27, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(7.3895985e-9, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(1.326, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(100.464, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(273.867, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(3.13, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Saturn"),
        new ObjectValue(0.00015108706936, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.05555, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(10759.22, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(2.485240, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(5.6836e26, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(6.1551273e-9, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.687, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(113.665, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(339.392, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(26.73, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Uranus"),
        new ObjectValue(0.0003038910924, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.046381, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(30688.5, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(0.773, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(8.6810e25, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(2.6807655e-9, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(1.27, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(74.006, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(96.998857, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(97.77, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    allPlanetsByStarID.put(1, new PlanetData(
        null,
        1,
        new PlanetData.PlanetName("Sol", null, "Neptune"),
        new ObjectValue(0.00047612071755, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(0.009456, ValueSource.SUPPLIED, Unit.NONE),
        new ObjectValue(60182, ValueSource.SUPPLIED, Unit.DAY),
        new ObjectValue(1.767975, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(1.0243e26, ValueSource.SUPPLIED, Unit.KG),
        new ObjectValue(2.6025475e-9, ValueSource.SUPPLIED, Unit.LY),
        new ObjectValue(1.638, ValueSource.SUPPLIED, Unit.G_PER_CC),
        new ObjectValue(131.784, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(276.336, ValueSource.SUPPLIED, Unit.DEGREE_GEOM),
        new ObjectValue(28.32, ValueSource.SUPPLIED, Unit.DEGREE_GEOM)
    ));

    LOG.info("Found stars from HYG");

  }

  public Collection<StarRecord> getSyntheticStars() {
    return new ArrayList<StarRecord>(syntheticStarsByHost.values());
  }

  private static StarRecord createSyntheticStar(String hostName, String[] line,
                                                Map<String, Integer> columns) {
    StarIdentifiers identifiers = new StarIdentifiers();
    identifiers.setProperName(hostName);
    double distanceParsecs = parseDoubleOrDefault(value(line, columns, "sy_dist"), 0.0);
    double rightAscension = parseDoubleOrDefault(value(line, columns, "ra"), 0.0);
    double declination = parseDoubleOrDefault(value(line, columns, "dec"), 0.0);
    return new StarRecord(
        identifiers,
        new ExternalLinks(),
        new ObjectValue(AstroConvert.parsecsToLightyears(distanceParsecs), ValueSource.DEFAULT, Unit.LY),
        new ObjectValue(AstroConvert.degreesToRadians(rightAscension), ValueSource.DEFAULT, Unit.RADIAN),
        new ObjectValue(AstroConvert.degreesToRadians(declination), ValueSource.DEFAULT, Unit.RADIAN),
        new ObjectValue(0.0, ValueSource.DEFAULT, Unit.MV),
        "G2V",
        null,
        1.0
    );
  }

  private static double parseDoubleOrDefault(String raw, double defaultValue) {
    if (raw.isEmpty()) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static Map<String, Integer> columnIndexes(String[] header) {
    Map<String, Integer> columns = new HashMap<String, Integer>();
    for (int i = 0; i < header.length; i++) {
      columns.put(header[i].trim(), i);
    }
    return columns;
  }

  private static String value(String[] line, Map<String, Integer> columns, String column) {
    Integer index = columns.get(column);
    return index == null || index >= line.length ? "" : line[index].trim();
  }

  private static String firstValue(String[] line, Map<String, Integer> columns, String... names) {
    for (String name : names) {
      String value = value(line, columns, name);
      if (!value.isEmpty()) {
        return value;
      }
    }
    return "";
  }

  private static Integer parseInteger(String raw) {
    return raw.isEmpty() ? null : Integer.valueOf(raw);
  }

  private static StarRecord findStar(StellarLibrary library, String... names) throws IOException {
    for (String name : names) {
      if (!name.isEmpty()) {
        StarRecord record = library.find(name);
        if (record != null) {
          return record;
        }
      }
    }
    return null;
  }

  private static String[] parseCsvLine(String line) {
    java.util.List<String> fields = new java.util.ArrayList<String>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char current = line.charAt(i);
      if (current == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        fields.add(field.toString());
        field.setLength(0);
      } else {
        field.append(current);
      }
    }
    fields.add(field.toString());
    return fields.toArray(new String[fields.size()]);
  }

  @Override
  public Multimap<Integer, PlanetData> getAllPlanetsByStarID(Collection<StarRecord> stars) {

    Multimap<Integer, PlanetData> forStars = HashMultimap.create();
    for (StarRecord star : stars) {
      Integer starID = star.getPrimaryId();
      forStars.putAll(starID, allPlanetsByStarID.get(starID));
    }

    return forStars;

  }
}