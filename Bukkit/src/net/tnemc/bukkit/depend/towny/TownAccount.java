package net.tnemc.bukkit.depend.towny;

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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import net.tnemc.core.account.NonPlayerAccount;
import net.tnemc.plugincore.PluginCore;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an account linked to a Town in the Towny Plugin.
 *
 * @author creatorfromhell
 * @since 0.1.2.0
 */
public class TownAccount extends NonPlayerAccount {

  /**
   * The identifier passed in is always kept. Town accounts used to replace it with the UUID of
   * whichever town currently has this name, which made every account file with the same name
   * (old copies, towns that fell and were refounded) load into the same account and overwrite
   * each other's balance. Name based lookups are resolved to the current town's UUID before the
   * account is created instead, see {@link TownyHandler}.
   */
  public TownAccount(final UUID identifier, final String name) {

    super(identifier, name);

    PluginCore.log().debug("Town Account created with Name: " + name + " and UUID: " + identifier + ".");
  }

  /**
   * Finds the UUID of the town that currently exists in Towny with this account name.
   *
   * @param name The account name, e.g. "town-Constantinople".
   *
   * @return The town's UUID, or empty if no such town exists right now.
   */
  public static Optional<UUID> currentTown(final String name) {

    try {

      final String prefix = TownySettings.getTownAccountPrefix();
      if(name == null || !name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
        return Optional.empty();
      }

      final Town town = TownyAPI.getInstance().getTown(name.substring(prefix.length()));
      return (town == null)? Optional.empty() : Optional.of(town.getUUID());
    } catch(final Throwable ignore) {

      //Towny isn't enabled yet, or something went wrong enabling it.
      return Optional.empty();
    }
  }

  /**
   * Used to get the type of account that this is. This is for data-purposes only.
   *
   * @return The account type.
   */
  @Override
  public String type() {

    return "town";
  }

  @Override
  public UUID generateIdentifier(final String name) {

    return currentTown(name).orElse(identifier);
  }
}