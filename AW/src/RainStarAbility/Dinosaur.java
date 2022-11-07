package RainStarAbility;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks.Behavior;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "공룡", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7게임 시작 §8- §6화석화§f: 공룡 알 화석이 되어 땅 속에 파묻힙니다.",
		" 그동안은 §3유령 상태§f가 되어 자유롭게 이동 및 순간 이동이 가능합니다.",
		" 누군가가 이 화석을 찾거나 유령 상태로 빙의를 시도할 때",
		" 유령 상태가 해제되고 화석에서 깨어나 §e흡수 체력§f을 잔뜩 획득합니다.",
		" 혹은 $[LONGEST_WAIT]초가 지나도 자동으로 풀려납니다.",
		" 강제로 깨어날 경우에는 흡수 체력을 절반만 획득합니다.",
		"§7철괴 좌클릭 §8- §2압도적인 힘으로§f: 조금 떠오른 뒤 바라보는 방향으로 찍어내립니다.",
		" 이를 §2$[COUNT]§f회, §2$[DAMAGE]§f의 피해로 반복합니다. $[COOLDOWN]",
		" 이후 대미지를 입힌 사람 수에 비례해 §e흡수 체력§f을 천천히 획득합니다.",
		"§b[§7아이디어 제공자§b] §2ehdgh141"
		},
		summarize = {
		"게임 시작 후 오랜 시간 §3관전 모드§f로 자유로이 이동할 수 있습니다.",
		"이때 §b빙의를 시도§f하거나 누군가 내 시작 포인트에 있던 §2공룡 알§f을 찾는다면",
		"관전 모드 해제 및 §e흡수 체력§f을 잔뜩 획득합니다.",
		"§7철괴 좌클릭 시§f 바라보는 방향으로 찍어내리며 피해를 입히고",
		"피해입힌 사람 수에 비례해 §e흡수 체력§f을 천천히 획득합니다."
		})

@SuppressWarnings("deprecation")
public class Dinosaur extends AbilityBase implements ActiveHandler {
	
