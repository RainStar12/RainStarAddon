package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostChat;

@AbilityManifest(name = "시민: 도굴꾼", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c사흔§8]§f 머더에게 살해당한 사람의 시체 위치가 보이고 안내됩니다.",
		"§8[§6도굴§8]§f 누군가가 사망 위기에 놓이면 대상이 갖고 있던 금과 화살을 획득합니다.",
		"또한, 그 대상의 직업을 알아냅니다.",
		"§8[§3출정§8]§f 금 F키로 가장 마지막으로 남겨진 §8[§c사흔§8]§f으로 즉시 이동합니다. §8(§71회용§8)",
		"이 스킬은 두 번째 §8[§c사흔§8]§f부터 사용할 수 있습니다.",
		"§8[§b유언§8]§f §8[§c사흔§8]§f으로 보여지는 표식 위에서 금 좌클릭 시,",
		"금 20개를 소모하여 대상의 사망 후 첫마디를 들을 수 있습니다."
})		
public class GraveRobber extends AbstractInnocent {
	
	public GraveRobber(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a도굴꾼", "§f그들의 마지막 외침을 들어내세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
	private boolean used = false;
	private boolean first = true;
	private Location lastBlood;
	private Map<Location, Blood> bloodMap = new HashMap<>();
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private GhostChat ghostchat = getGame().getModule(GhostChat.class);
	
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
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (!robbing.isRunning()) {
					if (bloodMap.size() > 0) {
						for (Location location : bloodMap.keySet()) {
							if (getPlayer().getLocation().distanceSquared(location) < 4) {
								if (murderMystery.consumeGold(getParticipant(), 20)) {
									robbing.start();	
								}
							}
						}
					}	
				} else getPlayer().sendMessage("§4[§c!§4] §c아직 도굴 중입니다.");	
			}
		}
	}
	
	public AbilityTimer robbing = new AbilityTimer(40) {
		
		@Override
		protected void run(int count) {
			ac.update("§c도굴§7: §3" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			Set<Location> deleteLocations = new HashSet<>();
			for (Location location : bloodMap.keySet()) {
				if (getPlayer().getLocation().distanceSquared(location) < 4) {
					deleteLocations.add(location);
					new AbilityTimer(TaskType.REVERSE, 5) {
						
						@Override
						public void run(int count) {
							SoundLib.ENTITY_GHAST_SCREAM.playSound(getPlayer(), 1, (float) (2 - (count * 0.15)));
						}
						
					}.setPeriod(TimeUnit.TICKS, 5).start();
					getPlayer().sendMessage("§8[§3유언§8] §7" + bloodMap.get(location).getParticipant().getPlayer().getName() + "§f: " + ghostchat.getFirstGhostChat(bloodMap.get(location).getParticipant()));
				}
			}
			
			for (Location location : deleteLocations) {
				bloodMap.get(location).stop(false);	
			}
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer digging = new AbilityTimer() {

		private Location location;
		private float flySpeed;

		@Override
		protected void onStart() {
			this.location = lastBlood;
			this.flySpeed = getPlayer().getFlySpeed();
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().setGameMode(GameMode.SPECTATOR);
		}

		@Override
		protected void run(int count) {
			if (getPlayer().getGameMode() == GameMode.SPECTATOR) {
				getPlayer().setSpectatorTarget(null);
			}
			getPlayer().setFlySpeed(0f);
			Location playerLocation = getPlayer().getLocation();
			getPlayer().setVelocity(location.toVector().subtract(playerLocation.toVector()).normalize().multiply(3));
			if (playerLocation.distanceSquared(location) < 4.0) {
				stop(false);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
			getPlayer().teleport(location);
		}

		@Override
		protected void onSilentEnd() {
			PotionEffects.BLINDNESS.addPotionEffect(getPlayer(), 5, 1, true, false, true);
			getPlayer().setGameMode(GameMode.ADVENTURE);
			getPlayer().setVelocity(new Vector());
			getPlayer().setFlySpeed(flySpeed);
			getPlayer().setFlying(false);
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer()) && !used) {
    		if (lastBlood != null) {
    			if (!first) {
            		getPlayer().sendMessage("§4[§c!§4] §3출정§f을 사용하였습니다.");
            		used = true;
            		digging.start();		
    			} else getPlayer().sendMessage("§4[§c!§4] §c첫 번째 사흔에서는 사용할 수 없습니다.");
    		} else getPlayer().sendMessage("§4[§c!§4] §c아직 사흔이 없습니다.");
    	}
    }
	
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && robbing.isRunning()) {
			e.setTo(e.getFrom());
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onMurder(final MurderEvent e) {
		MurderMystery murderMystery = (MurderMystery) getGame();
		AbstractJob job = (AbstractJob) e.getTarget().getAbility();
		new Blood(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0), job.getArrowCount(), murderMystery.getGold(e.getTarget()), e.getTarget(), e.getTarget().getAbility().getName()).start();
	}

	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onArrowKill(final ArrowKillEvent e) {
		MurderMystery murderMystery = (MurderMystery) getGame();
		AbstractJob job = (AbstractJob) e.getTarget().getAbility();
		if (e.getShooter().getAbility() instanceof AbstractMurderer) new Blood(e.getTarget().getPlayer().getLocation().add(0, 1, 0), job.getArrowCount(), murderMystery.getGold(e.getTarget()), e.getTarget(), e.getTarget().getAbility().getName()).start();
	}
	
	@SubscribeEvent
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().equals(getPlayer()) && digging.isRunning() && !e.getNewGameMode().equals(GameMode.SPECTATOR)) {
			e.setCancelled(true);
		}
	}
	
	public void getItems(Location location) {
		MurderMystery murderMystery = (MurderMystery) getGame();
		murderMystery.addGold(getParticipant(), bloodMap.get(location).getGold());
		if (bloodMap.get(location).getArrow() > 0) {
			ItemStack stack = getPlayer().getInventory().getItem(3);
			if (stack != null && stack.getType() == Material.ARROW) {
				if (stack.getAmount() < 64) {
					stack.setAmount(stack.getAmount() + bloodMap.get(location).getArrow());
					getPlayer().getInventory().setItem(3, stack);
					getPlayer().sendMessage("§8+ §f" + bloodMap.get(location).getArrow() + " 화살");
				}
			} else {
				getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW, bloodMap.get(location).getArrow()));
				getPlayer().sendMessage("§8+ §f" + bloodMap.get(location).getArrow() + " 화살");
			}
			if (!hasBow()) {
				getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
			}
		}
	}

	private class Blood extends AbilityTimer {

		private final Location location;
		private final int arrowcount, goldcount;
		private final Participant participant;

		private Blood(final Location location, int arrowcount, int goldcount, Participant participant, String jobname) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
			this.arrowcount = arrowcount;
			this.goldcount = goldcount;
			this.participant = participant;
			bloodMap.put(location, this);
			getPlayer().sendMessage("§8[§c사흔§8] §e위치 §5X §f" + df.format(location.getX()) + " §5Y §f" + df.format(location.getY()) + " §5Z §f" + df.format(location.getZ()));
			getPlayer().sendMessage("§8[§c사흔§8] §f그 시신의 직업은 §e" + jobname + " §f이었습니다.");
			if (lastBlood != null && first) first = false;
			lastBlood = location;
			getItems(location);
		}

		@Override
		protected void run(int count) {
			ParticleLib.SMOKE_LARGE.spawnParticle(getPlayer(), location, 0, 0, 0, 1, 0);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			bloodMap.remove(location);
		}
		
		public int getArrow() {
			return arrowcount;
		}
		
		public int getGold() {
			return goldcount;
		}
		
		public Participant getParticipant() {
			return participant;
		}

	}
	
}
