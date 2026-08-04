package rainstar.abilitywar.game.murder.innocent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;

import java.text.DecimalFormat;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 수집가", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§e수집§8]§f 자신의 주변에 있는 §e금§f을 자신에게 끌어당깁니다.",
		"§8[§b열광§8]§f §e금§f을 주울 때마다 2초간 §b신속 2§f를 받습니다.",
		"§8[§c탐욕§8]§f 가진 §e금§f의 수에 비례하여 §8[§e수집§8]§f의 자력을 강화해 더 넓고,",
		"더 빠르게 §e금§f들을 끌어옵니다. 증가치는 50개 이상일 때 기하급수적으로 증가하며,",
		"만일 §e금§f을 64개 소지하게 되는 순간 모든 §e금§f을 떨어뜨리고 10초간 §e§n기절§f합니다.",
		"§8[§3컬렉션 대방출§8]§f 활을 쏘면 모든 화살을 사용해 추가화살 수 비례로",
		"화살이 추가로 흩뿌려져서 발사§8(§7산탄§8)§f됩니다."
})
public class Hoarder extends AbstractInnocent {
	
	public Hoarder(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a수집가", "§f세상 모든 금을 독식하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
		}
	}
	
	private double range = 0.3;
	private double speed = 0.005;
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (!(entity instanceof Item)) return false;
			if (((Item) entity).getItemStack().isSimilar(Items.GOLD.getStack())) return true;
			return false;
		}
	};
	
	private final AbilityTimer boomcool = new AbilityTimer(400) {
		
		@Override
		public void run(int count) {
			ac2.update("§c금 폭발 방지§7: §e" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		protected void run(int count) {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			int goldcount = murderMystery.getGold(getParticipant());
			if (goldcount >= 64) {
				if (!boomcool.isRunning()) {
					boomcool.start();
					murderMystery.consumeGold(getParticipant(), goldcount);
					Stun.apply(getParticipant(), TimeUnit.SECONDS, 10);
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 1);
					new AbilityTimer(64) {
						
						@Override
	                    protected void run(int count) {
							final Item item = getPlayer().getWorld().dropItem(getPlayer().getLocation().clone().add(0, 1, 0), Items.GOLD.getStack());
							item.setPickupDelay(30);
			                item.setVelocity(new Vector(((random.nextDouble() * 2) - 1) / 1.25, .5, ((random.nextDouble() * 2) - 1) / 1.25));	
	                    }
						
					}.setPeriod(TimeUnit.TICKS, 3).start();	
				}
			} else if (goldcount > 50) {
				int overgold = goldcount - 50;
				range = 15 + (overgold * overgold * 0.4);
				speed = 1 + (overgold * overgold * 0.001);
			} else {
				range = goldcount * 0.3;
				speed = goldcount * 0.005;
			}
			
			for (Item gold : LocationUtil.getNearbyEntities(Item.class, getPlayer().getLocation(), range, range, predicate)) {
				gold.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().toVector().subtract(gold.getLocation().toVector()).normalize().multiply(speed)));
			}
			
			ac.update("§a범위§7: §e" + df.format(range) + "§f칸 §8/ §b속도§7: §e" + df.format(speed));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onPickUp(EntityPickupItemEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			MurderMystery murderMystery = (MurderMystery) getGame();
			if (murderMystery.getGold(getParticipant()) < 64 && e.getItem().getItemStack().isSimilar(Items.GOLD.getStack())) {
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 40, 1, false, false, true);
			}
		}	
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			double multifly = 0;
			int shootarrow = 0;
			if (getArrowCount() < 5) multifly = 1;
			else if (getArrowCount() < 15) multifly = 1.5;
			else if (getArrowCount() < 25) multifly = 2;
			else if (getArrowCount() < 40) multifly = 2.5;
			else if (getArrowCount() >= 40) multifly = 3;
			shootarrow = (int) (getArrowCount() * multifly);
			for (int i = 0; i <= shootarrow; i++) {
				Arrow arrow = e.getEntity().getWorld().spawn(e.getEntity().getLocation(), Arrow.class);
				arrow.setShooter(getPlayer());
				Vector x = VectorUtil.validateVector(VectorUtil.rotateAroundAxisX(e.getEntity().getVelocity(), (random.nextDouble() * 20) - 10));
				Vector y = VectorUtil.validateVector(VectorUtil.rotateAroundAxisY(x, (random.nextDouble() * 20) - 10));
				Vector z = VectorUtil.validateVector(VectorUtil.rotateAroundAxisZ(y, (random.nextDouble() * 20) - 10));
				arrow.setVelocity(VectorUtil.validateVector(z.normalize().multiply(e.getEntity().getVelocity().length())));
			}
			getPlayer().getInventory().remove(Material.ARROW);
		}
	}

}
