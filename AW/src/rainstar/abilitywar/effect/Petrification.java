package rainstar.abilitywar.effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@EffectManifest(name = "석화", displayName = "§8석화", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION
}, description = {
		"§f이동과 시야 전환이 불가능해집니다.",
		"§f공격할 수 없고, 대미지를 99% 경감하여 받습니다.",
		"§f7번째 피해는 석화를 해제시키고 3배의 피해를 입습니다.",
		"§f웅크리기를 연타하여 저항해, 지속시간을 조금씩 줄일 수 있습니다."
})
public class Petrification extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Petrification> registration = EffectRegistry.registerEffect(Petrification.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;
	private int stack = 0;
	private boolean lift = false;
	private final Block block;
	private final IBlockSnapshot snapshot;

	public Petrification(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomNameVisible(true);
		hologram.setCustomName("§8[§7석화§8]");
		setPeriod(TimeUnit.TICKS, 1);
		this.block = participant.getPlayer().getEyeLocation().getBlock().getRelative(BlockFace.DOWN);
		snapshot = Blocks.createSnapshot(block);
		block.setType(Material.STONE);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getBlock().equals(block)) e.setCancelled(true);
	}

	@EventHandler
	public void onExplode(BlockExplodeEvent e) {
		e.blockList().removeIf(blocks -> blocks.equals(block));
	}

	@EventHandler
	public void onExplode(EntityExplodeEvent e) {
		e.blockList().removeIf(blocks -> blocks.equals(block));
	}
	
	@EventHandler
	private void onPlayerMove(final PlayerMoveEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			e.setTo(e.getFrom());
		}
	}

	@EventHandler
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(participant.getPlayer()) && !e.isCancelled()) {
			stack++;
			e.setDamage(stack < 7 ? e.getDamage() * 0.01 : e.getDamage() * 3);
			if (stack >= 7) {
				SoundLib.ENTITY_PLAYER_ATTACK_STRONG.playSound(participant.getPlayer().getLocation(), 1, 1.15f);
				ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getEyeLocation(), .3f, .3f, .3f, 100, 0.5, MaterialX.STONE);
				this.stop(false);
			}
		}
	}
	
	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(participant.getPlayer()) && !e.isCancelled()) {
			stack++;
			e.setDamage(stack < 7 ? e.getDamage() * 0.01 : e.getDamage() * 3);
			if (stack >= 7) {
				SoundLib.ENTITY_PLAYER_ATTACK_STRONG.playSound(participant.getPlayer().getLocation(), 1, 1.15f);
				ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getEyeLocation(), .3f, .3f, .3f, 100, 0.5, MaterialX.STONE);
				this.stop(false);
			}
		}
	}
	
	@EventHandler
	private void onSwapHand(PlayerSwapHandItemsEvent e) {
		if (e.getPlayer().equals(participant.getPlayer())) e.setCancelled(true);
	}
	
	@EventHandler
	private void onSlotChange(final PlayerItemHeldEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) e.setCancelled(true);
	}
	
	@Override
	protected void run(int count) {
		super.run(count);
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		
		if (participant.getPlayer().isSneaking() && !lift) {
			setCount(getCount() - 1);
			lift = true;
		} else if (!participant.getPlayer().isSneaking() && lift) lift = false;
	}

	@Override
	protected void onEnd() {
		hologram.remove();
		snapshot.apply();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		hologram.remove();
		snapshot.apply();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}

}