package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "스구리", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7수치 §8- §c히트§f: §c히트§f가 §e100§c%§f 이상이면 피격 시 §6잔열§f이 추가로 증가합니다.",
		" 최대 §e300§c%§f까지 증가하고, 대시 중 §e250§c%§f 이상이라면 §6잔열§f이 쌓입니다.",
		"§7수치 §8- §6잔열§f: 증가할 때 체력이 감소하고, 감소할 때 체력이 증가합니다.",
		" §6잔열§f은 최대 체력을 차지해, §6잔열§f만큼의 최대 체력은 채울 수 없습니다.",
		"§7검 우클릭 §8- §3대시§f: 바라보는 방향으로 지속 대시합니다. §c히트§f 수치가 지속 상승하며,",
		" 사용 직후엔 §c히트§f, 대시 거리가 추가 증가하고 $[EVADE]초간 공격을 회피합니다.",
		" 회피한 피해량에 비례하여, §6잔열§f은 감소하고, §b액셀러레이터§f가 충전됩니다.",
		"§7철괴 우클릭 §8- §b액셀러레이터§f: 게이지를 모으면 사용할 수 있습니다.",
		" $[ACCELERATOR_DURATION]초간 §c히트§f가 §e0§c%§f가 되고, §6잔열§f이 빠르게 감소합니다.",
		" 또한 공격에 1~6의 추가 피해를 줍니다."
		},
		summarize = {
		"§7검 우클릭§f으로 지속 대시할 수 있습니다. 대시 시작 직후에는 모든 공격을",
		"회피할 수 있으며 회피한 피해량만큼 §6잔열§f은 감소, 스킬 게이지는 모여듭니다.",
		"또한 대시 간 §c히트§f 수치가 지속적으로 올라 피격 시 §6잔열§f이 증가합니다.",
		"§6잔열§f은 수치만큼의 최대 체력을 차지합니다.",
		"스킬 게이지를 모아 §7철괴 우클릭§f으로 §c히트§f와 §6잔열§f을 지속 감소시킵니다."
		})

public class Suguri extends AbilityBase implements ActiveHandler {
	
	public Suguri(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> EVADE = 
			abilitySettings.new SettingObject<Double>(Suguri.class, "evade", 0.8,
			"# 대시 직후 회피 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> ACCELERATOR_DURATION = 
			abilitySettings.new SettingObject<Double>(Suguri.class, "accelerator-duration", 7.5,
			"# 액셀러레이터 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DASH_START_HEAT_UP = 
			abilitySettings.new SettingObject<Integer>(Suguri.class, "dash-start-heat-up", 50,
			"# 대시 시작 시 추가 히트") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> DASH_RUN_HEAT_UP = 
			abilitySettings.new SettingObject<Double>(Suguri.class, "dash-run-heat-up", 20.0,
			"# 대시 유지 시 초당 증가하는 히트량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		heatupdater.start();
    		ac.update("§6잔열§f: " + df2.format(longHeat));
    	}
    }
    
	private static final Set<Material> swords;
	private final int evadeduration = (int) (EVADE.getValue() * 20);
	private final int duration = (int) (ACCELERATOR_DURATION.getValue() * 20);
	private final int dashstartheat = DASH_START_HEAT_UP.getValue();
	private final double dashrunningheat = DASH_RUN_HEAT_UP.getValue() / 20.0;
	
	private RGB rainbow;
	private int rainbowstack = 0;

	private final RGB rainbow1 = RGB.of(216, 103, 221), rainbow2 = RGB.of(187, 114, 211), rainbow3 = RGB.of(108, 125, 251),
			rainbow4 = RGB.of(117, 183, 252), rainbow5 = RGB.of(126, 241, 172), rainbow6 = RGB.of(228, 192, 140),
			rainbow7 = RGB.of(254, 254, 101), rainbow8 = RGB.of(241, 221, 111), rainbow9 = RGB.of(241, 175, 105),
			rainbow10 = RGB.of(235, 144, 161), rainbow11 = RGB.of(232, 123, 202), rainbow12 = RGB.of(198, 113, 237);

	@SuppressWarnings("serial")
	private List<RGB> rainbows = new ArrayList<RGB>() {
		{
			add(rainbow1);
			add(rainbow2);
			add(rainbow3);
			add(rainbow4);
			add(rainbow5);
			add(rainbow6);
			add(rainbow7);
			add(rainbow8);
			add(rainbow9);
			add(rainbow10);
			add(rainbow11);
			add(rainbow12);
		}
	};
	
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0");
	private final DecimalFormat df2 = new DecimalFormat("0.0");
	private final DecimalFormat df3 = new DecimalFormat("0.0");
	private final Random random = new Random();
	private double heat = 0;
	private BossBar bossBar = null;
	private long last;
	private double longHeat = 0;
	private double accelgauge = 0;
	private final Circle circle = Circle.of(1, 25);

	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !accelerator.isRunning()) {
			if (accelgauge >= 1) {
				accelgauge = 0;
				return accelerator.start();
			} else getPlayer().sendMessage("§c[§b!§c] §f아직 게이지가 모자랍니다. §b현재 게이지§f: §e" + df3.format(accelgauge));
		}
		return false;
	}
	
