package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableSet;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "집행관", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §3심판§f: 다른 생명체에게 해를 가한 대상은 $[STACK_DURATION]초간 §3심판 표식§f이 부여됩니다.",
        " §3심판 표식§f은 갱신될 수 있고, 폭발 시 $[UNIT_COOLDOWN]초간 부여되지 않습니다.",
        "§7검 들고 F §8- §b조율§f: §a유예 §7↔ §c집행§f 모드를 변경할 수 있습니다.",
        " 변경 후 다음 근접 공격은 심판 표식을 터뜨려 효과를 발동시킬 수 있습니다.",
        "§2[§a유예§2]§f", 
        "§8<§7패시브§8>§f 매 초당 $[EXTRA_HEAL_AMOUNT]의 체력을 §d회복§f합니다.",
        "§8<§7표식 폭발§8>§f 표식 수치만큼 대상은 공격력이 §b감소§f합니다. $[DECREASE_DURATION]초에 걸쳐 사라집니다.",
        "§4[§c집행§4]§f", 
        "§8<§7패시브§8>§f 적에게 가하는 피해가 §c$[INCREASE]%§f 증가합니다.",
        "§8<§7표식 폭발§8>§f 표식 수치만큼 §c추가 피해§f를 입히고 체력이 $[EXECUTE_HEALTH]% 이하면 §4처형§f합니다."
        },
		summarize = {
		"다른 생명체에게 해를 가한 대상은 §3심판 표식§f이 부여됩니다.",
		"§7검 들고 F키§f로 §a유예 §7↔ §c집행§f 모드를 변경할 수 있습니다.",
        "변경 후 다음 근접 공격은 심판 표식을 터뜨려 효과를 발동시킬 수 있습니다.",
        "§2[§a유예§2]§f", 
        "§8<§7패시브§8>§f 매 초당 $[EXTRA_HEAL]의 체력을 §d회복§f합니다.",
        "§8<§7표식 폭발§8>§f 표식 수치만큼 대상은 공격력이 §b감소§f합니다. 이는 $[DECREASE_DURATION]초에 걸쳐 점점 사라집니다.",
        "§4[§c집행§4]§f", 
        "§8<§7패시브§8>§f 적에게 가하는 피해가 §c$[INCREASE]%§f 증가합니다.",
        "§8<§7표식 폭발§8>§f 표식 수치만큼 §c추가 피해§f를 입히고 체력이 $[EXECUTE_HEALTH]% 이하면 §4처형§f합니다."
		})
public class Executioner extends AbilityBase {
	
