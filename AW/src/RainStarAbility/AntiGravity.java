package RainStarAbility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "반중력", rank = Rank.C, species = Species.OTHERS, explain = {
		"§5지급 아이템§7: 화살 $[ARROW]개, 엔더 진주 $[ENDERPEARL]개",
		"자신이 발사하는 모든 발사체가 1틱 후 §b정지§f합니다.",
		"정지된 발사체가 화살일 때 나 이외의 존재가 닿으면 정지가 해제됩니다.",
		"§7철괴 좌클릭 시§f, 정지된 모든 발사체를 정지 해제합니다.",
		"§7철괴를 들고 웅크린 채 휠§f로 정지까지 걸리는 시간을 조절 가능합니다.",
		"내 발사체에 맞은 적은 기본 무적 시간이 §c초기화§f됩니다."
		},
		summarize = {
		"발사하는 모든 투사체를 잠시 후 멈출 수 있습니다.",
		"시간은 웅크린 채 휠로 조절 가능합니다.",
		"철괴 좌클릭으로 모든 투사체의 정지를 해제합니다.",
		"투사체에 맞은 적은 기본 무적 시간이 초기화됩니다."
		})

@Tips(tip = {
        "발사체를 멈춰둘 수 있다는 것을 잘 살리셔야 합니다. 이를테면 엔더 진주를",
        "활용하여 공중에 멈춰두고 원하는 때에 텔레포트가 가능하고,",
        "포션이나 화살 등을 멈춰두어 대상을 한번에 공격이 가능합니다.",
        "다만 화살이 제대로 나아가지 못하기 때문에 기본 화살보다 사용이 어렵습니다."
}, strong = {
        @Description(subject = "트랩", explain = {
                "능력을 모르는 상대를 미리 준비해 둔 화살 및 포션을",
                "멈춰둔 트랩을 이용하여 유인해 공격할 수 있습니다."
        })
}, weak = {
        @Description(subject = "원거리전", explain = {
                "화살이 멀리 나아가기 힘들다는 점 때문에 원거리 전에 취약합니다.",
                "되도록이면 상대를 내게 쫓아오는 쪽으로 유도하세요."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.HARD)

public class AntiGravity extends AbilityBase implements ActiveHandler {
	
	public AntiGravity(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> ARROW = abilitySettings.new SettingObject<Integer>(AntiGravity.class,
			"arrow-amount", 64, "# 화살 추가 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ENDERPEARL = abilitySettings.new SettingObject<Integer>(AntiGravity.class,
			"enderpearl-amount", 16, "# 엔더진주 추가 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private ActionbarChannel ac = newActionbarChannel();	
	private final Map<Projectile, AntiGravitied> antigravityMap = new HashMap<>();
	private boolean kit = true;
	private int timer = 1;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
				Player player = (Player) entity;
				if (player.getGameMode().equals(GameMode.SPECTATOR)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && kit == true) {
			
			ItemStack arrow = new ItemStack(Material.ARROW, ARROW.getValue());
			ItemStack enderpearl = new ItemStack(Material.ENDER_PEARL, ENDERPEARL.getValue());
			ItemMeta ameta = arrow.getItemMeta();
			ItemMeta emeta = enderpearl.getItemMeta();
			
			ameta.setDisplayName("§5반중력 실험 키트 §7- §f화살");
			ameta.addEnchant(Enchantment.MENDING, 1, true);
			
			emeta.setDisplayName("§5반중력 실험 키트 §7- §3엔더 진주");
			emeta.addEnchant(Enchantment.MENDING, 1, true);
			
			enderpearl.setItemMeta(emeta);
			arrow.setItemMeta(ameta);
			
			getPlayer().getInventory().addItem(arrow);
			getPlayer().getInventory().addItem(enderpearl);
			kit = false;

			ac.update("§b정지 시간 §f: " + timer + "틱 후");
		}
		
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			for (Entry<Projectile, AntiGravitied> entry : antigravityMap.entrySet()) {
				entry.getValue().stop(false);
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onSlotChange(final PlayerItemHeldEvent e) {
		if (!getPlayer().isSneaking() || e.getPreviousSlot() == e.getNewSlot()) return;
		final PlayerInventory inventory = getPlayer().getInventory();
		final ItemStack previous = inventory.getItem(e.getPreviousSlot());
		if (previous != null && previous.getType() == Material.IRON_INGOT) {
			e.setCancelled(true);
			final State state = getState(e.getPreviousSlot(), e.getNewSlot());
			if (state == State.UNKNOWN) return;
			switch (state) {
				case UP:
					timer = limit(timer + 1, 5, 1);
					ac.update("§b정지 시간 §f: " + timer + "틱 후");
					break;
				case DOWN:
					timer = limit(timer - 1, 5, 1);
					ac.update("§b정지 시간 §f: " + timer + "틱 후");
					break;
				default:
					break;
			}
			NMS.sendTitle(getPlayer(), state == State.UP ? "§c↑" : "§9↓", String.valueOf(timer), 0, 20, 0);
			if (!titleClear.start()) {
				titleClear.setCount(10);
			}
		}
	}

	private int limit(final int value, final int max, final int min) {
		return Math.max(min, Math.min(max, value));
	}

	private State getState(final int previousSlot, final int newSlot) {
		if (previousSlot == 0) {
			return newSlot >= 6 ? State.UP : (newSlot <= 3 ? State.DOWN : State.UNKNOWN);
		} else if (previousSlot == 8) {
			return newSlot <= 2 ? State.DOWN : (newSlot >= 5 ? State.UP : State.UNKNOWN);
		} else {
			return calculate(previousSlot, -1) == newSlot
					|| calculate(previousSlot, -2) == newSlot
					|| calculate(previousSlot, -3) == newSlot ? State.UP :
					(calculate(previousSlot, 1) == newSlot
					|| calculate(previousSlot, 2) == newSlot
					|| calculate(previousSlot, 3) == newSlot ? State.DOWN : State.UNKNOWN
			);
		}
	}

	private int calculate(int slot, int offset) {
		final int value = slot + offset;
		if (value < 0) return 9 + value;
		else if (value > 8) return value - 9;
		else return value;
	}

	private enum State {
		UP, DOWN, UNKNOWN
	}
	
	private final AbilityTimer titleClear = new AbilityTimer(10) {
		@Override
		protected void run(int count) {
		}

		@Override
		protected void onEnd() {
			NMS.clearTitle(getPlayer());
		}

		@Override
		protected void onSilentEnd() {
			NMS.clearTitle(getPlayer());
		}
	}.setPeriod(TimeUnit.TICKS, 4);
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getHitBlock() == null) {
			if (getPlayer().equals(e.getEntity().getShooter())) {
				if (e.getHitEntity() instanceof LivingEntity) {
					LivingEntity l = (LivingEntity) e.getHitEntity();
					l.setNoDamageTicks(0);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			new AbilityTimer(timer) {
					
				@Override
				protected void run(int count) {
				}
					
				@Override
				protected void onEnd() {
					onSilentEnd();
				}

				@Override
				protected void onSilentEnd() {
					new AntiGravitied(e.getEntity()).start();
				}
					
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(AbilityBase.ClickType.LEFT_CLICK)) {
	    	for (Iterator<Entry<Projectile, AntiGravitied>> iterator = antigravityMap.entrySet().iterator(); iterator.hasNext();) {
	    	      final Entry<Projectile, AntiGravitied> entry = iterator.next();
	    	      iterator.remove();
	    	      entry.getValue().stop(false);
	    	}
	    }
	    return false;
	}
	
	class AntiGravitied extends AbilityTimer {
		
		private Projectile projectile;
		private final Vector zerov = new Vector(0, 0, 0);
		private final Map<Projectile, Vector> velocityMap = new HashMap<>();
		
		private AntiGravitied(Projectile projectile) {
			super(TaskType.NORMAL, 9999999);
			setPeriod(TimeUnit.TICKS, 1);
			this.projectile = projectile;
			antigravityMap.put(projectile, this);
		}
		
		@Override
		protected void onStart() {
			velocityMap.put(projectile, projectile.getVelocity());
			projectile.setGravity(false);
			projectile.setVelocity(zerov);
		}
		
		@Override
		protected void run(int count) {
			for (Player entity : getPlayer().getWorld().getPlayers()) {
				if (predicate.test(entity) && NMS.isArrow(projectile)) {
					if (LocationUtil.isConflicting(projectile, entity)) {
						this.stop(false);
					}	
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			projectile.setGravity(true);
			projectile.setVelocity(velocityMap.get(projectile));
			antigravityMap.remove(projectile);
		}
		
	}
	
}