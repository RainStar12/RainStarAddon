package RainStarAbility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "하늘고래", rank = Rank.SPECIAL, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §d드림 이터§f: 누군가를 죽이면 적의 §e꿈§f을 먹어 §a꿈 레벨§f을 올립니다.",
		" §e꿈§f을 먹을 때 체력을 §b12§f% 회복하고, 남은 쿨타임이 §b28§f%로 줄어듭니다.",
		" 만약 능력 지속 중에 §e꿈§f을 먹었다면 지속시간을 $[ADD_DURATION]초 연장합니다.",
		" §a꿈 레벨§f로 얻는 효과는 §7철괴를 좌클릭§f하여 볼 수 있습니다.",
		"§7철괴 우클릭 §8- §b드림 아쿠아리움§f: $[DURATION]초간 나갈 수 없는 §b꿈§f의 §b들판§f을 펼칩니다.",
		" 영역 내에서 자신이 잃은 체력의 $[HEAL_PERCENT]%는 영역 지속이 끝날 때 §d회복§f됩니다.",
		" 또한 §a꿈 레벨§f에 따른 추가 효과를 사용할 수 있습니다. $[COOLDOWN]",
		"§7영역 내 패시브 §8- §3웨이브§f: 영역의 중심에서부터 파도가 퍼져나가",
		" 적을 피해입히며 밀쳐냅니다. 피해는 중심에서 멀어질수록 강력해집니다.",
		" 피해는 영역에서 자신의 최고 대미지에 비례§8(§7× 0.5~1.5§8)§f합니다.",
		"§b[§7아이디어 제공자§b] §bSleepground"
		})

public class SkyWhale extends AbilityBase implements ActiveHandler {