	public Dinosaur(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Dinosaur.class, "cooldown", 120,
			"# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> LONGEST_WAIT = 
			abilitySettings.new SettingObject<Integer>(Dinosaur.class, "longest-wait", 180,
			"# 최대 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COUNT = 
			abilitySettings.new SettingObject<Integer>(Dinosaur.class, "count", 9,
			"# 연속 찍기 횟수") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE = 
			abilitySettings.new SettingObject<Integer>(Dinosaur.class, "damage", 11,
			"# 연속 찍기의 피해량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> ABSORTION_AMOUNT = 
			abilitySettings.new SettingObject<Integer>(Dinosaur.class, "absortion-amount", 150,
			"# 획득하는 흡수 체력", "# 단위는 %로, 최대 체력에 비례합니다.") {

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
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int absortion = ABSORTION_AMOUNT.getValue();
	private final int skillcount = COUNT.getValue();
	private final int skilldamage = DAMAGE.getValue();
	private int skillstack = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private boolean nofall = false;
	private Item egg;
	private Location teleLoc;
	private ActionbarChannel ac2 = newActionbarChannel();
	private boolean onetime = true;
	private Set<Player> damagecounter = new HashSet<>();
	private boolean forced = false;
	
	@Override
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	if (onetime) {
		    	fossil.start();
		    	onetime = false;
	    	}
	    	if (getPlayer().getLocation().getY() > 4) teleLoc = getPlayer().getLocation().clone().add(0, -3, 0);
	    	else teleLoc = getPlayer().getLocation();
	    } else if (update == Update.RESTRICTION_SET) {
	    	egg.teleport(egg.getLocation().add(10000, 10000, 10000));
	    } else if (update == Update.ABILITY_DESTROY) {
	    	NMS.setAbsorptionHearts(getPlayer(), 0);
	    }
	} 
	
	private AbilityTimer fossil = new AbilityTimer(LONGEST_WAIT.getValue() * 20) {
		
		@Override
		public void onStart() {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getParticipant().attributes().TARGETABLE.setValue(false);
			ItemStack eggcreate;
			if (ServerVersion.getVersion() >= 13) eggcreate = new ItemStack(Material.ZOMBIE_VILLAGER_SPAWN_EGG);
			else {
				eggcreate = new ItemStack(Material.getMaterial("MONSTER_EGG"));
				SpawnEggMeta meta = (SpawnEggMeta) eggcreate.getItemMeta();
				meta.setSpawnedType(EntityType.ZOMBIE_VILLAGER);
				eggcreate.setItemMeta(meta);
			}
			egg = getPlayer().getWorld().dropItem(getPlayer().getEyeLocation(), eggcreate);
			egg.setCustomName("§2공룡 알");
			egg.setCustomNameVisible(true);
			if (getPlayer().getLocation().getY() > 4) teleLoc = getPlayer().getLocation().clone().add(0, -2, 0);
		}
		
		@Override
		public void run(int count) {
			egg.teleport(teleLoc);
			getPlayer().setGameMode(GameMode.SPECTATOR);
			ac.update("§3유령 상태§7: §e" + (count / 20) + "§f초");
			if (getPlayer().getSpectatorTarget() != null) stop(false);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().teleport(LocationUtil.floorY(egg.getLocation()));
			getPlayer().setGameMode(GameMode.SURVIVAL);
			getParticipant().attributes().TARGETABLE.setValue(true);
			absortionUp.start();
			ac.update(null);
			egg.remove();
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer absortionUp = new AbilityTimer() {
		
		private float goalHeart = 0;
		
		@Override
		public void onStart() {
			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			float yellowheart = NMS.getAbsorptionHearts(getPlayer());
			goalHeart = (float) ((yellowheart + (maxHealth * (absortion * 0.01))) * (forced ? 0.5 : 1));
		}
		
		@Override
		public void run(int count) {
			float yellowheart = NMS.getAbsorptionHearts(getPlayer());
			if (yellowheart < goalHeart) {
				NMS.setAbsorptionHearts(getPlayer(), (float) (yellowheart + 1));
			} else stop(false);
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer absortionAdd = new AbilityTimer(2) {
		
		@Override
		public void run(int count) {
			float yellowheart = NMS.getAbsorptionHearts(getPlayer());
			NMS.setAbsorptionHearts(getPlayer(), (float) (yellowheart + 1));
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 30).register();
	
	@SubscribeEvent
	public void onEntityPickup(EntityPickupItemEvent e) {
		if (egg != null) {
			if (e.getItem().equals(egg)) {
				forced = true;
				fossil.stop(false);
				e.setCancelled(true);
			}
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !cooldown.isCooldown() && !fossil.isRunning()) {
			if (!skill.isRunning()) {
				if (skillstack > 1) {
					getPlayer().sendMessage("§2[§6!§2] §c아직 능력이 지속 중입니다.");
				} else return skill.start();	
			} else getPlayer().sendMessage("§2[§6!§2] §c아직 능력이 지속 중입니다.");
		}
		return false;
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && nofall) {
			if (e.getCause().equals(DamageCause.FALL)) {
				e.setCancelled(true);
				nofall = false;
			}
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer() {

		@Override
		public void onStart() {
			this.setCount(0);
			ac2.update("§2남은 돌진§7: §6" + (skillcount - skillstack));
			skillstack++;
			nofall = true;
			getPlayer().setVelocity(new Vector(0, 1.5, 0));
		}

		@Override
		public void run(int count) {
			if (count >= 7) {
				if (count == 7) getPlayer().setVelocity(new Vector(0, 0, 0));
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(4).setY(-4));
				if (getPlayer().isOnGround()) {
					getPlayer().setVelocity(new Vector(0, 0, 0));
					new Shockwave(getPlayer().getLocation()).start();
					stop(false);
				}
			}
		}

		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (skillstack < skillcount) {
				new AbilityTimer(3) {
					
					@Override
					public void onEnd() {
						onSilentEnd();
					}
					
					@Override
					public void onSilentEnd() {
						skill.start();
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
			} else {
				ac2.update(null);
				skillstack = 0;
				cooldown.start();
				absortionAdd.start();
				absortionAdd.setCount(absortionAdd.getCount() + (damagecounter.size() * 3));
				damagecounter.clear();
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class Shockwave extends AbilityTimer {
		
		private Location center;
		private double currentRadius;
		
		public Shockwave(Location location) {
			super(12);
			setPeriod(TimeUnit.TICKS, 1);
			this.center = location;
		}
		
		@Override
		public void onStart() {
			currentRadius = 0;
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(LocationUtil.floorY(center));
			ParticleLib.EXPLOSION_HUGE.spawnParticle(LocationUtil.floorY(center));
			for (Damageable d : LocationUtil.getNearbyEntities(Damageable.class, center, 5, 5, predicate)) {
				if (d instanceof Player) {
					Player p = (Player) d;
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(p);
					damagecounter.add(p);
				}
				d.damage(skilldamage, getPlayer());
				d.setVelocity(center.toVector().subtract(d.getLocation().toVector()).multiply(-0.5).setY(0.8));
			}
		}
		
		@Override
		public void run(int count) {
			double playerY = getPlayer().getLocation().getY();
			if (currentRadius < 3) currentRadius += 0.25;
			for (Iterator<Location> iterator = Circle.iteratorOf(center, currentRadius, (int) Math.min(currentRadius * 10, 50)); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + 0.1);
				ParticleLib.REDSTONE.spawnParticle(loc, RGB.RED);
				ParticleLib.BLOCK_CRACK.spawnParticle(loc, 0, 0, 0, 1, 10, loc.clone().add(0, -1, 0).getBlock());
			}
		}
		
		@Override
		public void onEnd() {
			if (ServerVersion.getVersion() >= 13) {
				for (Block block : LocationUtil.getBlocks2D(center, 3, true, true, true)) {
					if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
					if (block.getType() == Material.AIR) continue;
					Location location = block.getLocation().add(0, 1, 0);
					FallingBlocks.spawnFallingBlock(location, block.getType(), false, center.toVector().subtract(location.toVector()).multiply(-0.1).setY(Math.random()), Behavior.FALSE);
				}
			} else {
				for (Block block : LocationUtil.getBlocks2D(center, 3, true, true, true)) {
					if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
					if (block.getType() == Material.AIR) continue;
					Location location = block.getLocation().add(0, 1, 0);
					FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, center.toVector().subtract(location.toVector()).multiply(-0.1).setY(Math.random()), Behavior.FALSE);
				}
			}
		}
		
	}
	
}