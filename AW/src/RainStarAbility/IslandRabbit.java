package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "섬토끼", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §e밝은 귀§f: 주변 $[RANGE]칸 이내에 적이 들어올 경우 알 수 있습니다.",
		" 이때 5초 내로 철괴를 우클릭하면 맞은편으로 즉시 토낍니다. $[COOLDOWN]",
		"§7웅크리기 §8- §b깡총깡총§f: 땅에서 웅크렸다 떼면 높게 점프할 수 있습니다.",
		" 이 점프를 하고 난 뒤의 낙하 피해량이 §51/7§f로 감소합니다.",
		"§7치명타 공격 §8- §d강타§f: 근접 혹은 원거리 공격으로 치명타 피해를 입힐 때",
		" 피해량의 $[TRUE_DAMAGE]%는 트루 대미지의 §c고정 피해§f로 입힙니다.",
		" 또한 치명타 공격 시 무기의 §3강타 인챈트§f의 $[PERCENTAGE]%를 대상에게 적용 가능합니다.",
		" 강타 인챈트 효과는 치명타 $[SMITE_COUNT]번에 1번 발동합니다.",
		"§b[§7아이디어 제공자§b] §5SUHYEN"
		})

@SuppressWarnings("deprecation")
public class IslandRabbit extends AbilityBase implements ActiveHandler {

	public IslandRabbit(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(IslandRabbit.class,
			"cooldown", 80, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> TRUE_DAMAGE = abilitySettings.new SettingObject<Integer>(IslandRabbit.class,
			"true-damage", 20, "# 강타 효과의 트루 대미지 비율", "# 단위: 퍼센트") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> PERCENTAGE = abilitySettings.new SettingObject<Integer>(IslandRabbit.class,
			"percentage", 65, "# 강타 인챈트 추가 피해의 적용값", "# 단위: 퍼센트") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANGE = abilitySettings.new SettingObject<Integer>(IslandRabbit.class,
			"range", 15, "# 적 알람 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> SMITE_COUNT = abilitySettings.new SettingObject<Integer>(IslandRabbit.class,
			"smite-count", 2, "# 치명타 강타 인챈트 적용 조건") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	attackCooldownChecker.start();
	    	sneakchecker.start();
	    }
	}
	
	private boolean attackCooldown = false;
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	private static final Circle circle = Circle.of(1, 30);
	
	private RGB color = RGB.of(218, 162, 254);

	private Set<Arrow> critArrows = new HashSet<>();
	
	private ActionbarChannel ac = newActionbarChannel();
	
	private int longsneak = 0;
	private int sneakstack = 0;
	private boolean jumped = false;
	private boolean damagedecrease = false;
	
	private Player target;
	
	private final int smitecount = SMITE_COUNT.getValue();
	private int nowcount = smitecount;
	private int range = RANGE.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	
	private final double smitedamage = 2.5 * (PERCENTAGE.getValue() * 0.01);
	private final int trueDamageValue = TRUE_DAMAGE.getValue();
	
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
	
