package RainStarAbility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.util.Vector;

import RainStarEffect.Irreparable;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.LimitedPushingList;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "집행관", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §c형벌§f: 집행권이 없을 때 집행권을 획득하고, 있다면 소모합니다.",
		" §2[§a기본§2]§f 주변 $[RANGE_CONFIG]칸 내 대상에게 $[ACTIVE_SMALL_DAMAGE]의 피해를 입히고, 입힌 피해의 일부만큼 회복합니다.",
		" §4[§c집행§4]§f 가장 가까운 대상에게 $[ACTIVE_BIG_DAMAGE]의 피해를 입히고 $[IRREPARABLE_DURATION]초간 회복 불능 상태로 만듭니다.",
		" $[COOLDOWN_CONFIG]",
		"§7근접 공격 §8- §c심판§f: 매 $[CLOSE_ATTACK_COUNTER]번째 공격은 대상이 나에게 준 총 피해량에 비례합니다.",
		" §2[§a기본§2]§f 발동할 때 피해를 절반만 입히는 대신 내 방어력을 $[ARMOR_INCREASE] 상승시킵니다.",
		" §4[§c집행§4]§f 발동할 때 내 방어력에 비례한 피해를 입히고, 내 방어력을 $[ARMOR_DECREASE] 감소시킵니다.",
		"§7원거리 공격 §8- §c결단§f: 같은 대상을 2회 연속으로 맞힐 때 효과가 발동합니다.",
		" §2[§a기본§2]§f 피해를 출혈 피해로 입히고, 내 체력을 대미지의 $[HEAL_AMOUNT]% 만큼 회복합니다.",
		" §4[§c집행§4]§f 체력을 $[HEALTH_CONSUME_AMOUNT] 소모하여 내 남은 체력에 반비례하는 추가 피해를 입힙니다.",
		"§7패시브 §8- §4사형 선고§f: 대상이 내 최대 체력을 상회하는 피해 총량을 주었을 때",
		" 대상의 체력을 33% 이하로 만들 경우 $[PASSIVE_DURATION]초간 발동합니다.",
		" §2[§a기본§2]§f 잃은 체력만큼의 흡수 체력을 획득하고, 근접 피해량이 $[DAMAGE_DECREASE_AMOUNT]% 감소됩니다.",
		" §4[§c집행§4]§f 대상에게 영구 지속하는 출혈 피해를 주며, 근접 피해량이 $[DAMAGE_INCREASE_AMOUNT]% 증가합니다."
		},
		summarize = {
		"§7철괴 우클릭 시§f 집행권을 획득 혹은 소모해 주변 적에게 피해를 입힙니다.",
		" $[COOLDOWN_CONFIG]",
		"집행권 유무에 따라 근접, 원거리 공격이 각각의 방식대로 강화됩니다.",
		"§2[§a기본§2]§f 공격력이 낮아지지만, 방어력과 생존력이 증가합니다.",
		"§4[§c집행§4]§f 방어력을 낮추고 체력을 소모하는 대신 공격력이 증가합니다.",
		})

public class Executioner extends AbilityBase implements ActiveHandler {
	
