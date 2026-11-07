package net.tnemc.core.account.holdings;
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

import net.tnemc.core.utils.Identifier;
import net.tnemc.plugincore.PluginCore;
import net.tnemc.plugincore.core.compatibility.log.DebugLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/**
 * Keeps track of item currency changes made while a player is offline (taxes, offline payments,
 * etc).
 * <p>
 * Item balances live in the player's inventory and ender chest, so the stored wallet value is only
 * a snapshot. If the server stops uncleanly, that snapshot can be older than the inventory
 * Minecraft saved, and trusting it on join would recreate items the player had already spent. On
 * join the real items are used instead, and only the offline changes recorded here are applied.
 * <p>
 * Changes are written to disk as they happen so they survive a crash.
 *
 * @author ciel0beats
 * @since 0.1.5.3
 */
public class OfflineItemLedger {

  private static final String FILE_NAME = "offline-item-changes.properties";

  private static final Properties changes = new Properties();
  private static boolean loaded = false;

  private OfflineItemLedger() {
  }

  private static String key(final UUID player, final String region, final UUID currency, final Identifier handler) {

    return player + "|" + region + "|" + currency + "|" + handler.asID();
  }

  private static File file() {

    return new File(PluginCore.directory(), FILE_NAME);
  }

  private static void load() {

    if(loaded) return;
    loaded = true;

    final File file = file();
    if(!file.exists()) return;

    try(final InputStream in = Files.newInputStream(file.toPath())) {
      changes.load(in);
    } catch(final IOException e) {
      PluginCore.log().error("Unable to load " + FILE_NAME + ": " + e.getMessage(), DebugLevel.OFF);
    }
  }

  private static void save() {

    final File file = file();
    final File temp = new File(file.getParentFile(), FILE_NAME + ".tmp");

    try {
      if(file.getParentFile() != null) {
        file.getParentFile().mkdirs();
      }

      try(final OutputStream out = Files.newOutputStream(temp.toPath())) {
        changes.store(out, "Item currency changes made while players were offline. Do not edit.");
      }
      Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch(final IOException e) {
      try {
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch(final IOException ex) {
        PluginCore.log().error("Unable to save " + FILE_NAME + ": " + ex.getMessage(), DebugLevel.OFF);
      }
    }
  }

  /**
   * Records a change to an offline player's item holdings.
   *
   * @param change The amount the holdings changed by. Negative for removals.
   */
  public static synchronized void record(final UUID player, final String region, final UUID currency, final Identifier handler, final BigDecimal change) {

    if(change == null || change.signum() == 0) return;

    load();

    final String key = key(player, region, currency, handler);
    final BigDecimal total = current(key).add(change);

    if(total.signum() == 0) {
      changes.remove(key);
    } else {
      changes.setProperty(key, total.toPlainString());
    }
    save();
  }

  /**
   * Returns the pending offline change for the specified holdings and clears it.
   */
  public static synchronized BigDecimal take(final UUID player, final String region, final UUID currency, final Identifier handler) {

    load();

    final String key = key(player, region, currency, handler);
    final BigDecimal value = current(key);

    if(changes.remove(key) != null) {
      save();
    }
    return value;
  }

  private static BigDecimal current(final String key) {

    final String value = changes.getProperty(key);
    if(value == null) return BigDecimal.ZERO;

    try {
      return new BigDecimal(value);
    } catch(final NumberFormatException ignore) {
      return BigDecimal.ZERO;
    }
  }
}
