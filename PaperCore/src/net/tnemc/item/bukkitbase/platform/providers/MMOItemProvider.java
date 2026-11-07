package net.tnemc.item.bukkitbase.platform.providers;
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

import net.tnemc.item.AbstractItemStack;
import net.tnemc.item.providers.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Patched copy of the TNIL MMOItemProvider bundled through TNPC. It overrides the bundled class
 * when the jar is shaded.
 * <p>
 * The bundled class calls MMOItems directly, so every inventory check throws
 * NoClassDefFoundError on servers without MMOItems. This version looks MMOItems up by reflection
 * and simply reports "not an MMOItem" when the plugin is missing.
 *
 * @author CielDev
 * @since 0.1.5.0
 */
public class MMOItemProvider implements ItemProvider<ItemStack> {

  private static volatile boolean resolved = false;
  private static Object plugin;
  private static Method getType;
  private static Method getID;
  private static Method getItem;
  private static Method typeGetId;

  private static boolean available() {

    if(!resolved) {
      synchronized(MMOItemProvider.class) {
        if(!resolved) {
          try {
            if(Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
              final Class<?> mmo = Class.forName("net.Indyuce.mmoitems.MMOItems");
              getType = mmo.getMethod("getType", ItemStack.class);
              getID = mmo.getMethod("getID", ItemStack.class);
              getItem = mmo.getMethod("getItem", String.class, String.class);
              typeGetId = Class.forName("net.Indyuce.mmoitems.api.Type").getMethod("getId");
              plugin = mmo.getField("plugin").get(null);
            }
          } catch(final Throwable ignore) {
            plugin = null;
          }
          resolved = true;
        }
      }
    }
    return plugin != null;
  }

  @Override
  public boolean appliesTo(final AbstractItemStack<? extends ItemStack> stack, final ItemStack item) {

    if(item == null || !available()) {
      return false;
    }

    try {
      final Object type = getType.invoke(null, item);
      if(type == null) {
        return false;
      }

      final Object id = getID.invoke(null, item);
      if(id == null) {
        return false;
      }

      stack.setItemProvider(identifier());
      stack.setProviderItemID(typeGetId.invoke(type) + ":" + id);
      return true;
    } catch(final Throwable ignore) {
      return false;
    }
  }

  @Override
  public boolean similar(final AbstractItemStack<? extends ItemStack> stack, final ItemStack item) {

    if(item == null || !available()) {
      return false;
    }

    try {
      final Object id = getID.invoke(null, item);
      return id != null && id.equals(stack.providerItemID());
    } catch(final Throwable ignore) {
      return false;
    }
  }

  @Override
  public ItemStack locale(final AbstractItemStack<? extends ItemStack> stack, final int amount) {

    if(!available()) {
      return null;
    }

    try {
      final String[] split = stack.providerItemID().split(":");
      return (ItemStack)getItem.invoke(plugin, split[0], stack.providerItemID());
    } catch(final Throwable ignore) {
      return null;
    }
  }

  @Override
  public String identifier() {

    return "mmoitem";
  }
}
