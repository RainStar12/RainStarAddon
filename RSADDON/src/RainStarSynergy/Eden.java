package RainStarSynergy;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.function.Predicate;

@AbilityManifest(name = "에덴", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴 우클릭 시 무적 지대의 에덴 동산을 만듭니다. $[COOLDOWN_CONFIG]",
		"에덴 동산에서는 일정 주기로 §b선§c악§e과§f가 나오게 됩니다.",
		"§b선§c악§e과§f를 먹으면 절반의 확률로 최대 체력의 $[HEAL_PERCENTAGE]%를 §d회복§f하거나,",
		"혹은 최대 체력의 $[DEAL_PERCENTAGE]%를 잃을 수 있습니다.",
		"적을 회복시키면 회복의 절반만큼 §e흡수 체력§f을, 적과 본인 관계없이 체력을 잃으면",
		"잃은 체력의 20%만큼 §c영구 공격력§f을 얻습니다.",
		"10% 확률로 두 배의 효과를 지닌 §b선§c악§e과§f가 나올 수 있습니다."
		})
public class Eden extends Synergy implements ActiveHandler {
	
	public Eden(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = 
			synergySettings.new SettingObject<Integer>(Eden.class, "cooldown", 120, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> HEAL_PERCENTAGE = 
			synergySettings.new SettingObject<Integer>(Eden.class, "heal-percentage", 10, 
					"# 선악과의 회복량", "# 단위: 최대 체력의 n%") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DEAL_PERCENTAGE = 
			synergySettings.new SettingObject<Integer>(Eden.class, "deal-percentage", 5, 
					"# 선악과의 피해량", "# 단위: 최대 체력의 n%") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	@Override
	public void onUpdate(Update update) {
		if (update.equals(Update.RESTRICTION_CLEAR)) {
			ac.update("§c추가 공격력§f: §e" + df.format(addDamage));
		}
	}

	private static final Note D = Note.natural(0, Tone.D), FSharp = Note.sharp(1, Tone.F), LowA = Note.natural(0, Tone.A), A = Note.natural(1, Tone.A);

	private final DecimalFormat df = new DecimalFormat("0.00");
	private ActionbarChannel ac = newActionbarChannel();
	private static final Circle headCircle = Circle.of(0.5, 10);
	private final double healp = HEAL_PERCENTAGE.getValue() * 0.01;
	private final double dealp = DEAL_PERCENTAGE.getValue() * 0.01;
	private double addDamage = 0;
	private final Cooldown cooldownTimer = new Cooldown(COOLDOWN_CONFIG.getValue());
	private final Predicate<Entity> ONLY_PARTICIPANTS = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			return (!(entity instanceof Player)) || (getGame().isParticipating(entity.getUniqueId())
					&& (!(getGame() instanceof DeathManager.Handler) || !((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	private double currentRadius;
	private Location center = null;
	private final Random random = new Random();
	private static final RGB color = RGB.of(0, 254, 128);
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!skill.isDuration() && !cooldownTimer.isCooldown()) {
				return skill.start();
			}
		}
		return false;
	}
	
	private final Duration skill = new Duration(120, cooldownTimer) {

		private int count;
		private int soundCount;

		@Override
		public void onDurationStart() {
			count = 1;
			soundCount = 1;
			currentRadius = 11;
			center = getPlayer().getLocation();
		}

		@Override
		public void onDurationProcess(int seconds) {
			if (count <= 10) {
				double playerY = getPlayer().getLocation().getY();
				for (Iterator<Location> iterator = Circle.iteratorOf(center, count, count * 16); iterator.hasNext(); ) {
					Location loc = iterator.next();
					loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + 0.1);
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}

				final Note note;
				switch (count) {
					case 1:
					case 4:
					case 7:
					case 10:
						note = D;
						break;
					case 2:
					case 6:
					case 8:
						note = FSharp;
						break;
					case 3:
					case 5:
					case 9:
						note = LowA;
						break;
					default:
						note = null;
						break;
				}

				SoundLib.BELL.playInstrument(LocationUtil.getNearbyEntities(Player.class, center, 20, 20, ONLY_PARTICIPANTS), note);
			} else {
				double playerY = getPlayer().getLocation().getY();
				for (Iterator<Location> iterator = Circle.iteratorOf(center, currentRadius, (int) (currentRadius * 16)); iterator.hasNext(); ) {
					Location loc = iterator.next();
					loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + 0.1);
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}
				if (count % 4 == 0) {
					for (Location loc : LocationUtil.getRandomLocations(center, currentRadius, 1)) {
						loc.add(0, 1, 0);
						boolean special = false;
						if (random.nextInt(10) == 0) special = true;
						new FruitSpawner(loc, special).start();
					}	
				}
				if (soundCount % 5 == 0) {
					soundCount = 1;
					for (LivingEntity livingEntity : LocationUtil.getEntitiesInCircle(LivingEntity.class, center, currentRadius, ONLY_PARTICIPANTS)) {
						PotionEffects.GLOWING.addPotionEffect(livingEntity, 4, 0, true);
						if (livingEntity instanceof Player) SoundLib.BELL.playInstrument((Player) livingEntity, A);
					}
				} else {
					for (LivingEntity livingEntity : LocationUtil.getEntitiesInCircle(LivingEntity.class, center, currentRadius, ONLY_PARTICIPANTS)) {
						PotionEffects.GLOWING.addPotionEffect(livingEntity, 4, 0, true);
					}
				}
				soundCount++;
			}
			ParticleLib.NOTE.spawnParticle(getPlayer().getEyeLocation().clone().add(0, 0.6, 0).add(headCircle.get(count % 10)));
			count++;
		}

		@Override
		public void onDurationEnd() {
			for (Player player : LocationUtil.getEntitiesInCircle(Player.class, center, currentRadius, ONLY_PARTICIPANTS)) {
				SoundLib.BELL.playInstrument(player, D);
				SoundLib.BELL.playInstrument(player, FSharp);
				SoundLib.BELL.playInstrument(player, A);
			}
			center = null;
		}

		@Override
		public void onDurationSilentEnd() {
			center = null;
		}

	}.setPeriod(TimeUnit.TICKS, 2);
	
	public class FruitSpawner extends AbilityTimer implements Listener {
		
		private final Location location;
		private final boolean special;
		private Item fruit;
		
		public FruitSpawner(Location location, boolean special) {
			super(TaskType.REVERSE, 60);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
			this.special = special;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			ItemStack fruittype;
			fruittype = new ItemStack(special ? Material.GOLDEN_APPLE : Material.APPLE);
			ItemMeta meta = fruittype.getItemMeta();
			meta.setDisplayName(special ? "§b§l선§c§l악§e§l과" : "§b선§c악§e과");
			fruit = location.getWorld().dropItem(location, fruittype);
		}
		
		@Override
		public void run(int count) {
			if (count <= 10) {
				if (count % 2 == 0) fruit.setGlowing(true);
				else fruit.setGlowing(false);
			}
		}
 		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			fruit.remove();
		}
		
		@EventHandler
		public void onEntityPickup(EntityPickupItemEvent e) {
			if (e.getItem().equals(fruit) && e.getEntity() instanceof Player) {
				Player p = (Player) e.getEntity();
				e.setCancelled(true);
				this.stop(false);
				SoundLib.ENTITY_ITEM_PICKUP.playSound(e.getEntity().getLocation(), 1, 0.65f);
				if (random.nextBoolean()) {
					final double maxHP = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					double heal = maxHP * healp * (special ? 2 : 1);
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, heal, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Healths.setHealth(p, p.getHealth() + (heal));
						if (!getPlayer().equals(p)) {
							NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + (heal / 2)));
						}
					}
				} else {
					final double maxHP = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					double deal = maxHP * dealp * (special ? 2 : 1);
					Healths.setHealth(p, Math.max(1, p.getHealth() - deal));
					addDamage += (deal / 5);
					ac.update("§c추가 공격력§f: §e" + df.format(addDamage));
				}
			}
		}
		
	}

	@SubscribeEvent(ignoreCancelled = true, priority = 6, childs = {EntityDamageByEntityEvent.class, EntityDamageByBlockEvent.class})
	public void onEntityDamage(EntityDamageEvent e) {
		if (center != null) {
			if (LocationUtil.isInCircle(center, e.getEntity().getLocation(), currentRadius)) {
				ParticleLib.HEART.spawnParticle(e.getEntity().getLocation(), 2, 2, 2, 5);
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() + addDamage);
		}	
	}

}
