package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "머더: 살인청부업자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3청부§8]§f 금 좌클릭으로 금 7개를 소모해 무작위 시민을 30초간 §c목표물§f로 지정해",
		"§e발광§f시킵니다. 목표물이 아닌 무작위 대상 1명도 §e발광§f시킵니다.",
		"§c목표물§f을 처치하기 전까진 아무도 처치할 수 없습니다.",
		"§c목표물§f을 처치에 성공할 경우 화살을 막아주는 §3실드§f 2개§8(§7중첩 가능§8)§f를 얻습니다.",
		"§8[§a의뢰§8]§f §c목표물§f이 있다면, 나침반이 §c목표물§f을 가리킵니다.",
		"또한 §b이동 속도 증가 25%§f를 받습니다."
})
public class HitmanMurderer extends AbstractMurderer {
	
	public HitmanMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5살인청부업자", "§a시민§f을 지정하여 대상을 성공적으로 처리하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}	
	
	private Random random = new Random();
	private Participant target, faketarget;
	private ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.25, Operation.ADD_SCALAR);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
		}
	};
	
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
				if (!skill.isRunning()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					List<Participant> participants = new ArrayList<>();
					for (Participant participant : getGame().getParticipants()) {
						if (!(participant.getAbility() instanceof AbstractMurderer) && !((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
							participants.add(participant);
						}
					}
					if (participants.size() > 0) {
						if (murderMystery.consumeGold(getParticipant(), 7)) {
							target = random.pick(participants);
							List<Participant> allparticipants = new ArrayList<>();
							for (Participant participant : getGame().getParticipants()) {
								if (!participant.equals(target) && !((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
									allparticipants.add(participant);
								}
							}
							faketarget = random.pick(allparticipants);
							getPlayer().sendMessage("§4[§c!§4] §c" + target.getPlayer().getName() + "§f님은 이제 당신의 목표물입니다.");	
							skill.start();
						}
					} else getPlayer().sendMessage("§4[§c!§4] §c지정할 무작위 대상이 없습니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 지정 대상이 있습니다.");
			}
		}
	}
	
	@SubscribeEvent(priority = 7)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled()) {
			if (skill.isRunning() && getParticipant().equals(e.getMurderer().getParticipant())) {
				if (e.getTarget().equals(target)) {
					skill.stop(false);
					Shield.addShield(getParticipant(), 2);
				} else e.setCancelled(true); 
			}	
		}
	}
	
	@SubscribeEvent(priority = 7)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (skill.isRunning() && getParticipant().equals(e.getShooter())) {
				if (e.getTarget().equals(target)) {
					skill.stop(false);
					Shield.addShield(getParticipant(), 2);
				} else e.setCancelled(true); 
			}	
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (target == null) {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
				}
			} else {
				getPlayer().setCompassTarget(target.getPlayer().getLocation());	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skill = new AbilityTimer(600) {
		
		@Override
		public void onStart() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
		}
		
		@Override
		public void run(int count) {
			ac.update("§b목표물§7: §c" + target.getPlayer().getName() + "§7 | §b남은 시간§7: §e" + df.format(count / 20.0) + "§f초");
			PotionEffects.GLOWING.addPotionEffect(target.getPlayer(), 600, 1, true, false, true);
			PotionEffects.GLOWING.addPotionEffect(faketarget.getPlayer(), 600, 1, true, false, true);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			target.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
			faketarget.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
