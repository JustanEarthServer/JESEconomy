package net.tnemc.paper.command;
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

import net.tnemc.plugincore.PluginCore;
import net.tnemc.plugincore.core.compatibility.CmdSource;
import net.tnemc.plugincore.core.compatibility.PlayerProvider;
import net.tnemc.plugincore.core.io.message.MessageData;
import net.tnemc.plugincore.core.io.message.MessageHandler;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.Optional;

/**
 * Command source that messages through Paper's native Adventure support. Replaces PaperCommandSource,
 * which goes through adventure-platform-bukkit and breaks on Adventure 5.
 *
 * @author CielDev
 * @since 0.1.4.0
 */
public class PaperCommandSource extends CmdSource<BukkitCommandActor> {

  private final PlayerProvider provider;

  public PaperCommandSource(final BukkitCommandActor actor) {

    super(actor);

    this.provider = (actor.isPlayer() && actor.asPlayer() != null)? PluginCore.server().initializePlayer(actor.asPlayer()) : null;
  }

  @Override
  public boolean isPlayer() {

    return actor.isPlayer();
  }

  @Override
  public Optional<PlayerProvider> player() {

    return Optional.ofNullable(provider);
  }

  @Override
  public void message(final MessageData messageData) {

    MessageHandler.translate(messageData, identifier().orElse(null), actor.sender());
  }
}
