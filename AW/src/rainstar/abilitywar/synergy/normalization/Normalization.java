package rainstar.abilitywar.synergy.normalization;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.Cooldown.CooldownTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import kotlin.ranges.RangesKt;
import rainstar.abilitywar.RainStarAddon;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBerserker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBlackCandle;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBombArrow;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBrewer;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientChaos;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientHacker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientSniper;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientTheHighPriestess;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientVoid;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientYeti;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientZombie;

@AbilityManifest(name = "바로 어빌워 정상화", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
		"§7패시브 §8- §b신새벽§f: §b새벽§f 능력의 모든 효과가 적용됩니다.",
		" 자세한 내용은 §7/aw abilities 새벽§f을 통해 참고해주세요.",
		"§7패시브 §8- §3WWE§f: 치명적인 피해를 입을 때, §e여명§f이 $[REBIRTH_CONSUME] 이상이라면",
		" §e여명§f을 전부 소모하고 §e여명§f $[COUNT_HEAL]당 1의 HP를 §d회복§f하고 §a부활§f합니다.",
		"§7패시브 §8- §c애드온 척결§f: §7애드온§f 능력을 소유중인 대상은, §b검기§f로 입는 피해가",
		" §b§l50%§f 증가합니다§8(§7믹스 능력자에서는 능력당 25%§8)§f.",
		"§7패시브 §8- §a정상화§f: $[RESET]초마다 무작위 §5고대 능력§f을 하나 부여받습니다.",
		" 부여받은 능력이 남아있다면 자동으로 버립니다. 정상화로 부여받은 능력의 모든",
		" 쿨타임이 빠르게 종료됩니다.",
		"§7아이템 버리기 §f- §4OUT!!§f: 정상화로 부여받은 능력을 버리고 정상화 대기",
        " 시간을 단축합니다. §3/§f 이후 주위 $[RANGE]칸 이내의 모든 플레이어를 $[ROOTED]초간 §2§n속박§f하고,",
        " 체력을 §d$[HEAL]hp§f + 주위의 플레이어 당 §d$[ADD_HEAL]hp§f만큼 §a회복§f합니다.",
        "§5---------------------------------",
        "$(explain)",
        "§5---------------------------------"
		})
public class Normalization extends Synergy implements ActiveHandler, TargetHandler {
	
