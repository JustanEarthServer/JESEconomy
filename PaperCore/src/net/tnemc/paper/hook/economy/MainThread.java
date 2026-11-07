package net.tnemc.paper.hook.economy;
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

import net.tnemc.paper.PaperCore;
import org.bukkit.Bukkit;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs balance changes on the main server thread.
 * <p>
 * Item currencies live in player inventories. If another plugin changes a balance from a
 * background thread, the balance is read on that thread and the items are changed afterwards, so
 * a player moving coins at the same moment could end up with extra items. Running the whole change
 * on the main thread keeps the read and the change together.
 *
 * @author ciel0beats
 * @since 0.1.5.3
 */
public final class MainThread {

  private static final long TIMEOUT_SECONDS = 10;

  private MainThread() {
  }

  /**
   * Runs the action on the main thread and waits for the result. Already on the main thread, it
   * simply runs it.
   *
   * @param action   The balance change to run.
   * @param fallback Returned when the main thread doesn't run the action in time. In that case
   *                 the action is cancelled and nothing is changed.
   */
  public static <T> T call(final Supplier<T> action, final Supplier<T> fallback) {

    if(Bukkit.isPrimaryThread()) {
      return action.get();
    }

    final Future<T> future = Bukkit.getScheduler().callSyncMethod(PaperCore.instance().getPlugin(), action::get);
    try {
      return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch(final Exception e) {

      //Only give up if it never started, otherwise wait for the change that is in progress.
      if(future.cancel(false)) {
        return fallback.get();
      }

      try {
        return future.get();
      } catch(final Exception ignore) {
        return fallback.get();
      }
    }
  }
}
