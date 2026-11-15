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
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import net.tnemc.core.TNECore;
import net.tnemc.core.account.Account;
import net.tnemc.core.account.PlayerAccount;
import net.tnemc.core.account.SharedAccount;
import net.tnemc.core.account.holdings.HoldingsEntry;
import net.tnemc.core.account.holdings.Wallet;
import net.tnemc.core.io.storage.datables.yaml.YAMLAccount;
import net.tnemc.plugincore.PluginCore;
import net.tnemc.plugincore.core.compatibility.log.DebugLevel;
import net.tnemc.plugincore.core.id.UUIDPair;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One-time repair for Towny bank accounts that ended up with several copies.
 * <p>
 * Older builds matched town/nation accounts by name, so the same town could get several account
 * files (one per old id, one per town that fell and was refounded with that name). On startup they
 * all loaded into the same account and whichever loaded last replaced the balance.
 * <p>
 * For every town and nation that exists in Towny, the copy that was saved most recently is the one
 * that was actually in use. Its balance is moved to the town's real Towny UUID. All other copies
 * are moved to accounts-archive/ (never deleted outright) and listed in a report. Accounts of towns
 * that no longer exist are left untouched.
 *
 * @author ciel0beats
 * @since 0.1.5.4
 */
public final class TownyAccountRepair {

  private static final String MARKER = "towny-account-repair.done";

  private TownyAccountRepair() {
  }

  /**
   * Runs the repair unless it already ran on this server.
   */
  public static void runOnce() {

    final File directory = PluginCore.directory();
    final File marker = new File(directory, MARKER);
    if(marker.exists()) return;

    final List<Town> towns;
    final List<Nation> nations;
    try {
      towns = new ArrayList<>(TownyAPI.getInstance().getTowns());
      nations = new ArrayList<>(TownyAPI.getInstance().getNations());
    } catch(final Throwable e) {
      PluginCore.log().error("Towny account repair skipped, Towny isn't ready: " + e.getMessage(), DebugLevel.OFF);
      return;
    }

    if(towns.isEmpty()) {
      //Towny hasn't loaded its towns yet. Don't mark the repair as done, try again next startup.
      PluginCore.log().error("Towny account repair skipped, Towny has no towns loaded.", DebugLevel.OFF);
      return;
    }

    //Group every non-player account by name before changing anything.
    final Map<String, List<Account>> byName = new HashMap<>();
    for(final Account account : new ArrayList<>(TNECore.eco().account().getAccounts().values())) {

      if(account instanceof PlayerAccount || account.getName() == null) continue;
      byName.computeIfAbsent(account.getName().toLowerCase(Locale.ROOT), k->new ArrayList<>()).add(account);
    }

    final List<String> report = new ArrayList<>();
    report.add("Towny account repair - " + new Date());
    report.add("For each town/nation: the copy that was in use (most recently saved) is kept on the Towny UUID.");
    report.add("Other copies were moved to accounts-archive/.");
    report.add("");

    int repaired = 0;
    for(final Town town : towns) {
      if(repair(TownySettings.getTownAccountPrefix() + town.getName(), town.getUUID(), byName, report)) repaired++;
    }
    for(final Nation nation : nations) {
      if(repair(TownySettings.getNationAccountPrefix() + nation.getName(), nation.getUUID(), byName, report)) repaired++;
    }

    report.add("");
    report.add("Repaired: " + repaired);

    try {
      final String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
      Files.write(new File(directory, "towny-account-repair-" + stamp + ".txt").toPath(), report, StandardCharsets.UTF_8);
      Files.writeString(marker.toPath(), "Ran " + new Date() + System.lineSeparator(), StandardCharsets.UTF_8);
    } catch(final IOException e) {
      PluginCore.log().error("Unable to write the Towny account repair report: " + e.getMessage(), DebugLevel.OFF);
    }

    PluginCore.log().inform("Towny account repair finished. Repaired " + repaired + " town/nation accounts. See the towny-account-repair report in the TNE folder.");
  }

