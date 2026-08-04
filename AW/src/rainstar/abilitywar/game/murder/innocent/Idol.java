package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

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
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

@AbilityManifest(name = "시민: 아이돌", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c리허설§8]§f 머더가 검을 얻고 15초가 지나기 전까지 §3[§b아이돌의 선언§3]§f을 할 수 없습니다.",
		"§3[§b아이돌의 선언§3]§f 금 좌클릭으로 금 §b39§f개를 소모해 머더들의 §b이름§f",
		"한 글자씩을 알아냅니다. 단, 전부 소문자로 보여지고, _는 포함하지 않습니다.",
		"사용 후 §2[§a라이브 개시!§2]§f의 §c쿨타임§f이 39초 줄어듭니다.",
		"§2[§a라이브 개시!§2]§f 금 F키로 금 6개를 소모해, 28초간 §b무적§f이 됩니다.",
		"지속 동안 활을 들 수 없고 §b신속 1§f을 받으며, 10칸 내 사람들에게서",
		"매 초마다 §e금§f을 하나씩 걷어옵니다. §c쿨타임 §7: §f93초"
})		
public class Idol extends AbstractInnocent {
	
	public Idol(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a아이돌", "§f공연을 성공적으로 끝마치세요!", 10, 80, 10);
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
	
	private final Map<Participant, Integer> murderOrders = new HashMap<>();
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private Cooldown cooldown = new Cooldown(93);
	private final Circle circle = Circle.of(10, 100);
	private CheckMurdererSword checkmurderersword = getGame().getModule(CheckMurdererSword.class);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (getPlayer().equals(entity)) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	@SubscribeEvent(priority = 1)
	private void onMurder(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
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
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (checkmurderersword.isCheckable()) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 39)) {
						List<String> namelist = new ArrayList<>();
						for (Participant participant : getGame().getParticipants()) {
							if (participant.getAbility() instanceof AbstractMurderer) {
								char namechar;
								while(true) {
									namechar = participant.getPlayer().getName().charAt(random.nextInt(participant.getPlayer().getName().length()));
									if (namechar != '_') break;
								}
								if (!murderOrders.containsKey(participant)) murderOrders.put(participant, murderOrders.size() + 1);
								namelist.add("§c머더 " + murderOrders.get(participant) + "§7: §5" + Character.toLowerCase(namechar));
							}
						}
						
						for (String names : namelist) {
							getPlayer().sendMessage(names);
						}
						
						if (cooldown.isRunning()) cooldown.setCount(Math.max(cooldown.getCount() - 39, 0));
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c아직 선언할 수 없습니다.");
			}
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (!cooldown.isCooldown()) {
    			if (!skill.isRunning()) {
    				final MurderMystery murderMystery = (MurderMystery) getGame();
    				if (murderMystery.consumeGold(getParticipant(), 6)) {
    					getPlayer().sendMessage("§4[§c!§4] §a라이브 개시!§f를 사용하였습니다.");
        				song.start();
        				skill.start();	
    				}
    			}
    		}
    	}
    }
    
	public AbilityTimer skill = new AbilityTimer(560) {
		
		@Override
		public void run(int count) {
			getPlayer().getInventory().setHeldItemSlot(0);
			ac.update("§a라이브 모드§7: §d" + df.format(count / 20.0) + "§e초");
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 20, 0, false, false, true);
			ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 10, 10, 10, 2, 0);	
			if (count % 10 == 0) {
				for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.TOTEM.spawnParticle(locs, 0, 0, 0, 1, 0.08f);
				}	
			}
			if (count % 20 == 0) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
					if (murderMystery.consumeGold(getGame().getParticipant(player), 1)) murderMystery.addGold(getParticipant(), 1);
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			cooldown.start();
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer song = new AbilityTimer(TaskType.NORMAL, 562) {
		
		private final Note As = new Note(16), Gs = new Note(14), C = new Note(18), Dslow = new Note(9),
				F = new Note(11), Cslow = new Note(7), Clow = new Note(6), Dlow = new Note(8),
				Cs = new Note(19), Ds = new Note(21), G = new Note(13);
		
		private void play(Note note) {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 3.9f, note);
		}
		
		@Override
		public void run(int count) {
			switch(count) {
			case 184:
			case 428:
			case 464:
			case 468:
				play(Clow);
				break;
			case 180:
			case 188:
			case 424:
			case 432:
			case 472:
			case 476:
				play(Cslow);
				break;
			case 192:
				play(Dlow);
				break;
			case 148:
			case 156:
			case 164:
			case 168:
			case 172:
			case 176:
			case 416:
			case 420:
			case 460:
				play(Dslow);
				break;
			case 152:
			case 200:
			case 216:
			case 220:
			case 408:
			case 456:
				play(F);
				break;
			case 404:
			case 452:
				play(G);
				break;
			case 8:
			case 16:
			case 28:
			case 32:
			case 40:
			case 52:
			case 60:
			case 76:
			case 92:
			case 116:
			case 136:
			case 140:
			case 144:
			case 208:
			case 248:
			case 260:
			case 268:
			case 280:
			case 284:
			case 292:
			case 312:
			case 320:
			case 324:
			case 332:
			case 336:
			case 384:
			case 392:
			case 400:
			case 440:
			case 444:
			case 500:
			case 516:
			case 520:
			case 532:
			case 544:
			case 556:
			case 560:
				play(Gs);
				break;
			case 4:
			case 12:
			case 36:
			case 68:
			case 84:
			case 120:
			case 244:
			case 256:
			case 264:
			case 288:
			case 328:
			case 352:
			case 380:
			case 484:
			case 488:
			case 552:
				play(As);
				break;
			case 20:
			case 44:
			case 124:
			case 224:
			case 240:
			case 272:
			case 296:
			case 360:
			case 376:
				play(C);
				break;
			case 232:
			case 368:
				play(Cs);
				break;
			case 304:
			case 504:
			case 536:
				play(Ds);
				break;
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