	public Normalization(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> SPEED_INCREASE = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "speed-increase", 20, "# 이동 속도 증가량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> BLESS_DURATION = rsSynergySettings.new SettingObject<Double>(
			Normalization.class, "bless-duration", 5.0, "# 가호 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> BLESS_CONSUME = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "bless-consume", 20, "# 가호 재활성에 필요한 여명 소모량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> STACK_COUNT = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "stack-count", 10, "# 하늘 찢기 충전에 필요한 여명") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CUT_COOLDOWN = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "cut-cooldown", 20, "# 하늘 가르기 내부 쿨타임", "# 단위: 틱 (20틱 = 1초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> REBIRTH_CONSUME = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "rebirth-dawn-consume", 100, "# WWE 발동 기준치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COUNT_HEAL = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "dawn-count-heal", 10, "# 여명 수치당 체력 1 회복") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> RESET = rsSynergySettings.new SettingObject<Integer>(
			Normalization.class, "reset", 50, "# 정상화 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = rsSynergySettings.new SettingObject<Double>(
			Normalization.class, "range-amount", 10.0, "# OUT!! 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> ROOTED = rsSynergySettings.new SettingObject<Double>(
			Normalization.class, "rooted-amount", 2.5, "# OUT!! 속박 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> HEAL = rsSynergySettings.new SettingObject<Double>(Normalization.class, 
			"heal-amount", 4.0, "# 디버깅 회복력") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> ADD_HEAL = rsSynergySettings.new SettingObject<Double>(Normalization.class, 
			"add-heal-amount", 1.0, "# 디버깅 인당 추가 회복력") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private int blessduration = (int) (BLESS_DURATION.getValue() * 20);
	private int stackcount = STACK_COUNT.getValue(), blessconsume = BLESS_CONSUME.getValue();
	private int cutcool = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(40) * CUT_COOLDOWN.getValue() : CUT_COOLDOWN.getValue());
	private boolean blessing = true;
	private AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", SPEED_INCREASE.getValue() * 0.01, Operation.ADD_SCALAR);
	private final int rebirthConsume = REBIRTH_CONSUME.getValue(), countHeal = COUNT_HEAL.getValue();
    private final double range = RANGE.getValue();
    private final int rooted = (int) (ROOTED.getValue() * 20);
    private final double heal = HEAL.getValue();
    private final double addheal = ADD_HEAL.getValue();
    
    private final Random random = new Random();
    private final Circle rootCircle = Circle.of(1, 15), circle2 = Circle.of(range, (int) (range * 10));
    private AbilityBase ability;
    private final AbilityChanger abilityChanger = new AbilityChanger();
	
	private int dawn = 0, stack = 0, charge = 0;
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel(), ac4 = newActionbarChannel();
	private static final Set<Material> swords;
	private boolean state = false;
	private DecimalFormat df = new DecimalFormat("0.0");
	private boolean modifieradded = false;
	private static final int particleCount = 40;
	private static final List<RGB> gradation = Gradient.createGradient(100, RGB.of(136, 68, 211), RGB.of(79, 129, 159), RGB.of(37, 174, 120));
	private static final Circle circle = Circle.of(0.5, particleCount);
	private static final double yDiff = 0.6 / particleCount;
	private int colorstack = 0;
	private boolean colordirection = false;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}

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
	};
	
	private static final Component[] components = new Component[]{
			new Component(RGB.of(3, 102, 252), 2.8, 1.35),
			new Component(RGB.of(3, 102, 252), 2.8, 1.3),
			new Component(RGB.of(80, 118, 242), 2.8, 1.25),
			new Component(RGB.of(80, 118, 242), 2.8, 1.2),
			new Component(RGB.of(92, 183, 242), 2.8, 1.15),
			new Component(RGB.of(92, 183, 242), 2.8, 1.1),
			new Component(RGB.WHITE, 2.8, 1.05),
			new Component(RGB.WHITE, 2.8, 1)
	};
	
    @SuppressWarnings("unused")
    private final Object explain = new Object() {
        @Override
        public String toString() {
            if (hasAbility()) {
                final StringJoiner joiner = new StringJoiner("\n");
                joiner.add("§b" + ability.getName() + " §f[" + (ability.isRestricted() ? "§7능력 비활성화됨" : "§a능력 활성화됨") + "§f] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
                for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext(); ) {
                    joiner.add(ChatColor.RESET + iterator.next());
                }
                return joiner.toString();
            } else return "능력이 없습니다.";
        }
    };
	
	
	@Override
	public void onUpdate(Update update) {
		if (update.equals(Update.RESTRICTION_CLEAR)) {
			ac1.update("§e여명§7: §b" + dawn);
			ac2.update(Strings.repeat("§9✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
			ac3.update((blessing ? "§3" : "§7") + "새벽의 가호");
			passive.start();
			showHelix();
		}
		
        if (update == Update.RESTRICTION_CLEAR) {
            if (ability != null) {
                ability.setRestricted(false);
            }
            abilityChanger.start();
        } else if (update == Update.RESTRICTION_SET) {
            if (ability != null) {
                ability.setRestricted(true);
            }
        } else if (update == Update.ABILITY_DESTROY) {
            if (ability != null) {
                ability.destroy();
                this.ability = null;
                abilityChanger.bossBar.removeAll();
            }
        }
	}
	
	public void stackAdd() {
		dawn++;
		stack++;
		ac1.update("§e여명§7: §b" + dawn);
		if (stack >= stackcount) {
			stack -= stackcount;
			charge = Math.min(4, charge + 1);
			ac2.update(Strings.repeat("§b✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), dawn * 0.0002, RegainReason.REGEN);
    		if (!event.isCancelled()) {
    			Healths.setHealth(getPlayer(), Math.min(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), getPlayer().getHealth() + event.getAmount()));
    		}
    		if (!inv.isRunning() && blessing == false && dawn >= blessconsume) {
    			dawn -= blessconsume;
    			blessing = true;
    			ac1.update("§e여명§7: §b" + dawn);
    		}
    		ac3.update((blessing ? "§3" : "§7") + "새벽의 가호");
    		
    		if (blessing && !modifieradded) {
    			modifieradded = true;
    			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
    		}
    		
    		if (!blessing && modifieradded) {
    			modifieradded = false;
    			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    	}
	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer inv = new AbilityTimer(blessduration) {
    	
		private double y;
		private boolean add;
		private int stack;
    	
    	@Override
    	public void onStart() {
    		getGame().getParticipant(getPlayer()).attributes().TARGETABLE.setValue(false);
    		ac4.update("§3무적§f: §e" + df.format(blessduration / 20.0) + "§f초");
			y = 0.0;
			add = true;
			stack = 0;
    	}
    	
    	@Override
    	public void run(int count) {
    		ac4.update("§3무적§f: §e" + df.format(count / 20.0) + "§f초");
			if (add && y >= 2.0) add = false;
			else if (!add && y <= 0) add = true;

			y = add ? y + 0.1 : y - 0.1;
			stack = stack < 100 ? stack + 1 : 0;
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradation.get(Math.max(0, stack - 1)));
			}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getGame().getParticipant(getPlayer()).attributes().TARGETABLE.setValue(true);
    		showHelix();
    		ac4.update(null);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getItem() != null && swords.contains(e.getItem().getType())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				cut();
			} else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (charge > 0) {
					new Dash(getPlayer().getLocation().clone().add(0, 1, 0), getPlayer().getLocation().getDirection(), RGB.AQUA, RGB.WHITE).start();
					charge--;
					ac2.update(Strings.repeat("§b✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
				}
			}
		}
	}

	@SubscribeEvent
	private void onDamageCut(final EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			if (swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
				cut();
			}
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(getPlayer())) {
				damager = getPlayer();
			}
		}
		if (!e.getEntity().equals(getPlayer()) && !(e.getEntity() instanceof ArmorStand) && (getPlayer().equals(e.getDamager()) || getPlayer().equals(damager))) {
			if (getPlayer().equals(e.getDamager())) e.setDamage(e.getDamage() * 0.66);
			e.setDamage(e.getDamage() * (1 + (dawn * 0.004)));
			stackAdd();
		}
		
		if (e.getEntity().equals(getPlayer()) && inv.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEffect(ParticipantPreEffectApplyEvent e) {
		if (e.getParticipant().equals(getParticipant())) {
			if (blessing) {
				inv.start();
				blessing = false;
				e.setCancelled(true);
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 1.5f);
				final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
				final FireworkMeta meta = firework.getFireworkMeta();
				Color color1 = RGB.of(67, 191, 243).getColor(), color2 = RGB.of(52, 101, 161).getColor(), color3 = RGB.of(54, 171, 234).getColor(), 
						color4 = RGB.of(254, 254, 254).getColor(), color5 = RGB.of(192, 235, 251).getColor(), color6 = RGB.of(56, 185, 248).getColor(),
						color7 = RGB.of(12, 139, 252).getColor(), color8 = RGB.of(254, 223, 134).getColor();
				meta.addEffect(
						FireworkEffect.builder()
						.withColor(color1, color2, color3, color4, color5, color6, color7, color8)
						.withTrail()
						.withFlicker()
						.with(Type.BURST)
						.build()
				);	
				meta.setPower(0);
				firework.setFireworkMeta(meta);
				firework.setMetadata("Firework", NULL_VALUE);
				new BukkitRunnable() {
					@Override
					public void run() {
						firework.detonate();
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);
			}
			if (inv.isRunning()) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByFirework(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = 1000)
	public void onDeathByEntity(EntityDamageByEntityEvent e) {
		onDeath(e);
	}
	
	@SubscribeEvent(priority = 1000)
	public void onDeathByBlock(EntityDamageByBlockEvent e) {
		onDeath(e);
	}
	
	@SubscribeEvent(priority = 1000)
	public void onDeath(EntityDamageEvent e) {
		if (!e.isCancelled() && e.getEntity().equals(getPlayer()) && dawn >= rebirthConsume) {
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				e.setCancelled(true);
				Healths.setHealth(getPlayer(), 1);
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), dawn / countHeal, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
				}
				ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 100, 1);
				SoundLib.ITEM_TOTEM_USE.playSound(getPlayer().getLocation(), 1, 1.25f);
				dawn = 0;
				ac1.update("§e여명§7: §b" + dawn);
			}
		}
	}
	
	private void showHelix() {
		new AbilityTimer((particleCount * 3) / 2) {
			int count = 0;

			@Override
			protected void run(int a) {
				for (int i = 0; i < 2; i++) {
					if (colorstack == 99 || colorstack == 0) colordirection = !colordirection;
					colorstack = colordirection ? Math.min(colorstack + 1, 99) : Math.max(colorstack - 1, 0);
					ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(circle.get(count % 40)).add(0, count * yDiff, 0), gradation.get(colorstack));
					count++;
				}
			}
			
			@Override
			protected void onEnd() {
				if (!inv.isRunning()) {
					showHelix();
				}
			}
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	private AbilityTimer cut_cooldown = new AbilityTimer(cutcool) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	private void cut() {
		if (!cut_cooldown.isRunning()) {
			cut_cooldown.start();
			new Cut(getPlayer().getLocation().clone().add(0, 1.5, 0), (state = !state) ? 25 : -25, null).start();
			SoundLib.ENTITY_WITHER_SHOOT.playSound(getPlayer().getLocation());
		}
	}
	
	private static class Component {

		private final RGB color;
		private final double radius, length;

		private Component(RGB color, double radius, double length) {
			this.color = color;
			this.radius = radius;
			this.length = length;
		}

	}

	public class Cut extends AbilityTimer {

		private final CenteredBoundingBox boundingBox = CenteredBoundingBox.of(new Vector(), -1, -1, -1, 1, 1, 1);
		private final Location base;
		private final double angle;
		private final int sharpness;
		private final Consumer<LivingEntity> onDamage;
		private final Set<LivingEntity> damaged = new HashSet<>();

		private Cut(Location base, double angle, Consumer<LivingEntity> onDamage) {
			super(angle < 0 ? TaskType.REVERSE : TaskType.NORMAL, 3);
			setPeriod(TimeUnit.TICKS, 1);
			this.base = base;
			this.angle = angle;
			this.sharpness = getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
			this.onDamage = onDamage;
		}

		@Override
		protected void run(int count) {
			final Vector direction = base.getDirection().setY(0).normalize(), axis = VectorUtil.rotateAroundAxisY(direction.clone(), 90);
			for (int i = 0; i < 10; i++) {
				final double radians = 0.10471975511965977461542144610932 * ((count - 1) * 10 + i);
				final double cos = FastMath.cos(radians), sin = FastMath.sin(radians);
				for (Component component : components) {
					final Location loc = base.clone().add(
							VectorUtil.rotateAroundAxis(
									VectorUtil.rotateAroundAxis(
											VectorUtil.rotateAroundAxisY(new Vector(cos, 0, component.length * sin), -base.getYaw()), direction, angle
									), axis, base.getPitch()
							).multiply(component.radius)
					);
					ParticleLib.REDSTONE.spawnParticle(loc, component.color);
					boundingBox.setCenter(loc);
					for (LivingEntity entity : LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer().getWorld(), boundingBox, predicate)) {
						if (!damaged.contains(entity)) {
							entity.setNoDamageTicks(0);
							float damage = (float) (EnchantLib.getDamageWithSharpnessEnchantment(getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue(), sharpness) * 0.66);
							if (entity instanceof Player && getGame().isParticipating((Player) entity) && getGame().getParticipant((Player) entity).hasAbility()) {
								Participant participant = getGame().getParticipant((Player) entity);
								AbilityBase ab = participant.getAbility();
								if (ab instanceof Mix) {
									Mix mix = (Mix) ab;
									AbilityBase first = mix.getFirst(), second = mix.getSecond();
									int stack = 0;
									if (first != null && first.getRegistration().getProvider() instanceof Addon) {
										stack += 25;
									}
									if (second != null && second.getRegistration().getProvider() instanceof Addon) {
										stack += 25;
									}
									damage *= (1 + (stack * 0.01));
								} else {
									if (ab.getRegistration().getProvider() instanceof Addon && ((Addon) ab.getRegistration().getProvider()).getClass().equals(RainStarAddon.class)) {
										damage *= 1.5;
									}
								}
							}
							Damages.damageMagic(entity, getPlayer(), false, damage);
							if (onDamage != null) onDamage.accept(entity);
							damaged.add(entity);
						}
					}
				}
			}
		}

	}

	private final double startRadius = 1.25, distance = 12.5;

	public class Dash extends AbilityTimer {

		private final Vector direction;
		private final double yaw;
		private final RGB color, circleColor;
		private Location lastLocation;
		private double radius = startRadius, radians = 0.0;

		private Dash(Location startLocation, Vector direction, RGB color, RGB circleColor) {
			super(5);
			setPeriod(TimeUnit.TICKS, 1);
			this.direction = direction.setY(0).normalize().multiply(distance / 5);
			this.yaw = LocationUtil.getYaw(direction);
			this.color = color;
			this.circleColor = circleColor;
			this.lastLocation = startLocation;
		}

		@Override
		protected void run(int count) {
			final Location newLocation = lastLocation.clone().add(direction);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				Location location = iterator.next();
				ParticleLib.REDSTONE.spawnParticle(location, color);
				radians += 0.15707963267948966192313216916398;
				radius -= (startRadius / (distance * 10));
				ParticleLib.REDSTONE.spawnParticle(location.clone().add(
						VectorUtil.rotateAroundAxisY(new Vector(FastMath.cos(radians), FastMath.sin(radians), 0), -yaw).multiply(radius)
				), circleColor);
			}
			lastLocation = newLocation;
			getPlayer().setVelocity(direction);
			if (count % 2 != 0) {
				final Location base = newLocation.clone();
				base.setDirection(direction);
				new Cut(base, (state = !state) ? 25 : -25, null).start();
				SoundLib.ENTITY_WITHER_SHOOT.playSound(base);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			getPlayer().setVelocity(VectorUtil.ZERO);
		}
	}
	
	//개발자
	
    public boolean hasAbility() {
        return ability != null;
    }
	
    @Override
    public boolean usesMaterial(Material material) {
        return super.usesMaterial(material) || (ability != null && ability.usesMaterial(material));
    }

    @Override
    public Set<GameTimer> getTimers() {
        if (hasAbility()) {
            return SetUnion.union(super.getTimers(), ability.getTimers());
        } else return super.getTimers();
    }

    @Override
    public Set<GameTimer> getRunningTimers() {
        if (hasAbility()) {
            return SetUnion.union(super.getRunningTimers(), ability.getRunningTimers());
        } else return super.getRunningTimers();
    }

    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (hasAbility()) {
            return ability instanceof ActiveHandler && ((ActiveHandler) ability).ActiveSkill(material, clickType);
        }
        return false;
    }

    @Override
    public void TargetSkill(Material material, LivingEntity entity) {
        if (hasAbility()) {
            if (ability instanceof TargetHandler) ((TargetHandler) ability).TargetSkill(material, entity);
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    private void onPlayerDropItem(PlayerDropItemEvent e) {
        if (abilityChanger.removeAbility()) {
            e.setCancelled(true);
        }
    }

    @SubscribeEvent(onlyRelevant = true)
    private void onPlayerJoin(PlayerJoinEvent e) {
        abilityChanger.bossBar.addPlayer(getPlayer());
    }

    private static final Note[] NOTES = {
            Note.natural(1, Tone.D), Note.flat(1, Tone.F), Note.natural(1, Tone.A)
    };
    
    private class AbilityChanger extends AbilityTimer {

        private final BossBar bossBar = Bukkit.createBossBar("§f능력 없음", BarColor.WHITE, BarStyle.SOLID);
        private final List<Class<? extends AbilityBase>> allabilities = new ArrayList<>(
        		Arrays.asList(
        	    		AncientBerserker.class,
        	    		AncientBlackCandle.class,
        	    		AncientBombArrow.class,
        	    		AncientBrewer.class,
        	    		AncientChaos.class,
        	    		AncientHacker.class,
        	    		AncientSniper.class,
        	    		AncientTheHighPriestess.class,
        	    		AncientVoid.class,
        	    		AncientYeti.class,
        	    		AncientZombie.class
        	        )
        	    );
        private boolean accelerate = false;

        private AbilityChanger() {
            super(TaskType.NORMAL, RESET.getValue() * 10);
            setPeriod(TimeUnit.TICKS, 2);
            setBehavior(RestrictionBehavior.PAUSE_RESUME);
        }

        @Override
        protected void onStart() {
            bossBar.addPlayer(getPlayer());
            bossBar.setProgress(1d);
            try {
                setAbility(random.pick(allabilities));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
            this.accelerate = false;
        }

        @Override
        protected void run(int count) {
            if (count <= 14) {
                if (count == 3 || count == 7) {
                    for (Note note : NOTES) {
                        SoundLib.PIANO.playInstrument(getPlayer(), note);
                    }
                }
                SoundLib.BLOCK_WOODEN_TRAPDOOR_CLOSE.playSound(getPlayer(), 1, 2);
                SoundLib.BLOCK_WOODEN_TRAPDOOR_OPEN.playSound(getPlayer(), 1, 2);
                int index = (count % 7) - 1;
                if (index == -1) index = 6;
                bossBar.setColor(BarColor.values()[index]);
            } else {
                if (accelerate) setCount(getCount() + 4);
            }
            if (count % 5 == 0 && hasAbility()) {
                for (GameTimer timer : ability.getRunningTimers()) {
                    if (timer instanceof CooldownTimer) {
                        timer.setCount(timer.getCount() - 1);
                    }
                }
            }
            bossBar.setProgress((double) (getMaximumCount() - count) / getMaximumCount());
        }

        @Override
        protected void onEnd() {
            start();
        }

        @Override
        protected void onSilentEnd() {
            bossBar.removeAll();
        }

        public void setAbility(final Class<? extends AbilityBase> clazz) throws ReflectiveOperationException {
            removeAbility();
            final AbilityBase ability = create(clazz, getParticipant());
            Normalization.this.ability = ability;
            Normalization.this.ability.setRestricted(false);
            for (String line : Formatter.formatAbilityInfo(ability)) {
                getPlayer().sendMessage(line);
            }
            bossBar.setTitle("§5" + ability.getName() + "§r " + ability.getRank().getRankName() + "§r " + ability.getSpecies().getSpeciesName());
        }

        public boolean removeAbility() {
        	if (hasAbility()) {
                this.accelerate = true;
                ability.destroy();
                Normalization.this.ability = null;
                bossBar.setTitle("§f능력 없음");
                final List<Player> players = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate);
                for (Player player : players) {
                    final Participant participant = getGame().getParticipant(player);
                    Rooted.apply(participant, TimeUnit.TICKS, rooted);
                    new AbilityTimer(TaskType.NORMAL, 20) {
                        @Override
                        protected void run(int count) {
                            for (Vector vector : rootCircle) {
                                ParticleLib.REDSTONE.spawnParticle(player.getLocation().clone().add(vector).add(0, count / 20.0 * 2, 0), RGB.AQUA);
                            }
                        }
                    }.setPeriod(TimeUnit.TICKS, 1).start();
                }

                final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), heal + (players.size() * addheal), RegainReason.CUSTOM);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    getPlayer().setHealth(RangesKt.coerceIn(getPlayer().getHealth() + event.getAmount(), 0, getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                }
                SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer(), 1, 2);
                SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer(), 1, 1);
                PotionEffects.BLINDNESS.addPotionEffect(getPlayer(), 20, 1, true);
                new AbilityTimer(TaskType.NORMAL, 11) {
                    final Location location = getPlayer().getLocation();
                    @Override
                    protected void run(int count) {
                        circle2.rotateAroundAxisY(17);
                        for (Vector vector : circle2) {
                            ParticleLib.REDSTONE.spawnParticle(location.clone().add(vector).add(0, (count - 6) / 5.0 * 2, 0), random.pick(gradation));
                        }
                    }
                }.setPeriod(TimeUnit.TICKS, 1).start();
                return true;
            } else return false;
        }

    }
	
}