	private AbilityTimer evade = new AbilityTimer(evadeduration) {
		
		private double y;
		private boolean add;
		
		@Override
		public void run(int count) {
			if (rainbowstack >= 11) {
				rainbowstack = 0;
			} else {
				rainbowstack++;
			}
			rainbow = rainbows.get(rainbowstack);
			
			if (add && y >= 2.0) add = false;
			else if (!add && y <= 0) add = true;

			y = add ? y + 0.1 : y - 0.1;
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, rainbow);
			}
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, 2.0 - y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, rainbow);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer rightclickchecker = new AbilityTimer() {
		
		@Override
		public void onStart() {
			heatGain(dashstartheat);
			if (evade.isRunning()) heatGain(Math.min(dashstartheat, heat / 10));
			evade.start();
		}
		
		@Override
		public void run(int count) {
			if (System.currentTimeMillis() - last >= 220) this.stop(false);
			else heatGain(dashrunningheat);
			getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.2));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer heatupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c히트 §e" + df.format(heat) + "§c%", BarColor.PINK, BarStyle.SOLID);
			bossBar.setProgress(Math.min(1, (heat - ((int) heat / 100) * 100) * 0.01));
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override 
		public void run(int count) {    		
    		if (!rightclickchecker.isRunning()) heatLose(0.5);
			
			if (accelerator.isRunning()) {
				bossBar.setColor(BarColor.BLUE);
				bossBar.setTitle("§b액셀러레이터");
				bossBar.setProgress(accelerator.getCount() / (double) duration);
			} else {
				if (heat <= 100) bossBar.setColor(BarColor.PINK);
				else if (heat <= 250) bossBar.setColor(BarColor.RED);
				else bossBar.setColor(BarColor.PURPLE);
				bossBar.setTitle("§c히트 §e" + df.format(heat) + "§c%");
				bossBar.setProgress(Math.min(1, (heat - ((int) heat / 100) * 100) * 0.01));
			}
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer accelerator = new AbilityTimer(duration) {
		
    	@Override
    	public void onStart() {
    		ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 35, 1);
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 1.3f);
    	}
    	
    	@Override
    	public void run(int count) {
    		heat = 0;
    		longHeatLose(0.04);
    	}
    	
    	@Override
    	public void onEnd() {
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public void heatGain(double value) {
		heat = Math.min(300, heat + value);
		if (heat >= 250) longHeatGain(value * 0.1);
	}
	
	public void heatLose(double value) {
		heat = Math.max(0, heat - value);		
	}
	
	public void longHeatGain(double value) {
		longHeat = longHeat + value;
		ac.update("§6잔열§f: " + df2.format(longHeat));
		Healths.setHealth(getPlayer(), Math.max(0, getPlayer().getHealth() - value));
	}
	
	public void longHeatLose(double value) {
		longHeat = Math.max(0, longHeat - value);
		ac.update("§6잔열§f: " + df2.format(longHeat));
		Healths.setHealth(getPlayer(), getPlayer().getHealth() + value);
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() + e.getAmount() > getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - longHeat) {
			e.setAmount(Math.max(0, getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - longHeat - getPlayer().getHealth()));
		}
	}
	
	@SubscribeEvent
	public void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getHealth() > getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - longHeat) {
			e.setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - longHeat);
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && e.getPlayer().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
			last = System.currentTimeMillis();
			if (!rightclickchecker.isRunning()) {
				rightclickchecker.start();
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.3));
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEntityEvent e) {
		if (e.getPlayer().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
			last = System.currentTimeMillis();
			if (!rightclickchecker.isRunning()) {
				rightclickchecker.start();
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.3));
			}
		}
	}
	
	@SubscribeEvent(priority = 1000)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (evade.isRunning()) {
				longHeatLose(e.getFinalDamage() * (heat * 0.015));
				if (accelgauge < 1) {
					accelgauge = accelgauge + (e.getFinalDamage() * 0.15);
					if (accelgauge >= 1) {
						SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));	
						getPlayer().sendMessage("§c[§b!§c] §f액셀러레이터를 사용할 수 있습니다.");
					}
				}
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
				e.setCancelled(true);
			} else if (heat > 100) longHeatGain(e.getFinalDamage() * (heat * 0.003));
		}
	}
	
	@SubscribeEvent
	public void onAccelDamage(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && accelerator.isRunning()) {
			e.setDamage(e.getDamage() + random.nextInt(6) + 1);
		}
	}
	
    @SubscribeEvent
    private void onEntityDamage(EntityDamageEvent e) {
        if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
            e.setCancelled(true);
            getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
            SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
        }
    }
	
}