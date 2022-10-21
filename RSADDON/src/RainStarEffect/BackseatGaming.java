package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@EffectManifest(name = "훈수", displayName = "§c훈수", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.SIGHT_RESTRICTION
}, description = {
		"플레이어가 특정 행동들을 할 때마다 확률적으로 훈수를 둡니다.",
		"훈수를 받으면 시야가 잠깐 봉인됩니다.",
		"이 효과는 자동 영구 지속 효과입니다."
})
public class BackseatGaming extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<BackseatGaming> registration = EffectRegistry.registerEffect(BackseatGaming.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Player player;
	private Random random = new Random();
			
	public BackseatGaming(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.player = participant.getPlayer();
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		setCount(20);
	}

	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(player)) {
			if (random.nextInt(5) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 싸움 그렇게 하는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (e.getEntity().equals(player)) {
			if (random.nextInt(10) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 활 그렇게 쏘는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(5) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 템 그렇게 떨구는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(5) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 스왑 그렇게 하는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(20) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 블록 그렇게 부수는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(20) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 블록 그렇게 설치하는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onVehicleEnter(VehicleEnterEvent e) {
		if (e.getEntered().equals(player)) {
			if (random.nextInt(2) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 그렇게 타는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	private void onAbilityActiveSkill(AbilityActiveSkillEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(2) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 능력 그렇게 쓰는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.1f);
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.3f);
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.7f);
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 0.7f);
					player.sendMessage("§3[§b능력§3] §c개못하네 ㅋ");
				}
			}.runTaskLater(AbilityWar.getPlugin(), 60L);
			stop(false);
		}
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(player)) {
			if (random.nextInt(1000) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 무빙 그렇게 치는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	public void onEntityPickupItem(EntityPickupItemEvent e) {
		if (e.getEntity().equals(player)) {
			if (random.nextInt(20) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 파밍 그렇게 하는 거 아닌데~");
			}
		}
	}
	
	@EventHandler
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(player)) {
			if (random.nextInt(15) == 0) {
				PotionEffects.BLINDNESS.addPotionEffect(player, 40, 0, true);
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
				player.sendMessage("§3[§b능력§3] §c아~ 회복 그렇게 하는 거 아닌데~");
			}
		}
	}
	
	@Override
	protected void onEnd() {
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}