  private static boolean repair(final String name, final UUID target, final Map<String, List<Account>> byName, final List<String> report) {

    final List<Account> copies = new ArrayList<>(byName.getOrDefault(name.toLowerCase(Locale.ROOT), List.of()));

    final Account atTarget = TNECore.eco().account().getAccounts().get(target.toString());
    if(atTarget != null && !copies.contains(atTarget)) {
      copies.add(atTarget);
    }

    if(copies.isEmpty()) return false;
    if(copies.size() == 1 && copies.get(0).getIdentifier().equals(target)) return false;

    //The copy that was in use is the one saved most recently. Ties: prefer the Towny UUID, then the
    //larger balance.
    final Account live = copies.stream().max(Comparator
            .comparingLong((Account a)->YAMLAccount.lastSaved(a.getIdentifier().toString()).orElse(0L))
            .thenComparing((Account a)->a.getIdentifier().equals(target))
            .thenComparing(TownyAccountRepair::total)).orElseThrow();

    Account keeper = atTarget;
    if(keeper == null) {

      final Optional<SharedAccount> created = TNECore.eco().account().createNonPlayerAccount(target.toString(), name);
      if(created.isEmpty() || !created.get().getIdentifier().equals(target)) {
        report.add(name + ": FAILED to create the account for Towny UUID " + target + ". Nothing changed.");
        return false;
      }
      keeper = created.get();
      keeper.setCreationDate(live.getCreationDate());
      TNECore.eco().account().getAccounts().put(target.toString(), keeper);
    }

    report.add(name + " -> " + target + ": balance " + total(live).toPlainString() + " (from " + live.getIdentifier() + ", saved " + saved(live) + ")");

    if(keeper != live) {

      final Wallet wallet = new Wallet();
      for(final HoldingsEntry entry : live.getWallet().entryList()) {
        wallet.setHoldings(new HoldingsEntry(entry.getRegion(), entry.getCurrency(), entry.getAmount(), entry.getHandler()));
      }
      keeper.setWallet(wallet);
    }

    for(final Account copy : copies) {

      if(copy == keeper) continue;

      final String id = copy.getIdentifier().toString();
      if(!archive(id)) {
        report.add("   kept " + id + " (balance " + total(copy).toPlainString() + "), could not archive its file");
        TNECore.eco().account().getAccounts().remove(id);
        continue;
      }

      report.add("   archived " + id + " (balance " + total(copy).toPlainString() + ", saved " + saved(copy) + ")");
      TNECore.eco().account().deleteAccount(id);
    }

    TNECore.eco().account().uuidProvider().store(new UUIDPair(target, name));
    TNECore.instance().storage().store(keeper, target.toString());
    return true;
  }

  /**
   * Copies an account's file to accounts-archive/ so it can be restored by hand.
   *
   * @return True if the file was archived, or there was no file to archive.
   */
  private static boolean archive(final String id) {

    final File source = new File(PluginCore.directory(), "accounts/" + id + ".yml");
    if(!source.exists()) return true;

    final File folder = new File(PluginCore.directory(), "accounts-archive");
    try {
      Files.createDirectories(folder.toPath());
      Files.copy(source.toPath(), new File(folder, id + ".yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch(final IOException e) {
      PluginCore.log().error("Unable to archive account file " + id + ": " + e.getMessage(), DebugLevel.OFF);
      return false;
    }
  }

  private static BigDecimal total(final Account account) {

    BigDecimal total = BigDecimal.ZERO;
    for(final HoldingsEntry entry : account.getWallet().entryList()) {
      total = total.add(entry.getAmount());
    }
    return total;
  }

  private static String saved(final Account account) {

    return YAMLAccount.lastSaved(account.getIdentifier().toString()).map(time->new Date(time).toString()).orElse("unknown");
  }
}