	public Executioner(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> STACK_DURATION = 
			abilitySettings.new SettingObject<Double>(Executioner.class, "stack-duration", 3.5,
            "# 심판 표식 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> UNIT_COOLDOWN = 
			abilitySettings.new SettingObject<Double>(Executioner.class, "unit-cooldown", 20.0,
            "# 유닛별 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> EXTRA_HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Double>(Executioner.class, "extra-heal", 0.3,
            "# [유예] 초당 체력 회복량") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> DECREASE_DURATION = 
			abilitySettings.new SettingObject<Double>(Executioner.class, "decrease-duration", 10.0,
            "# [유예] 적 공격력 감소 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "damage-increase", 15,
            "# [집행] 공격력 증가량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> EXECUTE_HEALTH = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "execute-health", 10,
            "# [집행] 처형 조건 체력", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };

	private static final Set<Material> swords;
    private final int duration = (int) (STACK_DURATION.getValue() * 20);
    private final int unitCooldown = (int) (UNIT_COOLDOWN.getValue() * 1000 * Wreck.calculateDecreasedAmount(25));
    private final double healamount = EXTRA_HEAL_AMOUNT.getValue();
    private final int decreaseduration = (int) (DECREASE_DURATION.getValue() * 20);
    private final double attackincrease = 1 + (INCREASE.getValue() * 0.01);
    private final double executeable = EXECUTE_HEALTH.getValue() * 0.01;
	private final Crescent crescent = Crescent.of(1, 20);
	private ActionbarChannel ac = newActionbarChannel();
	private static final RGB red = RGB.of(188, 36, 36);
	private static final RGB green = RGB.of(128, 254, 128);
    
	private Map<Player, AttackStack> damageMap = new HashMap<>();
	private final Map<UUID, Long> lastExplode = new HashMap<>();
	private Set<Player> executed = new HashSet<>();
	
	private boolean execute = false;
	private boolean changed = false;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(execute ? "§8[§7집행§8]" : "§8[§7유예§8]");
			passive.start();
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!execute) {
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
				}	
			}
		}

	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		e.setCancelled(true);
    		execute = !execute;
    		ac.update(execute ? "§4[§c집행§4]" : "§2[§a유예§2]");
    		ParticleLib.PORTAL.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 10, 1);
    		SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 2);
    		changed = true;
    	}
    }
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (execute && getPlayer().equals(damager)) e.setDamage(e.getDamage() * attackincrease);
		
		if (damager != null && !getPlayer().equals(damager)) {
			if (System.currentTimeMillis() - lastExplode.getOrDefault(damager.getUniqueId(), 0L) >= unitCooldown) {
				if (!damageMap.containsKey(damager)) {
					new AttackStack(damager, duration, e.getFinalDamage()).start();
				}
			}	
		}
		
		if (damageMap.containsKey(e.getEntity()) && e.getDamager().equals(getPlayer()) && changed) {
			Player player = (Player) e.getEntity();
			changed = false;
			new CutParticle(90, execute ? red : green).start();
			ac.update(execute ? "§8[§7집행§8]" : "§8[§7유예§8]");
			if (!execute) {
				new DamageDecrease(player, damageMap.get(e.getEntity()).damage, decreaseduration).start();
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(0, Tone.C));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(1, Tone.F));
			} else {
				e.setDamage(e.getDamage() + damageMap.get(e.getEntity()).damage);
				double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				if (player.getHealth() <= maxHP * executeable) {
					player.setHealth(0);
					executed.add(player);
				}
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Tone.F));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			}
			damageMap.get(e.getEntity()).stop(false);
			lastExplode.put(player.getUniqueId(), System.currentTimeMillis());
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (executed.contains(e.getEntity())) {
			e.setDeathMessage(e.getEntity().getName() + "님이 §c집행관 §e" + getPlayer().getName() + "§f님에게 §4처형§f당했습니다.");
			executed.remove(e.getEntity());
		}
	}
	
	private class DamageDecrease extends AbilityTimer implements Listener {
		
		private final Player player;
		private final double maxDamage;
		private double damage = 0;
		private BossBar bossBar = null;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private DamageDecrease(Player player, double maxDamage, int duration) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.maxDamage = maxDamage;
			this.damage = maxDamage;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    		bossBar = Bukkit.createBossBar("§c공격력 감소§7: §e" + df.format(damage), BarColor.WHITE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(player);
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}
		
		@Override
		protected void run(int count) {
			damage = Math.max(0, damage - (double) maxDamage / duration);
    		if (damage == 0) this.stop(false);
    		else bossBar.setProgress(RangesKt.coerceIn((double) damage / maxDamage, 0, 1));
    		bossBar.setTitle("§c공격력 감소§7: §e" + df.format(damage));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (player.equals(damager) && !e.getEntity().equals(player)) {
				e.setDamage(e.getDamage() - damage);
			}
		}
		
	}
	
	private class AttackStack extends AbilityTimer implements Listener {
		
		private final Player player;
		private final IHologram hologram;
		private boolean hid = false;
		private double damage = 0;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private AttackStack(Player damager, int duration, double damage) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), "§4§l" + df.format(damage));
			hologram.display(getPlayer());
			this.player = damager;
			Executioner.this.damageMap.put(damager, this);
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int count) {
			if (NMS.isInvisible(player)) {
				if (!hid) {
					this.hid = true;
					hologram.hide(getPlayer());
				}
			} else {
				if (hid) {
					this.hid = false;
					hologram.display(getPlayer());
				}
				hologram.teleport(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), player.getLocation().getYaw(), 0);
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (player.equals(damager) && !e.getEntity().equals(damager)) {
				this.setCount(duration);
				damage += e.getFinalDamage();
				hologram.setText("§4§l" + df.format(damage));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			hologram.unregister();
			Executioner.this.damageMap.remove(player);
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;
		private final RGB color;
		
		private CutParticle(double angle, RGB color) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
			this.color = color;
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color);
			}
		}

	}

}
