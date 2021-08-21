package RainStarAbility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "���߷�", rank = Rank.C, species = Species.OTHERS, explain = {
		"�ڽ��� �߻��ϴ� ��� �߻�ü�� 1ƽ �� ��b������f�մϴ�.",
		"������ �߻�ü�� ȭ���� �� �� �̿��� ���簡 ������ ������ �����˴ϴ�.",
		"��7ö�� ��Ŭ�� �á�f, ������ ��� �߻�ü�� ���� �����մϴ�.",
		"��7ö���� ��� ��ũ�� ä �١�f�� �������� �ɸ��� �ð��� ���� �����մϴ�.",
		"�� �߻�ü�� ���� ���� �⺻ ���� �ð��� ��c�ʱ�ȭ��f�˴ϴ�."
		})

@Tips(tip = {
        "�߻�ü�� ����� �� �ִٴ� ���� �� �츮�ž� �մϴ�. �̸��׸� ���� ���ָ�",
        "Ȱ���Ͽ� ���߿� ����ΰ� ���ϴ� ���� �ڷ���Ʈ�� �����ϰ�,",
        "�����̳� ȭ�� ���� ����ξ� ����� �ѹ��� ������ �����մϴ�.",
        "�ٸ� ȭ���� ����� ���ư��� ���ϱ� ������ �⺻ ȭ�캸�� ����� ��ƽ��ϴ�."
}, strong = {
        @Description(subject = "Ʈ��", explain = {
                "�ɷ��� �𸣴� ��븦 �̸� �غ��� �� ȭ�� �� ������",
                "����� Ʈ���� �̿��Ͽ� ������ ������ �� �ֽ��ϴ�."
        })
}, weak = {
        @Description(subject = "���Ÿ���", explain = {
                "ȭ���� �ָ� ���ư��� ����ٴ� �� ������ ���Ÿ� ���� ����մϴ�.",
                "�ǵ����̸� ��븦 ���� �Ѿƿ��� ������ �����ϼ���."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.HARD)

public class AntiGravity extends AbilityBase implements ActiveHandler {
	
	public AntiGravity(Participant participant) {
		super(participant);
	}
	
	ActionbarChannel ac = newActionbarChannel();
	
	private final Map<Projectile, AntiGravitied> antigravityMap = new HashMap<>();
	private boolean arrows = true;
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
		if (update == Update.RESTRICTION_CLEAR && arrows == true) {		
			ac.update("��b���� �ð� ��f: " + timer + "ƽ ��");
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
					ac.update("��b���� �ð� ��f: " + timer + "ƽ ��");
					break;
				case DOWN:
					timer = limit(timer - 1, 5, 1);
					ac.update("��b���� �ð� ��f: " + timer + "ƽ ��");
					break;
				default:
					break;
			}
			NMS.sendTitle(getPlayer(), state == State.UP ? "��c��" : "��9��", String.valueOf(timer), 0, 20, 0);
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