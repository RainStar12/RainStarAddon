package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "카미카제", rank = Rank.A, species = Species.HUMAN, explain = {
		"가장 가까운 대상을 향해 추적하며, $[PERIOD]초마다 §c자폭§f하는 §2좀비§f를 소환합니다.",
		"자신과 §2좀비§f는 §c폭발 피해§f에 면역이며, 사망 시 $[COOLDOWN]초 이후 재소환합니다.",
		"§2좀비§f의 §b이동 속도§f와 §c폭발력§f은 내 현재 체력에 반비례합니다§8(§7낮을수록 증가§8)§f."
		})
public class Kamikaze extends Synergy {
	
	public Kamikaze(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD = rsSynergySettings.new SettingObject<Double>(Kamikaze.class, 
			"period", 7.0, "# 자폭 주기", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = rsSynergySettings.new SettingObject<Integer>(Kamikaze.class, 
			"cooldown", 28, "# 좀비 재소환 시간", "# 단위: 초") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final int period = getTime((int) (PERIOD.getValue() * 20));
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
	private Zombie zombie;
	private List<Location> log = new ArrayList<>();
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private int getTime(int baseTime) {
		double decrease = Wreck.calculateDecreasedAmount(100);
		if (Wreck.isEnabled(GameManager.getGame())) {
			if (decrease == 100) return 1;
			else return (int) (decrease * baseTime);
		} else return baseTime;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@SuppressWarnings("deprecation")
		@Override
		public void run(int count) {
			double nowHealth = getPlayer().getHealth(), maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if ((zombie == null || zombie.isDead()) && !cooldown.isRunning()) {
	    		zombie = getPlayer().getWorld().spawn(getPlayer().getLocation(), Zombie.class);
				if (ServerVersion.getVersion() >= 16) zombie.setAdult();
				else zombie.setBaby(false);
				
				zombie.getEquipment().setItemInMainHand(null);
				zombie.getEquipment().setArmorContents(getPlayer().getInventory().getArmorContents());
				zombie.getEquipment().setItemInMainHandDropChance(0);
				zombie.getEquipment().setBootsDropChance(0);
				zombie.getEquipment().setChestplateDropChance(0);
				zombie.getEquipment().setHelmetDropChance(0);
				zombie.getEquipment().setLeggingsDropChance(0);
				zombie.setCustomName("§c" + getPlayer().getName() + "의 자폭병");
				zombie.setCustomNameVisible(true);
				zombie.setSilent(true);
				
				zombie.setHealth(maxHealth);
			}
			
			if (zombie != null) {
				log.add(zombie.getLocation().clone());
				while (log.size() > 5) log.remove(0);
				drawLog();
				Player nearest = LocationUtil.getNearestEntity(Player.class, zombie.getLocation(), predicate);
				if (nearest != null) {
					Vector dir = nearest.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize().multiply(((1 - (nowHealth / maxHealth)) * 1.55) + 0.2);
					zombie.setVelocity(dir);
				}
				zombie.setTarget(nearest);
				if (!exploding.isRunning()) exploding.start();
				ac.update("§2좀비§7: §5X " + df.format(zombie.getLocation().getX()) + " §7| §5Y " + df.format(zombie.getLocation().getY()) + " §7| §5Z " + df.format(zombie.getLocation().getZ()));
			} else ac.update("§2좀비§7: §5사망");
		}
		
		@Override
		public void onSilentEnd() {
			if (zombie != null && !zombie.isDead()) zombie.setHealth(0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer exploding = new AbilityTimer(period) {
		
		@Override
		public void run(int count) {
			if (zombie != null && !zombie.isDead()) {
				if (count % 5 == 0) {
					if (count % 10 == 0) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(zombie.getLocation(), 1, 1.4f);
	    			else SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(zombie.getLocation(), 1, 1.7f);
				}
				switch(count) {
				case 39:
				case 26:
				case 13:
				case 1:
					SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(zombie.getLocation(), 0.5f, 2f);
					zombie.setGlowing(true);
					break;
				case 34:
				case 21:
				case 8:
					zombie.setGlowing(false);
					break;
				}
			}
		}
		
		@Override
		public void onEnd() {
			if (zombie != null && !zombie.isDead()) {
				double nowHealth = getPlayer().getHealth(), maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				ParticleLib.EXPLOSION_HUGE.spawnParticle(zombie.getLocation(), 0, 0, 0, 2, 1);
				SoundLib.ENTITY_GENERIC_EXPLODE.playSound(zombie.getLocation(), 1, 1);
				getPlayer().getWorld().createExplosion(zombie.getLocation().getX(), zombie.getLocation().getY(), zombie.getLocation().getZ(), (float) ((1 - (nowHealth / maxHealth)) * 5), false, true);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) || (zombie != null && !zombie.isDead() && e.getEntity().equals(zombie))) {
			if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	private void onEntityDeath(EntityDeathEvent e) {
		if (zombie != null && e.getEntity().equals(zombie)) {
			cooldown.start();
			log.clear();
		}
	}
	
	private void drawLog() {
	    if (log.size() < 2) return;

	    for (int i = 1; i < log.size(); i++) {
	        Location from = log.get(i - 1);
	        Location to = log.get(i);

	        Vector between = to.toVector().subtract(from.toVector());
	        double length = between.length();

	        if (length == 0) continue;

	        Vector unit = between.normalize().multiply(0.2);

	        for (double d = 0; d < length; d += 0.1) {
	            Location particleLoc = from.clone().add(unit.clone().multiply(d));
	            ParticleLib.SMOKE_NORMAL.spawnParticle(particleLoc, 0, 0, 0, 1, 0);
	            ParticleLib.ITEM_CRACK.spawnParticle(particleLoc, 0, 0, 0, 1, 0.4, MaterialX.REDSTONE_BLOCK);
	        }
	    }
	}
	
}
