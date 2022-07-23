package RainStarAbility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "응급처치", rank = Rank.B, species = Species.HUMAN, explain = {
		"$[INBATTLE_CHECK_DURATION]초간 피해를 주거나 받지 않으면 §a비전투 상태§f로 간주합니다.",
		"§a비전투 상태§f에서 철괴 우클릭 시 가장 마지막으로 입은 $[HEAL_COUNT]번의 피해를 §d회복§f합니다.",
		"§c전투 상태§f에서 사용하면 $[DECREASED_HEAL_COUNT]번 회복하고 $[COOLDOWN]를 가집니다."
		})

public class FirstAid extends AbilityBase implements ActiveHandler {
	
	public FirstAid(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> INBATTLE_CHECK_DURATION = 
			abilitySettings.new SettingObject<Double>(FirstAid.class, "inbattle-check-duration", 7.5,
            "# 전투 중인지 판단하는 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> HEAL_COUNT = 
			abilitySettings.new SettingObject<Integer>(FirstAid.class, "heal-count", 6,
            "# 비전투 중에 회복하는 횟수") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DECREASED_HEAL_COUNT = 
			abilitySettings.new SettingObject<Integer>(FirstAid.class, "decreased-heal-count", 3,
            "# 전투 중에 회복하는 횟수") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(FirstAid.class, "cooldown", 65,
            "# 전투 중 사용시 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
	private ActionbarChannel ac = newActionbarChannel();
	private List<Double> damages = new ArrayList<>();
	private final int healcount = HEAL_COUNT.getValue();
	private final int dechealcount = DECREASED_HEAL_COUNT.getValue();
	private final int duration = (int) (INBATTLE_CHECK_DURATION.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	ac.update("§a비전투 중");
	    }
	}
	
	private final AbilityTimer inbattle = new AbilityTimer(duration) {
		
		@Override
		public void onStart() {
			ac.update("§c전투 중");
		}
		
		@Override
		public void run(int count) {
			ac.update("§c전투 중");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update("§a비전투 중");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer healing = new AbilityTimer(healcount) {
		
		@Override
		public void run(int count) {
			if (damages.size() > 0) {
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), damages.get(0), RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + damages.get(0));
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.8f);
					ParticleLib.HEART.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
					new Holograms(getPlayer().getLocation(), damages.get(0));
				}
				damages.remove(0);
			} else stop(false);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			damages.clear();
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			if (inbattle.isRunning()) {
				if (damages.size() <= 0) getPlayer().sendMessage("§4[§c!§4] §f마지막 전투에서 입은 피해 기록이 없습니다.");
				else {
					healing.start();
					healing.setCount(dechealcount);
					cooldown.start();
				}
			} else {
				if (damages.size() <= 0) getPlayer().sendMessage("§4[§c!§4] §f마지막 전투에서 입은 피해 기록이 없습니다.");
				else return healing.start();	
			}
		}
		return false;
	}
	
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
    	if ((damager != null && e.getEntity().equals(getPlayer())) || getPlayer().equals(damager)) {
    		if (inbattle.isRunning()) inbattle.setCount(duration);
    		else inbattle.start();
    	}
    }
    
    @SubscribeEvent(priority = 1000)
    public void onDamageCollector(EntityDamageByEntityEvent e) {
    	if (e.getEntity().equals(getPlayer()) && !healing.isRunning()) {
    		damages.add(e.getFinalDamage());
    		if (damages.size() >= healcount + 1) {
    			damages.remove(0);
    		}
    	}
    }
    
	private class Holograms extends AbilityTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat healDF = new DecimalFormat("0.00");
		private double heal;
		
		private Holograms(Location hitLoc, double heal) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(getPlayer().getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					"§a§l" + healDF.format(heal));
			this.heal = heal;
			for (Player player : getPlayer().getWorld().getPlayers()) {
				hologram.display(player);
			}	
		}
		
		@Override
		protected void run(int count) {
			hologram.setText((count <= 20 ? "§a§l" : "§d§l") + healDF.format(heal));
			hologram.teleport(hologram.getLocation().add(0, 0.03, 0));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}

}