	public Executioner(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "cooldown", 30,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
	public static final SettingObject<Integer> RANGE_CONFIG = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "range", 7,
            "# 철괴 우클릭 사거리", "# 단위: 칸") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ACTIVE_SMALL_DAMAGE = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "active-small-damage", 4,
            "# 철괴 우클릭 [기본] 소 대미지") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ACTIVE_BIG_DAMAGE = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "active-big-damage", 9,
            "# 철괴 우클릭 [집행] 대 대미지") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> IRREPARABLE_DURATION = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "irreparable-duration", 15,
            "# 회복 불능 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> CLOSE_ATTACK_COUNTER = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "close-attack-counter", 3,
            "# 타격 카운터", "# 몇 회 근접 공격마다 발동시킬 건지 결정합니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ARMOR_DURATION = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "armor-duration", 3,
            "# 근접 공격 방어력 증감 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ARMOR_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "armor-increase", 3,
            "# 근접 공격 [기본] 방어력 상승 수치", "# 단위: 반 칸") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ARMOR_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "armor-decrease", 3,
            "# 근접 공격 [집행] 방어력 감소 수치", "# 단위: 반 칸") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "heal-amount", 60,
            "# 원거리 공격 [기본] 회복 수치", "# 단위: 준 대미지의 %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> HEALTH_CONSUME_AMOUNT = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "health-consume-amount", 3,
            "# 원거리 공격 [집행] 체력 소모 수치", "# 단위: 반 칸") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> PASSIVE_DURATION = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "passive-duration", 5,
            "# 패시브 공격력 증감 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DAMAGE_DECREASE_AMOUNT = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "damage-decrease-amount", 40,
            "# 패시브 [기본] 공격력 감소 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DAMAGE_INCREASE_AMOUNT = 
			abilitySettings.new SettingObject<Integer>(Executioner.class, "damage-increase-amount", 160,
            "# 패시브 [집행] 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private boolean execute = false;
	private int count = 0;
	private Map<Player, Double> damagecounter = new HashMap<>();
	private Set<Player> noPassive = new HashSet<>();
	private Set<Participant> bleeds = new HashSet<>();
	private AttributeModifier armorincrease = new AttributeModifier(UUID.randomUUID(), "armor_increase", ARMOR_INCREASE.getValue(), Operation.ADD_NUMBER);
	private AttributeModifier armordecrease = new AttributeModifier(UUID.randomUUID(), "armor_decrease", -ARMOR_DECREASE.getValue(), Operation.ADD_NUMBER);
	private int range = RANGE_CONFIG.getValue();
	private static final Circle circle1 = Circle.of(0.7, 15);
	private static final RGB color = RGB.of(188, 36, 36);
	private final Cooldown cool = new Cooldown(COOLDOWN_CONFIG.getValue(), "형벌", CooldownDecrease._50);
	private final ActionbarChannel status = newActionbarChannel();
	private final ActionbarChannel ac = newActionbarChannel();
	private LimitedPushingList<Player> hitplayer = new LimitedPushingList<>(1);
	private ArmorStand hologram;
	private Location defaultLocation;
	private final Crescent crescent = Crescent.of(1, 20);
	
	private static final Points LAYER = Points.of(0.175, new boolean[][]{
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
		{true, true, true, true, true, true},
		{true, true, true, true, true, true},
		{false, false, true, true, false, false},
		{false, false, true, true, false, false},
	});
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	bleeding.start();
	    	defaultLocation = getPlayer().getLocation().clone().add(0, 100, 0);
			this.hologram = getPlayer().getWorld().spawn(defaultLocation, ArmorStand.class);
			hologram.setVisible(false);
			hologram.setGravity(false);
			hologram.setInvulnerable(true);
			NMS.removeBoundingBox(hologram);
			hologram.setCustomNameVisible(true);
			hologram.setCustomName("§4§l※");
			if (!execute) {
				status.update("§2§l[§a§l기본§2§l]");
			} else {
				status.update("§4§l[§c§l집행§4§l]");
			}
	    }
	    if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
	    	if (hologram != null) {
		    	hologram.remove();	
	    	}
	    	status.update(null);
	    	ac.update(null);
	    }
	}
	
    private final AbilityTimer damageincreasing = new AbilityTimer(PASSIVE_DURATION.getValue() * 20) {
    	
    	@Override
    	public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer damagedecreasing = new AbilityTimer(PASSIVE_DURATION.getValue() * 20) {
    	
    	@Override
    	public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer armorAdding = new AbilityTimer(ARMOR_DURATION.getValue() * 20) {
    	
    	@Override
		public void onStart() {
    		getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(armorincrease);
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(armorincrease);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer armorRemoving = new AbilityTimer(ARMOR_DURATION.getValue() * 20) {
    	
    	@Override
		public void onStart() {
    		getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(armordecrease);
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(armordecrease);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer bleeding = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		for (Participant p : getGame().getParticipants()) {
    			if (bleeds.contains(p) && !p.hasEffect(Bleed.registration)) {
    				Bleed.apply(p, TimeUnit.SECONDS, 3, 15);
    			}
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer hologramteleporting = new AbilityTimer() {
    	
    	@Override
		public void onStart() {
			hologram.teleport(hitplayer.getFirst().getLocation().clone().add(0, 2.2, 0));
    	}
    	
    	@Override
    	public void run(int count) {
			hologram.teleport(hitplayer.getFirst().getLocation().clone().add(0, 2.2, 0));
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		hologram.teleport(defaultLocation);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	private final AbilityTimer circle = new AbilityTimer(10) {
		
		private VectorIterator iterator;
		
    	@Override
		public void onStart() {
    		this.iterator = Circle.infiniteIteratorOf(RANGE_CONFIG.getValue(), Math.min(RANGE_CONFIG.getValue() * 20, 250));
    	}
		
    	@Override
		public void run(int i) {
			for (int j = 0; j < 10; j++) {
				Location loc = getPlayer().getLocation().clone().add(iterator.next());
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()) + 0.1);
				ParticleLib.REDSTONE.spawnParticle(loc, color);;
			}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (bleeds.contains(getGame().getParticipant(e.getEntity()))) {
			bleeds.remove(getGame().getParticipant(e.getEntity()));
		}
	}
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			Player player = (Player) e.getDamager();
		    double damage = (damagecounter.getOrDefault(player, 0d)).doubleValue();
		    damagecounter.put(player, damage + e.getFinalDamage());
		}
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) {
			    double damage = (damagecounter.getOrDefault((Player) projectile.getShooter(), 0d)).doubleValue();
			    damagecounter.put((Player) projectile.getShooter(), damage + e.getFinalDamage());
			}
		}
		
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			count++;
			if (count % CLOSE_ATTACK_COUNTER.getValue() == 0) {
				if (!execute) {
					if (damagecounter.containsKey(e.getEntity())) e.setDamage((e.getDamage() + (Math.min(4, (damagecounter.get(e.getEntity()) * 0.2)))) * 0.5);
					else e.setDamage(e.getDamage() * 0.5);
					if (armorAdding.isRunning()) {
						armorAdding.stop(false);
					}
					armorAdding.start();
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(0, Tone.C));
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(1, Tone.F));
					new AbilityTimer(7) {
						@Override
						protected void run(int count) {
							Location center = getPlayer().getLocation().clone().add(0, 1 - count * 0.15, 0);
							for (Location loc : circle1.toLocations(center)) {
								ParticleLib.VILLAGER_HAPPY.spawnParticle(loc);
							}
						}
					}.setPeriod(TimeUnit.TICKS, 1).start();
				} else {
					if (damagecounter.containsKey(e.getEntity())) e.setDamage(e.getDamage() + Math.min(4, (damagecounter.get(e.getEntity()) * 0.2)) + (getPlayer().getAttribute(Attribute.GENERIC_ARMOR).getValue() * 0.2));
					else e.setDamage(e.getDamage() + (getPlayer().getAttribute(Attribute.GENERIC_ARMOR).getValue() * 0.2));
					if (armorRemoving.isRunning()) {
						armorRemoving.stop(false);
					}
					armorRemoving.start();
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(1, Tone.A));
					new CutParticle(90).start();
					ParticleLib.DAMAGE_INDICATOR.spawnParticle(e.getEntity().getLocation(), 0, 0, 0, 20, 0.5);
				}
				count = 0;
			} else {
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			}
			if (damagecounter.containsKey(e.getEntity()) && !noPassive.contains(e.getEntity())) {
				Player p = (Player) e.getEntity();
				if (p.getHealth() <= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 3) {
	 				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					if (!execute) {
						if (maxHealth <= damagecounter.get(p)) {
				    		float yellowheart = NMS.getAbsorptionHearts(getPlayer());
							NMS.setAbsorptionHearts(getPlayer(), (float) (yellowheart + ((maxHealth - getPlayer().getHealth()) * 0.5)));
							if (damagedecreasing.isRunning()) damagedecreasing.setCount(PASSIVE_DURATION.getValue() * 20);
							else damagedecreasing.start();
							noPassive.add(p);
							getPlayer().sendMessage("§4[§c!§4] §e" + p.getName() + "§f님께 §4사형 선고§f를 내립니다.");
							getPlayer().sendMessage("§4[§c!§4] §c집행권§f이 없어 대신 §e추가 체력§f을 획득하고 피해량이 낮아집니다.");
						}
					} else {
						if (maxHealth <= damagecounter.get(p)) {
							bleeds.add(getGame().getParticipant(p));
							if (damageincreasing.isRunning()) damageincreasing.setCount(PASSIVE_DURATION.getValue() * 20);
							else damageincreasing.start();
							noPassive.add(p);
							getPlayer().sendMessage("§4[§c!§4] §e" + p.getName() + "§f님께 §4사형 선고§f를 내립니다.");
							p.getPlayer().sendMessage("§4[§c!§4] §e" + getPlayer().getName() + "§f님이 당신에게 §4사형 선고§f를 내렸습니다.");
							p.getPlayer().sendMessage("§4[§c!§4] §f죽기 전까지 풀 수 없는 §c출혈 §f상태이상이 걸립니다.");
							SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(p.getPlayer(), 0.5f, 0.7f);
							new AbilityTimer(60) {
						    	@Override
								public void run(int count) {
						    		final Location headLocation = p.getPlayer().getEyeLocation().clone().add(0, 1.5, 0);
						    		final Location baseLocation = headLocation.clone().subtract(0, 1.4, 0);
									final float yaw = p.getPlayer().getLocation().getYaw();
									for (Location loc : LAYER.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
										ParticleLib.REDSTONE.spawnParticle(loc, color);
									}
									LAYER.rotateAroundAxisY(yaw);
						    	}
							}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).start();
						}
					}
				}
			}
			if (count != 0) ac.update(Strings.repeat("§4✗", count));
			else ac.update(null);
		}
		
		if (NMS.isArrow(e.getDamager()) && !e.getEntity().equals(getPlayer()) && e.getEntity() instanceof Player) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				if (hitplayer.contains(e.getEntity())) {
					hologramteleporting.stop(false);
					hologram.teleport(defaultLocation);
					if (!execute) {
						final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (e.getFinalDamage() * (HEAL_AMOUNT.getValue() * 0.01)), RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							Healths.setHealth(getPlayer(), getPlayer().getHealth() + (e.getFinalDamage() * (HEAL_AMOUNT.getValue() * 0.01)));	
						}
						Bleed.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.SECONDS, (int) (e.getFinalDamage() * 1.2), 10);
						e.setDamage(0);
					} else {
						Healths.setHealth(getPlayer(), getPlayer().getHealth() - (HEALTH_CONSUME_AMOUNT.getValue()));
						double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						double health = getPlayer().getHealth();
						e.setDamage(e.getDamage() + ((maxHealth - health) / maxHealth) * 5);
					}
					hitplayer.clear();
				} else {
					hitplayer.add((Player) e.getEntity());
					hologramteleporting.start();
				}
				Player p = (Player) e.getEntity();
				if (p.getHealth() <= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 3) {
	 				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	 				if (damagecounter.containsKey(e.getEntity()) && !noPassive.contains(e.getEntity())) {
						if (!execute) {
							if (maxHealth <= damagecounter.get(p)) {
					    		float yellowheart = NMS.getAbsorptionHearts(getPlayer());
								NMS.setAbsorptionHearts(getPlayer(), (float) (yellowheart + (maxHealth - getPlayer().getHealth())));
								if (damagedecreasing.isRunning()) damagedecreasing.setCount(PASSIVE_DURATION.getValue() * 20);
								else damagedecreasing.start();
								noPassive.add(p);
								getPlayer().sendMessage("§4[§c!§4] §e" + p.getName() + "§f님께 §4사형 선고§f를 내립니다.");
								getPlayer().sendMessage("§4[§c!§4] §c집행권§f이 없어 대신 §e추가 체력§f을 획득하고 피해량이 낮아집니다.");
							}
						} else {
							if (maxHealth <= damagecounter.get(p)) {
								bleeds.add(getGame().getParticipant(p));
								if (damageincreasing.isRunning()) damageincreasing.setCount(PASSIVE_DURATION.getValue() * 20);
								else damageincreasing.start();
								noPassive.add(p);
								getPlayer().sendMessage("§4[§c!§4] §e" + p.getName() + "§f님께 §4사형 선고§f를 내립니다.");
								p.getPlayer().sendMessage("§4[§c!§4] §e" + getPlayer().getName() + "§f님이 당신에게 §4사형 선고§f를 내렸습니다.");
								p.getPlayer().sendMessage("§4[§c!§4] §f죽기 전까지 풀 수 없는 §c출혈 §f상태이상이 걸립니다.");
								SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(p.getPlayer(), 0.5f, 0.7f);
								new AbilityTimer(60) {
							    	@Override
									public void run(int count) {
							    		final Location headLocation = p.getPlayer().getEyeLocation().clone().add(0, 1.5, 0);
							    		final Location baseLocation = headLocation.clone().subtract(0, 1.4, 0);
										final float yaw = p.getPlayer().getLocation().getYaw();
										for (Location loc : LAYER.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
											ParticleLib.REDSTONE.spawnParticle(loc, color);
										}
										LAYER.rotateAroundAxisY(yaw);
							    	}
								}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).start();
							}
						}	
	 				}
				}
			}
		}
		
		if (e.getDamager().equals(getPlayer()) && damageincreasing.isRunning()) {
			e.setDamage(e.getDamage() * (DAMAGE_INCREASE_AMOUNT.getValue() * 0.01));
		}
		if (e.getDamager().equals(getPlayer()) && damagedecreasing.isRunning()) {
			e.setDamage(e.getDamage() - (e.getDamage() * (DAMAGE_DECREASE_AMOUNT.getValue() * 0.01)));
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
			if (!execute) {
				double damage = 0.4;
				float counting = 0.1f;
		   		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer(), 1, 0.9f);
				for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
					Damages.damageMagic(p, getPlayer(), false, ACTIVE_SMALL_DAMAGE.getValue());
					damage += (ACTIVE_SMALL_DAMAGE.getValue() * 0.25);
					if (counting < 2) {
						counting += 0.1;
					}
					SoundLib.BLOCK_NOTE_BLOCK_XYLOPHONE.playSound(getPlayer().getLocation(), 1, counting);
				}
				if (damage != 0.4) {
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), damage, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Healths.setHealth(getPlayer(), getPlayer().getHealth() + damage);
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.2f);
						ParticleLib.VILLAGER_HAPPY.spawnParticle(getPlayer().getLocation(), 1, 1, 1, (int) Math.min(100, damage * 10), 0);
					}	
				}
				if (circle.isRunning()) circle.stop(false);
				circle.start();
				status.update("§4§l[§c§l집행§4§l]");
				execute = true;
			} else {
				Player p = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
				if (p != null) {
					Damages.damageMagic(p, getPlayer(), false, ACTIVE_BIG_DAMAGE.getValue());
					Irreparable.apply(getGame().getParticipant(p), TimeUnit.SECONDS, IRREPARABLE_DURATION.getValue());
					for (Location loc : Line.between(getPlayer().getLocation().add(0, 0.5, 0), p.getPlayer().getLocation().add(0, 0.5, 0), 120).toLocations(getPlayer().getLocation().add(0, 0.5, 0))) {
			   			ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0, 0, 0, 1, 0);
			   		}
					SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer().getLocation(), 1, 1.2f);
				}
				status.update("§2§l[§a§l기본§2§l]");
				execute = false;
			}
			return cool.start();
		}
		return false;
	}

	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
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
