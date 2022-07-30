package RainStarAbility;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "유리 대포",
		rank = Rank.B, 
		species = Species.OTHERS, 
		explain = {
		"내가 주는 §c추가 대미지§f만큼 내가 피해입을 때 §c추가 피해§f를 입습니다.",
		"철괴를 들고 웅크린 상태로 마우스 휠을 이용하여,",
		"내가 받을 §c추가 피해량§ 및 §c추가 대미지§f를 정할 수 있습니다.",
		"이 수치는 최대 5까지 설정 가능합니다."
		},
		summarize = {
		"웅크린 채 철괴를 들고 마우스 휠을 이용하여",
		"§e1§f~§e5§f의 §c공격력 및 받는 피해 증가§f를 설정할 수 있습니다."
		})

@Tips(tip = {
        "강한 공격력을 가지는 대신 그만큼의 추가 피해를 받을 각오를 해야하는",
        "능력으로, 추가 대미지와 추가 피해량이 동일하기 때문에",
        "본인의 PVP 실력에 큰 영향을 받습니다."
}, strong = {
        @Description(subject = "높은 공격력", explain = {
                "최대 7까지의 높은 공격력을 상대에게 가할 수 있습니다.",
                "일반 다이아몬드 칼 기준, 2배의 대미지를 가하는 셈입니다."
        }),
        @Description(subject = "급습", explain = {
                "급습으로 대상보다 1회의 타격이라도 이득을 보면",
                "높은 대미지 덕에 상대와의 격차가 커집니다."
        })
}, weak = {
        @Description(subject = "낮은 방어력", explain = {
                "대미지를 높이는 대신 그만큼의 대미지를 받을 수 있기에,",
                "그 점을 감안하고 대미지를 잘 조절해서 싸워야 합니다."
        })
}, stats = @Stats(offense = Level.NINE, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.HARD)

public class GlassCannon extends AbilityBase {
	
	public GlassCannon(Participant participant) {
		super (participant);
	}
	
	private int damage = 1;
	private int amount = 10 + (damage * 20);
	private double speed = 1 + (damage * 0.1);
	
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
					damage = limit(damage + 1, 5, 1);
					ac.update("§e대미지 및 피해 증가 §7: §c" + damage);
					break;
				case DOWN:
					damage = limit(damage - 1, 5, 1);
					ac.update("§e대미지 및 피해 증가 §7: §c" + damage);
					break;
				default:
					break;
			}
			NMS.sendTitle(getPlayer(), state == State.UP ? "§c↑" : "§9↓", String.valueOf(damage), 0, 20, 0);
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
					(
							calculate(previousSlot, 1) == newSlot
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

	private final ActionbarChannel ac = newActionbarChannel();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§e대미지 및 피해 증가 §7: §c" + damage);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		
		if (e.getDamager().equals(getPlayer())) {
			e.setDamage(e.getDamage() + damage);
			SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, (float) speed);
			ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 
						2, 0, amount, 1, MaterialX.GLASS);
		}
		
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			e.setDamage(e.getDamage() + damage);
			SoundLib.BLOCK_GLASS_BREAK.playSound((Player) e.getDamager(), 1, (float) speed);
			ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0, 
					2, 0, amount, 1, MaterialX.GLASS);
		}
		
		if (e.getDamager() instanceof Projectile) {
			Projectile arrow = (Projectile) e.getDamager();
			if (getPlayer().equals(arrow.getShooter()) && e.getEntity() instanceof LivingEntity
					&& !e.getEntity().equals(getPlayer())) {
				e.setDamage(e.getDamage() + damage);
				SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, (float) speed);
				ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 
						2, 0, amount, 1, MaterialX.GLASS);
			}
			if (arrow.getShooter() instanceof Player && e.getEntity().equals(getPlayer()) && !getPlayer().equals(arrow.getShooter())) {
				e.setDamage(e.getDamage() + damage);
				SoundLib.BLOCK_GLASS_BREAK.playSound((Player) arrow.getShooter(), 1, (float) speed);
				ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 
						2, 0, amount, 1, MaterialX.GLASS);
			}
		
		}
	
	}
}