package net.tnemc.item.providers;
/*
 * The New Economy
 * Copyright (C) 2022 - 2024 Daniel "creatorfromhell" Vidmar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.vdurmont.semver4j.Semver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Patched copy of the TNIL VersionUtil bundled through TNPC. It overrides the bundled class when
 * the jar is shaded.
 * <p>
 * Since 26.1, Paper reports versions like "26.2.build.2633", which semver4j rejects. The server
 * version is reduced to its leading numeric part ("26.2") before comparing.
 *
 * @author CielDev
 * @since 0.1.5.0
 */
public class VersionUtil {

  private static final Pattern RELEASE = Pattern.compile("^\\d+(\\.\\d+){0,2}");

  public static boolean isVersion(final String version, final String required) {

    return new Semver(release(version), Semver.SemverType.LOOSE).isGreaterThanOrEqualTo(required);
  }

  public static boolean isLessThan(final String version, final String required) {

    return new Semver(release(version), Semver.SemverType.LOOSE).isLowerThan(required);
  }

  private static String release(final String version) {

    final Matcher matcher = RELEASE.matcher(version);
    return (matcher.find())? matcher.group() : version;
  }

  public static boolean isOneSeven(final String version) {

    return isVersion(version, "1.7.0");
  }

  public static boolean isOneEight(final String version) {

    return isVersion(version, "1.8.0");
  }

  public static boolean isOneNine(final String version) {

    return isVersion(version, "1.9.0");
  }

  public static boolean isOneTen(final String version) {

    return isVersion(version, "1.10.0");
  }

  public static boolean isOneEleven(final String version) {

    return isVersion(version, "1.11.0");
  }

  public static boolean isOneTwelve(final String version) {

    return isVersion(version, "1.12.0");
  }

  public static boolean isOneThirteen(final String version) {

    return isVersion(version, "1.13.0");
  }

  public static boolean isOneFourteen(final String version) {

    return isVersion(version, "1.14.0");
  }

  public static boolean isOneFifteen(final String version) {

    return isVersion(version, "1.15.0");
  }

  public static boolean isOneSixteen(final String version) {

    return isVersion(version, "1.16.0");
  }

  public static boolean isOneSeventeen(final String version) {

    return isVersion(version, "1.17.0");
  }

  public static boolean isOneTwenty(final String version) {

    return isVersion(version, "1.20.0");
  }

  public static boolean isOneTwentyOne(final String version) {

    return isVersion(version, "1.21.0");
  }

  public static boolean isOneTwentyOneTwo(final String version) {

    return isVersion(version, "1.21.2");
  }

  public static boolean isOneTwentyOneThree(final String version) {

    return isVersion(version, "1.21.3");
  }

  public static boolean isOneTwentyOneFour(final String version) {

    return isVersion(version, "1.21.4");
  }

  public static boolean isOneTwentyOneFive(final String version) {

    return isVersion(version, "1.21.5");
  }

  public static boolean isOneTwentyTwo(final String version) {

    return isVersion(version, "1.22.0");
  }

  public static boolean isOneTwentyThree(final String version) {

    return isVersion(version, "1.23.0");
  }
}
