package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.BindingSmoke;

@AbilityManifest(name = "암습", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §7힛 앤 런§f: 적을 §a근접 공격§f하면 대상에게 §c폭발 표식§f을 부여합니다.",
		" §c폭발 표식§f은 최대 3개까지 중첩해 부여할 수 있으며, 5§7/§f3§7/§f1초마다 1초의",
		" §8§n속박의 연막§f을 대상에게 부여합니다.",
		"§7철괴 우클릭 §8- §3비열한 도주§f: 1초간 §b비행§f합니다. $[COOLDOWN]",
		" 자신이 이번 게임 동안 §c폭발 표식§f을 부여한 횟수만큼 §b비행 시간§f이 0.2초 늘어납니다.",
		"§7철괴 좌클릭 §8- §c기폭!§f: 사거리 $[RANGE]칸 밖에 있는 모든 대상의",
		" §c폭발 표식§f을 터뜨립니다. §c표식§f 하나당 $[EXPLODE]f의 위력으로 폭발하며,",
		" 대상이 §8§n속박의 연막§f 상태였다면 $[STUN]초간 §e§n기절§f시킵니다.",
		"§b[§7아이디어 제공자§b] §emes_seu§f"
		})
public class Sneak extends Synergy implements ActiveHandler {
	
	public Sneak(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN =
			rsSynergySettings.new SettingObject<Integer>(Sneak.class, "cooldown", 45,
			"# 비열한 도주의 쿨타임", "# 단위: 초") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};

	public static final SettingObject<Double> RANGE =
			rsSynergySettings.new SettingObject<Double>(Sneak.class, "range", 15.0,
			"# 기폭 최소 발동 거리", "# 단위: 칸") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> EXPLODE =
			rsSynergySettings.new SettingObject<Double>(Sneak.class, "explode", 0.8,
			"# 표식 하나당 폭발 위력", "# 단위: f") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> STUN =
			rsSynergySettings.new SettingObject<Double>(Sneak.class, "stun", 3.0,
			"# 속박의 연막 상태에서 기폭 시 기절 시간", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double range = RANGE.getValue();
	private final double explode = EXPLODE.getValue();
	private final int stun = (int) (STUN.getValue() * 20);
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private int flyingStack = 0;
	private final Crescent crescent = Crescent.of(0.85, 20);
	private int particleSide = 15;
    private final Circle circle = Circle.of(range, (int) Math.min(range * 12.5, 200));
    private static final List<RGB> gradation = Gradient.createGradient(30, RGB.of(77, 77, 77), RGB.of(254, 223, 9));
	private int colorstack = 0;
	private boolean colordirection = false;
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
    
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {			
				if (colorstack == 29 || colorstack == 0) colordirection = !colordirection;
				colorstack = colordirection ? Math.min(colorstack + 1, 29) : Math.max(colorstack - 1, 0);
				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation.get(colorstack));
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK) && !fly.isDuration() && !cooldown.isCooldown()) {
	    		fly.start();
	    		fly.setCount(20 + (flyingStack * 4));
	    		return true;
	    	}
	    	if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		boolean used = false;
	    		for (Player player : new ArrayList<>(stackMap.keySet())) {
	    			if (getPlayer().getLocation().distanceSquared(player.getLocation()) >= range * range) {
	    				stackMap.get(player).explode();
	    				if (!used) used = true;
	    			}
	    		}
	    		
	    		if (used) return true;
	    	}
	    }
	    return false;
	}
	
    private final Duration fly = new Duration(20 + (flyingStack * 4), cooldown) {

        @Override
        public void onDurationProcess(int seconds) {
            getPlayer().setAllowFlight(true);
            getPlayer().setFlying(true);
        }

        @Override
        public void onDurationEnd() {
            getPlayer().setFlying(false);
            GameMode mode = getPlayer().getGameMode();
            getPlayer().setAllowFlight(mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE);
        }

        @Override
        protected void onDurationSilentEnd() {
            getPlayer().setFlying(false);
            GameMode mode = getPlayer().getGameMode();
            getPlayer().setAllowFlight(mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE);
        }

    }.setPeriod(TimeUnit.TICKS, 1);
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			if (stackMap.containsKey(e.getEntity())) {
				if (stackMap.get(e.getEntity()).getCount() < 590) {
					stackMap.get(e.getEntity()).addStack();
				}
			} else new Stack((Player) e.getEntity()).start();
			new CutParticle(getPlayer(), particleSide).start();
			particleSide *= -1;
		}
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				e.setCancelled(true);
				e.getEntity().sendMessage("§a낙하 대미지를 받지 않습니다.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound((Player) e.getEntity());
			}
		}
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§c✸", stack).concat(Strings.repeat("§7✸", 3 - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			
			int period = 140 - (stack * 40);
			if (count % period == 0 && getGame().isParticipating(player) && !(getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(player.getUniqueId()))) BindingSmoke.apply(getGame().getParticipant(player), TimeUnit.TICKS, 20);
		}

		private void addStack() {
			setCount(200);
			if (stack < 3) {
				flyingStack++;
				stack++;
			}
			hologram.setText(Strings.repeat("§c✸", stack).concat(Strings.repeat("§7✸", 3 - stack)));
		}
		
		private void explode() {
			player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), (float) (stack * explode), false, false);
			if (getGame().isParticipating(player) && getGame().getParticipant(player).hasEffect(BindingSmoke.registration)) Stun.apply(getGame().getParticipant(player), TimeUnit.TICKS, stun);
			stop(false);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;
		private final Entity entity;

		private CutParticle(Entity entity, double angle) {
			super(1);
			setPeriod(TimeUnit.TICKS, 1);
			this.entity = entity;
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(entity.getLocation().getDirection().setY(0).normalize(), 90), entity.getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = entity.getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-entity.getLocation().getYaw())
					.rotateAroundAxis(entity.getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -15);
		}
		
		@Override
		protected void run(int count) {
			Location baseLoc = entity.getLocation().clone().add(vector).add(0, 0.45, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(baseLoc, 0.75f, 0.5f);
		}

	}

}