	public SkyWhale(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> ADD_DURATION = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "add-duration", 10, "# 추가 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "duration", 20, "# 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "cooldown", 135, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> WAVE_PERIOD = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "wave-period", 40, "# 파도의 주기 (단위: 틱, 1틱 = 0.05초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> HEAL_PERCENT = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "heal-percent", 70, "# 영역이 끝난 후 회복량 (단위: %)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> INCREASE_DAMAGE = abilitySettings.new SettingObject<Double>(
			SkyWhale.class, "increase-damage", 1.0, "# 꿈 레벨당 추가 공격력") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§b꿈 레벨§f: §a" + (dreamlevel == 5 ? "MAX" : dreamlevel));
		}
	}

	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 50);	
	private final int period = WAVE_PERIOD.getValue();
	private final int addDuration = ADD_DURATION.getValue();
	private final double increasedamage = INCREASE_DAMAGE.getValue();
	private boolean isSkillRunning = false;
	private Random random = new Random();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private ActionbarChannel ac = newActionbarChannel();
	
	private int dreamlevel = 0;
	
	private double bestDamage = 7;
	private double lostHealth = 0;
	
	private final Circle circle = Circle.of(15, 100);

	private Location center;
	private double currentRadius;
	
	private int stacks;
	private boolean turns = true;

	private final static Color sky = Color.fromRGB(72, 254, 254), mint = Color.fromRGB(236, 254, 254), snow = Color.fromRGB(28, 254, 243),
			teal = Color.TEAL, lime = Color.fromRGB(49, 254, 32), yellow = Color.YELLOW, pink = Color.fromRGB(254, 174, 201),
			orange = Color.fromRGB(254, 177, 32), applemint = Color.fromRGB(34, 253, 220);
	
	private RGB aqua1 = RGB.of(74, 208, 229), aqua2 = RGB.of(85, 212, 231), aqua3 = RGB.of(96, 216, 232),
			aqua4 = RGB.of(107, 221, 234), aqua5 = RGB.of(118, 225, 235), aqua6 = RGB.of(130, 230, 237),
			aqua7 = RGB.of(148, 234, 240), aqua8 = RGB.of(167, 238, 243), aqua9 = RGB.of(186, 242, 245),
			aqua10 = RGB.of(204, 245, 247), aqua11 = RGB.of(223, 249, 250);
	
	@SuppressWarnings("serial")
	private List<RGB> aquas = new ArrayList<RGB>() {
		{
			add(aqua1);
			add(aqua2);
			add(aqua3);
			add(aqua4);
			add(aqua5);
			add(aqua6);
			add(aqua7);
			add(aqua8);
			add(aqua9);
			add(aqua10);
			add(aqua11);
		}
	};
	
	private RGB aqua = aqua1;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
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
	
	private final Predicate<Entity> soundpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
		    	if (!isSkillRunning) {
		    		if (!cooldown.isCooldown()) {
		    	   		showSpiral(RGB.AQUA);
		    	   		isSkillRunning = true;
		    	   		return true;		
		    		}
		    	} else {
		    		getPlayer().sendMessage("§3[§b!§3] §c아직 스킬이 지속 중입니다.");
		    	}	
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		getPlayer().sendMessage("§e=========== §b꿈 스킬 §e===========");
	    		final StringJoiner joiner = new StringJoiner("\n");
	    		joiner.add("§aLevel " + (dreamlevel == 5 ? "MAX" : dreamlevel) + " §7| §c추가 공격력 §e+" + (dreamlevel * increasedamage));
	    		if (dreamlevel == 0) joiner.add("§7아직 활성화된 스킬이 없습니다.");
	    		if (dreamlevel >= 1) joiner.add("§c화염 내성§7: §f모든 화염계 피해에 내성이 생깁니다.");
	    		if (dreamlevel >= 2) joiner.add("§a정보 통제§7: §f적의 체력을 실시간으로 확인 가능합니다.");
	    		if (dreamlevel >= 3) joiner.add("§b자유 비행§7: §f자유롭게 비행할 수 있습니다.");
	    		if (dreamlevel >= 4) {
	    			joiner.add("§d공간 초월§7: §f3초마다 사거리가 매우 길고 지형지물을 관통하는");
	    			joiner.add("           §f특수한 근접 공격을 사용 가능합니다.");
	    		}	
	    		if (dreamlevel == 5) {
	    			joiner.add("§6시간 단축§7: §f사망 위기에 빠지면 능력 지속시간을 0초로 만들고");
	    			joiner.add("           §f영역이 끝나면 받는 회복을 바로 받습니다.");
	    		}
	    		getPlayer().sendMessage(joiner.toString());
	    		getPlayer().sendMessage("§e================================");
	    	}
	    }
		return false;
	}
	
    private final AbilityTimer attackcool = new AbilityTimer(60) {
    	
    	@Override
		public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (skill.isRunning() && LocationUtil.isInCircle(center, getPlayer().getLocation(), 15)) {
				if (dreamlevel >= 1) {
					if (e.getCause().equals(DamageCause.LAVA) || e.getCause().equals(DamageCause.FIRE_TICK) || e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.HOT_FLOOR)) {
						e.setDamage(0);
						e.setCancelled(true);
					}	
				}
				
				lostHealth += (e.getFinalDamage() * 0.85);
				
				if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && dreamlevel == 5) {
					ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 100, 0.3, MaterialX.CLOCK);
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
					e.setCancelled(true);
					skill.stop(false);
				}
			}	
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
		
		if (skill.isRunning()) {
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (getPlayer().equals(projectile.getShooter())) {
					e.setDamage(e.getDamage() + (dreamlevel * increasedamage));
					if (e.getDamage() > bestDamage) bestDamage = e.getDamage();
				}
			} else if (e.getDamager().equals(getPlayer())) {
				e.setDamage(e.getDamage() + (dreamlevel * increasedamage));
				if (e.getDamage() > bestDamage) bestDamage = e.getDamage();
			} else if (e.getDamager().hasMetadata("Wave")) {
				if ((e.getDamage() * 0.8) > bestDamage) bestDamage = e.getDamage() * 0.8;
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (skill.isRunning()) {
			if (LocationUtil.isInCircle(center, e.getFrom(), 15) && !LocationUtil.isInCircle(center, e.getTo(), 15)) {
				e.setTo(e.getFrom());
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (skill.isRunning() && dreamlevel >= 4) {
			if (e.getItem() != null && swords.contains(e.getItem().getType()) && !attackcool.isRunning()) {
				if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
					final ItemStack mainHand = getPlayer().getInventory().getItemInMainHand();
					if (swords.contains(mainHand.getType())) {
						new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection().multiply(.4), mainHand.getEnchantmentLevel(Enchantment.DAMAGE_ALL), getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue()).start();
						attackcool.start();
					}
				}
			}	
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getPlayer().equals(e.getEntity().getKiller())) {
			double addHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.12;
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), addHealth, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + addHealth);	
			}
			if (cooldown.isRunning()) {
				cooldown.setCount((int) (cooldown.getCount() * 0.28));
			}
			if (skill.isDuration()) {
				skill.setCount(skill.getCount() + (addDuration * 20));
			}
			final Firework firework = getPlayer().getWorld().spawn(e.getEntity().getLocation(), Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			Color color1 = null, color2 = null, color3 = null, color4 = null;
			switch(random.nextInt(5)) {
			case 0:
				color1 = mint;
				color2 = snow;
				color3 = teal;
				color4 = sky;
				break;
			case 1:
				color1 = pink;
				color2 = yellow;
				color3 = lime;
				color4 = applemint;
				break;
			case 2:
				color1 = applemint;
				color2 = snow;
				color3 = lime;
				color4 = yellow;
				break;
			case 3:
				color1 = orange;
				color2 = yellow;
				color3 = pink;
				color4 = snow;
				break;
			case 4:
				color1 = Color.WHITE;
				color2 = Color.SILVER;
				color3 = Color.GRAY;
				color4 = Color.BLACK;
				break;
			}
			meta.addEffect(
					FireworkEffect.builder()
					.withColor(color1, color2, color3, color4)
					.withTrail()
					.withFlicker()
					.with(Type.STAR)
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
			if (dreamlevel < 5) SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1);
			dreamlevel = Math.min(5, dreamlevel + 1);
			ac.update("§b꿈 레벨§f: §a" + (dreamlevel == 5 ? "MAX" : dreamlevel));
		}
	}
	
	private final AbilityTimer biggerEffect = new AbilityTimer() {
		
		@Override
		protected void onStart() {
			center = LocationUtil.floorY(getPlayer().getLocation().clone());
			currentRadius = 0;
			final Firework firework = getPlayer().getWorld().spawn(center, Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(
					FireworkEffect.builder()
					.withColor(aqua1.getColor(), aqua11.getColor(), Color.AQUA)
					.with(Type.BALL_LARGE)
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
			SoundLib.ENTITY_PLAYER_SPLASH.playSound(center, 2, 0.5f);
		}
		
		@Override
		protected void run(int count) {
			if (count % 3 == 0) {
				if (turns)
					stacks++;
				else
					stacks--;
				if (stacks % (aquas.size() - 1) == 0) {
					turns = !turns;
				}
				aqua = aquas.get(stacks);	
			}
			
			double playerY = getPlayer().getLocation().getY();
			if (currentRadius < 15) currentRadius += 0.5;
			for (Iterator<Location> iterator = Circle.iteratorOf(center, currentRadius, 100); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + 0.1);
				ParticleLib.REDSTONE.spawnParticle(loc, aqua);
			}
			if (currentRadius == 15) this.stop(false);
		}
		
		@Override
		protected void onEnd() {
			skill.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cooldown) {
		
		private boolean up = true;
		private double addY = 0.1;
		
		@Override
		protected void onDurationStart() {
			if (dreamlevel >= 2) {
				for (Player player : getPlayer().getWorld().getPlayers()) {
					if (predicate.test(player)) {
						new DataCatch(player).start();
					}
				}
			}
		}

		@Override
		protected void onDurationProcess(int count) {
			ParticleLib.DRIP_WATER.spawnParticle(center, 15, 5, 15, 20, 0);
			
			if (dreamlevel >= 3) {
				getPlayer().setAllowFlight(true);
				if (getPlayer().isFlying()) ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 10, 0);
			}
			
			if (count % 2 == 0) {
				double playerY = getPlayer().getLocation().getY();
				for (Location loc : circle.toLocations(center).floor(playerY)) {
					if (turns)
						stacks++;
					else
						stacks--;
					if (stacks % (aquas.size() - 1) == 0) {
						turns = !turns;
					}
					aqua = aquas.get(stacks);	
					if (addY >= 1.5) up = false;
					else if (addY <= 0) up = true;		
					addY = addY + (up ? 0.05 : -0.05);
					Location location = loc.clone().add(0, addY + 0.3, 0);
					ParticleLib.REDSTONE.spawnParticle(location, aqua);
				}
			}
			
			if (count % 10 == 0) {
				for (Player p : LocationUtil.getEntitiesInCircle(Player.class, center, 15, soundpredicate)) {
					SoundLib.BLOCK_WATER_AMBIENT.playSound(p, 1, 1.2f);	
				}
			}
			
			if (count % period == 0) {
				new Wave(bestDamage).start();
				SoundLib.PLING.playInstrument(center, 1.5f, Note.sharp(0, Tone.F));
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			bestDamage = 7;
			isSkillRunning = false;
			if (dreamlevel >= 3) {
				if (!getPlayer().getGameMode().equals(GameMode.CREATIVE) && !getPlayer().getGameMode().equals(GameMode.SPECTATOR)) getPlayer().setAllowFlight(false);
			}
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), lostHealth, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + lostHealth);	
			}
			lostHealth = 0;
		}
	
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1);
	
	public class Wave extends AbilityTimer {
		
		private final double damage;
		private double waveRadius;
		private final Predicate<Entity> predicate;
		private Set<Damageable> hitEntity = new HashSet<>();
		private ArmorStand damager;
		private boolean up = true;
		private double addY = 0.1;
		
		private RGB waveColor1 = RGB.of(1, 183, 194), waveColor2 = RGB.of(18, 186, 197), waveColor3 = RGB.of(36, 189, 200),
				waveColor4 = RGB.of(55, 192, 204), waveColor5 = RGB.of(73, 195, 207), waveColor6 = RGB.of(92, 198, 211),
				waveColor7 = RGB.of(110, 200, 214), waveColor8 = RGB.of(128, 202, 217), waveColor9 = RGB.of(147, 205, 221),
				waveColor10 = RGB.of(177, 213, 228), waveColor11 = RGB.of(202, 222, 236), waveColor12 = RGB.AQUA;
		
		@SuppressWarnings("serial")
		private List<RGB> waveColors = new ArrayList<RGB>() {
			{
				add(waveColor1);
				add(waveColor2);
				add(waveColor3);
				add(waveColor4);
				add(waveColor5);
				add(waveColor6);
				add(waveColor7);
				add(waveColor8);
				add(waveColor9);
				add(waveColor10);
				add(waveColor11);
				add(waveColor12);
			}
		};
		
		private RGB color = waveColor1;
		
		public Wave(double damage) {
			setPeriod(TimeUnit.TICKS, 1);
			this.damage = damage;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(getPlayer())) return false;
					if (hitEntity.contains(entity)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(getPlayer().getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		public void onStart() {
			waveRadius = 0;
			damager = center.getWorld().spawn(center.clone().add(99999, 0, 99999), ArmorStand.class);
			damager.setVisible(false);
			damager.setInvulnerable(true);
			damager.setGravity(false);
			damager.setMetadata("Wave", NULL_VALUE);
			NMS.removeBoundingBox(damager);
		}
		
		@Override
		public void run(int count) {
			if (!skill.isRunning()) this.stop(false);
			if (waveRadius < 15) waveRadius += 0.35;
			else this.stop(false);
			
			if (count == 1) color = waveColor1;
			if (count % 4 == 0) color = waveColors.get(count / 4);
			if (count % 5 == 0) up = !up;
			
			addY = addY + (up ? 0.1 : -0.1);
			
			double playerY = getPlayer().getLocation().getY();
			for (Iterator<Location> iterator = Circle.iteratorOf(center, waveRadius, 75); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + addY + 0.2);
				ParticleLib.REDSTONE.spawnParticle(loc, color);
				if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(loc, 0, 0, 0, 1, 0);
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, 0.6, 0.6, predicate)) {
					double increase = (waveRadius / 15) + 0.5;
					damageable.damage(increase * damage, damager);
					hitEntity.add(damageable);
					damageable.setVelocity(VectorUtil.validateVector(damageable.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.1).setY(0.5)));
					if (damageable instanceof LivingEntity) PotionEffects.SLOW.addPotionEffect((LivingEntity) damageable, 100, 1, false);
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			SoundLib.ENTITY_PLAYER_SWIM.playSound(center, 2f, 1.4f);
			damager.remove();
		}
		
	}
	
	private void showSpiral(final RGB color) {
		new AbilityTimer(40) {
			private double radius = 10;
			private double count = 120;
			private double fourPi = Math.PI * 4;
			private double theta = 0;
			private double addSpiralY = 1;
			private boolean yUp = false;

			@Override
			protected void run(int a) {				
				Location me = getPlayer().getLocation();			
				for (int i = 0; i < 6; i++) {
					if (addSpiralY >= 1.6) yUp = false;
					else if (addSpiralY <= 0.4) yUp = true;
					addSpiralY = yUp ? Math.min(1.6, addSpiralY + 0.02) : Math.max(0.4, addSpiralY - 0.02);
					radius -= (0.5 / 12);
					theta += (fourPi / count);
					Location loc = me.clone().add(Math.cos(theta) * radius, addSpiralY, Math.sin(theta) * radius);
					ParticleLib.REDSTONE.spawnParticle(loc, color);
					ParticleLib.DRIP_WATER.spawnParticle(loc, 0, 0, 0, 1, 0);
					SoundLib.ENTITY_BOAT_PADDLE_WATER.playSound(loc);
				}
			}
			
			@Override
			protected void onEnd() {
				biggerEffect.start();
			}
			
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	public class DataCatch extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int health = 0;
		private int maxHealth = 0;
		
		private DataCatch(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§a|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			hologram.display(getPlayer());
		}
		
		@Override
		protected void run(int count) {
			health = (int) player.getHealth();
			maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			hologram.setText(Strings.repeat("§c|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			if (!skill.isRunning()) this.stop(false);
			if (health <= 0) this.stop(false);
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
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int sharpnessEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;
		private Set<LivingEntity> hitEntity = new HashSet<>();

		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int sharpnessEnchant, double damage) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.2, -1.2, -1.2, 1.2, 1.2, 1.2);
			this.forward = arrowVelocity.multiply(10);
			this.sharpnessEnchant = sharpnessEnchant;
			this.damage = damage;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (hitEntity.contains(entity)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void onStart() {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(shooter.getLocation(), 1, 0.75f);
		}

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
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
				final Location location = iterator.next();
				entity.setLocation(location);
				if (!isRunning()) {
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						livingEntity.damage((float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant)) * 1.2, getPlayer());
						hitEntity.add(livingEntity);
					}
				}
				ParticleLib.CRIT.spawnParticle(location, 0, 0, 0, 1, 0);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}
	
}