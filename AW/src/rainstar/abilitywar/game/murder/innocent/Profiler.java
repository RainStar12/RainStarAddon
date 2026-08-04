package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 프로파일러", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b직감§8]§f 전직 후 §5머더§f의 첫 살인 대상이 본인이라면 1.5초간 무적 및",
		"활과 화살을 1개 지급받습니다. 효과는 §a프로파일러§f만 남았을 땐 발동하지 않으며,",
		"첫 살인 대상이 본인이 아니라면 무작위 시민팀 두 명을 직업까지 알아냅니다.",
		"§8[§c조사§8]§f §5머더§f가 살인하고 나서 5초간의 움직인 §c흔적§f을 볼 수 있습니다.",
		"§8[§3분석§8]§f §5머더§f가 살인하고 나면 대상 §5머더§f로부터 3초간 공격받지 않습니다."
})
public class Profiler extends AbstractInnocent {
	
	public Profiler(Participant participant) {
		super(participant);
	}
	
	private Map<Player, LogParticle> logMap = new HashMap<>();
	private Map<Player, MurderedTimer> timerMap = new HashMap<>();
	private boolean firstblood = true;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a프로파일러", "§f모든 §c머더§f의 흔적을 밝혀내세요.", 10, 80, 10);
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
	
	@SubscribeEvent(priority = 0)
	private void onMurderInv(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && inv.isRunning()) {
			e.setCancelled(true);
			SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, 1.25f);
		}
	}
	
	@SubscribeEvent(priority = Integer.MAX_VALUE, eventPriority = EventPriority.MONITOR)
	private void onTrueMurder(MurderEvent e) {
		if (!e.isCancelled()) {
			new MurderedTimer(e.getMurderer().getPlayer()).start();
			new LogParticle(e.getMurderer().getPlayer()).start();
		}
		
	}
	
	@SubscribeEvent(priority = Integer.MAX_VALUE, eventPriority = EventPriority.MONITOR)
	private void onTrueArrowKill(ArrowKillEvent e) {
		if (e.getShooter().getAbility() instanceof AbstractMurderer && !e.isCancelled()) {
			new MurderedTimer(e.getShooter().getPlayer()).start();
			new LogParticle(e.getShooter().getPlayer()).start();
		}
	}
	
	@SubscribeEvent(priority = 6)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled()) {
			if (e.getTarget().equals(getParticipant())) {
				if (firstblood) {
					boolean onlyprofiler = true;
					for (Participant participant : getGame().getParticipants()) {
						if (participant.hasAbility() && participant.getAbility() instanceof AbstractInnocent && !participant.getAbility().getClass().equals(Profiler.class)) {
							onlyprofiler = false;
							break;
						}
					}
					if (!onlyprofiler) {
						firstblood = false;
						inv.start();
						getPlayer().sendMessage("§8[§b직감§8] §b자신이 첫 살인 대상이라는 것을 직감적으로 알아냈습니다.");
						e.setCancelled(true);
					}
				} else {
					if (timerMap.containsKey(e.getMurderer().getPlayer())) {
						e.setCancelled(true);
						SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, 1.25f);
					}
				}
			} else {
				if (firstblood) {
					firstblood = false;
					List<Participant> innocents = new ArrayList<>(getGame().getParticipants());
					for (Participant participant : getGame().getParticipants()) {
						if (getParticipant().equals(participant) || participant.getAbility() instanceof AbstractMurderer || ((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) innocents.remove(participant);
					}
					if (innocents.size() > 1) {
						Collections.shuffle(innocents);
						getPlayer().sendMessage("§8[§b직감§8] §c직감적으로, §a" + innocents.get(0).getPlayer().getName() + "§c님, §a" + innocents.get(1).getPlayer().getName() + "§c님은 무고한 시민이 맞다는 걸 깨닫습니다.");
						getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(0).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(0).getAbility().getDisplayName() + "§f입니다.");
						getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(1).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(1).getAbility().getDisplayName() + "§f입니다.");
					} else if (innocents.size() > 0) {
						Collections.shuffle(innocents);
						getPlayer().sendMessage("§8[§b직감§8] §c직감적으로, §a" + innocents.get(0).getPlayer().getName() + "§c님은 무고한 시민이 맞다는 걸 깨닫습니다.");
						getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(0).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(0).getAbility().getDisplayName() + "§f입니다.");
						getPlayer().sendMessage("§8[§b직감§8] §c그리고 이외에 무고한 시민이 없습니다.");
					} else getPlayer().sendMessage("§8[§b직감§8] §c더는 무고한 시민이 없습니다.");
				}
			}
		}
	}
	
	@SubscribeEvent
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().getAbility() instanceof AbstractMurderer) {
			if (e.getTarget().equals(getParticipant()) && inv.isRunning()) {
				e.setCancelled(true);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, 1.25f);
			} else if (!e.isCancelled()) {
				if (e.getTarget().equals(getParticipant())) {
					if (firstblood) {
						boolean onlyprofiler = true;
						for (Participant participant : getGame().getParticipants()) {
							if (participant.hasAbility() && participant.getAbility() instanceof AbstractInnocent && !participant.getAbility().getClass().equals(Profiler.class)) {
								onlyprofiler = false;
								break;
							}
						}
						if (!onlyprofiler) {
							firstblood = false;
							inv.start();
							getPlayer().sendMessage("§8[§b직감§8] §b자신이 첫 살인 대상이라는 것을 직감적으로 알아냈습니다.");
							e.setCancelled(true);
						}
					} else {
						if (timerMap.containsKey(e.getShooter().getPlayer())) {
							e.setCancelled(true);
							SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, 1.25f);
						}
					}
				} else {
					if (firstblood) {
						firstblood = false;
						List<Participant> innocents = new ArrayList<>(getGame().getParticipants());
						for (Participant participant : getGame().getParticipants()) {
							if (getParticipant().equals(participant) || participant.getAbility() instanceof AbstractMurderer || ((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) innocents.remove(participant);
						}
						if (innocents.size() > 1) {
							Collections.shuffle(innocents);
							getPlayer().sendMessage("§8[§b직감§8] §c직감적으로, §a" + innocents.get(0).getPlayer().getName() + "§c님, §a" + innocents.get(1).getPlayer().getName() + "§c님은 무고한 시민이 맞다는 걸 깨닫습니다.");
							getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(0).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(0).getAbility().getDisplayName() + "§f입니다.");
							getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(1).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(1).getAbility().getDisplayName() + "§f입니다.");
						} else if (innocents.size() > 0) {
							Collections.shuffle(innocents);
							getPlayer().sendMessage("§8[§b직감§8] §c직감적으로, §a" + innocents.get(0).getPlayer().getName() + "§c님은 무고한 시민이 맞다는 걸 깨닫습니다.");
							getPlayer().sendMessage("§8[§b직감§8] §a" + innocents.get(0).getPlayer().getName() + "§c님의 직업은 §a" + innocents.get(0).getAbility().getDisplayName() + "§f입니다.");
							getPlayer().sendMessage("§8[§b직감§8] §c그리고 이외에 무고한 시민이 없습니다.");
						} else getPlayer().sendMessage("§8[§b직감§8] §c더는 무고한 시민이 없습니다.");
					}
				}
			}
		}
	}
	
	public AbilityTimer inv = new AbilityTimer(30) {
		
		@Override
		public void onStart() {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			if (!addArrow()) {
				murderMystery.addGold(getParticipant());
			} else {
				if (!hasBow()) {
					getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
				}
			}
		}
		
		@Override
		public void run(int count) {
			ac.update("§3무적§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	
	private class MurderedTimer extends AbilityTimer {
		
		private final Player player;
		
		private MurderedTimer(Player player) {
			super(TaskType.REVERSE, 60);
			setPeriod(TimeUnit.TICKS, 1);
			timerMap.put(player, this);
			this.player = player;
		}
		
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			timerMap.remove(player);
		}
		
	}
	
	private class LogParticle extends AbilityTimer {
		
		private List<Location> locations = new ArrayList<>();
		private final Player player;
		private Random random = new Random();
		private final RGB color;
		private int timer = 0;
		
		private LogParticle(Player player) {
			super(TaskType.NORMAL, 1000);
			setPeriod(TimeUnit.TICKS, 2);
			this.player = player;
			logMap.put(player, this);
			color = RGB.of(random.nextInt(254) + 1, random.nextInt(254) + 1, random.nextInt(254) + 1);
		}
		
    	@Override
    	protected void run(int count) {
    		if (timer <= 50) locations.add(player.getLocation());
    		timer++;
    		if (timer % 5 == 0) {
            	for (Location loc : locations) {
                	ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);	
            	}	
    		}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		locations.clear();
    		logMap.remove(player);
    	}

	}
	
}
