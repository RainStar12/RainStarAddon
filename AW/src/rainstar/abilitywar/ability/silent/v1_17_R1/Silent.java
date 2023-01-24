package rainstar.abilitywar.ability.silent.v1_17_R1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil.FieldUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.network.syncher.DataWatcher.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import rainstar.abilitywar.ability.silent.AbstractSilent;

public class Silent extends AbstractSilent {

	private static final DataWatcherObject<Byte> BYTE_DATA_WATCHER_OBJECT;
	private static final List<com.mojang.datafixers.util.Pair<EnumItemSlot, ItemStack>> NULL_PAIR_LIST = Arrays.asList(
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.a, ItemStack.b),
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.b, ItemStack.b),
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.c, ItemStack.b),
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.d, ItemStack.b),
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.e, ItemStack.b),
			com.mojang.datafixers.util.Pair.of(EnumItemSlot.f, ItemStack.b)
	);

	static {
		try {
			BYTE_DATA_WATCHER_OBJECT = FieldUtil.getStaticValue(Entity.class, "Z");
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private final Set<UUID> affectPlayers = new HashSet<>();
	private final Map<UUID, Pair<CraftPlayer, ChannelOutboundHandlerAdapter>> channelHandlers = new HashMap<>();

	public Silent(Participant participant) {
		super(participant);
	}

	@SubscribeEvent
	private void onJoin(PlayerJoinEvent e) {
		if (affectPlayers.contains(e.getPlayer().getUniqueId())) {
			final CraftPlayer player = (CraftPlayer) e.getPlayer();
			new BukkitRunnable() {
				@Override
				public void run() {
					player.getHandle().b.sendPacket(new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), NULL_PAIR_LIST));
					injectPlayer(player);
				}
			}.runTaskLater(AbilityWar.getPlugin(), 2L);	
		}
	}

	@SubscribeEvent
	private void onQuit(PlayerQuitEvent e) {
		if (affectPlayers.contains(e.getPlayer().getUniqueId())) {
			final CraftPlayer player = (CraftPlayer) e.getPlayer();
			if (channelHandlers.containsKey(player.getUniqueId())) {
				try {
					player.getHandle().b.a.k.pipeline().remove(channelHandlers.remove(player.getUniqueId()).getRight());
				} catch (NoSuchElementException ignored) {
				}
			}	
		}
	}

	@Override
	protected void hide0(Player player) {
		affectPlayers.add(player.getUniqueId());
		final CraftPlayer craftPlayer = (CraftPlayer) getPlayer();
		craftPlayer.getHandle().getDataWatcher().set(new DataWatcherObject<>(13, DataWatcherRegistry.b), 0);
		final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), NULL_PAIR_LIST);
		((CraftPlayer) player).getHandle().b.sendPacket(packet);
		injectPlayer((CraftPlayer) player);
	}

	@Override
	protected void show0(Player player) {
		affectPlayers.remove(player.getUniqueId());
		final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(getPlayer().getEntityId(), Arrays.asList(
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.a, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInMainHand())),
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.b, CraftItemStack.asNMSCopy(getPlayer().getInventory().getItemInOffHand())),
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.c, CraftItemStack.asNMSCopy(getPlayer().getInventory().getBoots())),
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.d, CraftItemStack.asNMSCopy(getPlayer().getInventory().getLeggings())),
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.e, CraftItemStack.asNMSCopy(getPlayer().getInventory().getChestplate())),
				com.mojang.datafixers.util.Pair.of(EnumItemSlot.f, CraftItemStack.asNMSCopy(getPlayer().getInventory().getHelmet()))
		));
		try {
			((CraftPlayer) player).getHandle().b.a.k.pipeline().remove(channelHandlers.get(player.getUniqueId()).getRight());
		} catch (NoSuchElementException ignored) {
		}
		if (((CraftPlayer) player).isValid()) {
			((CraftPlayer) player).getHandle().b.sendPacket(packet);
			channelHandlers.remove(player.getUniqueId());	
		}
	}

	private void injectPlayer(CraftPlayer player) {
		if (!player.isValid()) return;
		if (channelHandlers.containsKey(player.getUniqueId())) {
			final Pair<CraftPlayer, ChannelOutboundHandlerAdapter> pair = channelHandlers.get(player.getUniqueId());
			if (!pair.getLeft().isValid()) {
				try {
					pair.getLeft().getHandle().b.a.k.pipeline().remove(pair.getRight());
				} catch (NoSuchElementException ignored) {
				}
			} else return;
		}
		final ChannelOutboundHandlerAdapter handler = new ChannelOutboundHandlerAdapter() {
			@Override
			public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
				if (packet instanceof PacketPlayOutEntityEquipment) {
					if ((int) FieldUtil.getValue(packet, "b") == getPlayer().getEntityId()) {
						FieldUtil.setValue(packet, "c", NULL_PAIR_LIST);
					}
				} else if (packet instanceof PacketPlayOutEntityMetadata) {
					if ((int) FieldUtil.getValue(packet, "a") == getPlayer().getEntityId()) {
						List<Item<?>> items = FieldUtil.getValue(packet, "b");
						if (items.size() != 0) {
							Item<?> item = items.get(0);
							if (BYTE_DATA_WATCHER_OBJECT.equals(item.a())) {
								Item<Byte> byteItem = (Item<Byte>) item;
								byteItem.a((byte) (byteItem.b() | 1 << 5));
							}
						}
					}
				}
				super.write(ctx, packet, promise);
			}
		};
		channelHandlers.put(player.getUniqueId(), Pair.of(player, handler));
		player.getHandle().b.a.k.pipeline().addBefore("packet_handler", hashCode() + ":" + player.getName(), handler);
	}

}