	private final AbilityTimer attackCooldownChecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer warptimer = new AbilityTimer(50) {
		
		@Override
		public void run(int count) {
			if (!cooldown.isRunning()) {
				ac.update("§d도망 가능§7: " + df.format(this.getCount() * 0.1) + "초");
				Block lastEmpty = null;
				try {
					Vector vector = target.getLocation().clone().subtract(getPlayer().getLocation().toVector()).toVector();
					for (BlockIterator iterator = new BlockIterator(getPlayer().getWorld(), getPlayer().getLocation().toVector(), 
							vector.setY(0), 1, (int) (vector.length() * 2)); iterator.hasNext(); ) {
						final Block block = iterator.next();
						if (!block.getType().isSolid()) {
							lastEmpty = block;
						}
					}
				} catch (IllegalStateException ignored) {}
				if (lastEmpty != null) {
					for (Location loc : circle.toLocations(lastEmpty.getLocation()).floor(lastEmpty.getLocation().getY())) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
					}	
				}	
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer sneakchecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!jumped) {
				if (getPlayer().isSneaking()) {
					if (longsneak == 0) NMS.sendTitle(getPlayer(), "", Strings.repeat("§b§l⤊", sneakstack).concat(Strings.repeat("§7§l⤊", 10 - sneakstack)), 0, 100, 1);
					longsneak++;
					if (longsneak % 10 == 0) {
						if (sneakstack != 10) SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 0.5f, (float) (1 + (0.1 * (sneakstack + 1))));
						sneakstack = Math.min(10, sneakstack + 1);
						if (sneakstack == 10) NMS.sendTitle(getPlayer(), "", Strings.repeat("§d§l⤊", sneakstack).concat(Strings.repeat("§7§l⤊", 10 - sneakstack)), 0, 100, 1);
						else if (sneakstack > 5) NMS.sendTitle(getPlayer(), "", Strings.repeat("§a§l⤊", sneakstack).concat(Strings.repeat("§7§l⤊", 10 - sneakstack)), 0, 100, 1);
						else NMS.sendTitle(getPlayer(), "", Strings.repeat("§b§l⤊", sneakstack).concat(Strings.repeat("§7§l⤊", 10 - sneakstack)), 0, 100, 1);
					}	
				} else {
					if (getPlayer().isOnGround()) {
						NMS.clearTitle(getPlayer());
						longsneak = 0;
						if (sneakstack > 0) {
							ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 50, 1, getPlayer().getLocation().clone().add(0, -1, 0).getBlock());
							getPlayer().setVelocity(new Vector(getPlayer().getVelocity().getX() * 1.3, ((0.15 * sneakstack) + 0.6), getPlayer().getVelocity().getZ() * 1.3));
							sneakstack = 0;
							jumped = true;
							damagedecrease = true;
							particle.start();
						}	
					}
				}	
			} else {
				if (getPlayer().isOnGround()) {
					jumped = false;
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer particle = new AbilityTimer() {
		
		private VectorIterator iterator;
		private Location center;
		
    	@Override
		public void onStart() {
    		this.iterator = Circle.infiniteIteratorOf(0.5, 10);
			center = getPlayer().getLocation();
    	}
		
    	@Override
		public void run(int i) {
			for (int j = 0; j < 10; j++) {
	    		center = getPlayer().getLocation();
				Location loc = center.clone().add(iterator.next());
				ParticleLib.REDSTONE.spawnParticle(loc, RGB.of(185, 98, 185));
			}
			if (getPlayer().getVelocity().getY() < 0) {
				stop(false);
			}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
	    		if (!cooldown.isCooldown() && warptimer.isRunning()) {
	    			if (target != null) {
	        			Block lastEmpty = null;
	    				try {
	    					Vector vector = target.getLocation().clone().subtract(getPlayer().getLocation().toVector()).toVector();
	    					for (BlockIterator iterator = new BlockIterator(getPlayer().getWorld(), getPlayer().getLocation().toVector(), 
	    							vector.setY(0), 
	    							1, (int) (vector.length() * 2)); iterator.hasNext(); ) {
	    						final Block block = iterator.next();
	    						if (!block.getType().isSolid()) {
	    							lastEmpty = block;
	    						}
	    					}
	    				} catch (IllegalStateException ignored) {}
	        			
	    				if (lastEmpty != null) {
	    					SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.2f);
	    					ParticleLib.PORTAL.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 100, 1);
	    					getPlayer().teleport(LocationUtil.floorY(lastEmpty.getLocation()).setDirection(getPlayer().getLocation().getDirection()));
	    	    			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 100, 0.35);
	    	    			SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.3f);
	    	    			PotionEffects.SPEED.addPotionEffect(getPlayer(), 100, 3, true);
	    					PotionEffects.INVISIBILITY.addPotionEffect(getPlayer(), 100, 0, true);
	    	    			warptimer.stop(false);
	    	    			target = null;
	    	    			return cooldown.start();
	    				} else {
	    					getPlayer().sendMessage("§5[§d!§5] §f맞은편 방향에 이동할 수 있는 곳이 없습니다.");
	    				}
	    			}
	    		}
	    	}
	    }
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL) && damagedecrease) {
				e.setDamage(e.getDamage() / 7);
				damagedecrease = false;
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			if (NMS.isArrow(e.getEntity())) {
				Arrow arrow = (Arrow) e.getEntity();
				if (arrow.isCritical()) {
					critArrows.add(arrow);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		Participant participant = getGame().getParticipant(e.getPlayer());
		if (participant != null && !getParticipant().equals(participant) && getPlayer().getWorld().equals(participant.getPlayer().getWorld())) {
			if (!LocationUtil.isInCircle(getPlayer().getLocation(), e.getFrom(), range) && LocationUtil.isInCircle(getPlayer().getLocation(), e.getTo(), range)) {
				if (!participant.getPlayer().equals(target)) getPlayer().sendMessage("§d섬토끼 §7| §e" + participant.getPlayer().getName() + "§f님이 접근중");
				target = participant.getPlayer();
				warptimer.start();
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (NMS.isArrow(e.getEntity())) {
			Arrow arrow = (Arrow) e.getEntity();
			if (critArrows.contains(arrow) && e.getHitEntity() == null) critArrows.remove(arrow);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (isCriticalHit(getPlayer(), attackCooldown)) {
				if (nowcount >= smitecount) {
					ItemStack mainHand = getPlayer().getInventory().getItemInMainHand();
					if (mainHand.containsEnchantment(Enchantment.DAMAGE_UNDEAD)) {
						e.setDamage(e.getDamage() + (smitedamage * mainHand.getEnchantmentLevel(Enchantment.DAMAGE_UNDEAD)));
						getPlayer().sendMessage("§c[§b!§c] §3강타 추가 피해§c " + (smitedamage * mainHand.getEnchantmentLevel(Enchantment.DAMAGE_UNDEAD)) + " §f적용!");
						nowcount = 0;
					}
				}
				
				double trueDamage = (e.getDamage() * (trueDamageValue * 0.01));
				e.setDamage(e.getDamage() - trueDamage);
				if (Damages.canDamage(player, DamageCause.ENTITY_ATTACK, trueDamage)) {
					Healths.setHealth(player, Math.max(1, player.getHealth() - trueDamage));	
				}
				
				nowcount++;
				attackCooldown = true;
				
				ParticleLib.CRIT_MAGIC.spawnParticle(player.getLocation(), 0.25, 1, 0.25, 100, 1);
			}
		}
		
		if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player) {
			Arrow arrow = (Arrow) e.getDamager();
			Player player = (Player) e.getEntity();
			if (critArrows.contains(arrow) && !e.getEntity().equals(getPlayer())) {
				
				double trueDamage = (e.getDamage() * (trueDamageValue * 0.01));
				e.setDamage(e.getDamage() - trueDamage);
				if (Damages.canDamage(player, DamageCause.ENTITY_ATTACK, trueDamage)) {
					Healths.setHealth(player, Math.max(1, player.getHealth() - trueDamage));	
				}
				
				ParticleLib.CRIT_MAGIC.spawnParticle(player.getLocation(), 0.25, 1, 0.25, 100, 1);
			}
		}
	}
	
}
