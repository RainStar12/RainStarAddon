package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 연쇄살인범", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"살인자의 검으로 상대를 죽일 경우 5초간 투명 효과를 받습니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c욕망§8]§f 살인할 때마다 영구적으로 이동 속도가 10%씩 증가합니다.",
		"§8[§c쾌락§8]§f 검 F키로 직선 방향으로 대시하면서 1초간 무적 상태가 됩니다.",
		"2회 사용할 수 있으나 살인할 때마다 사용 횟수가 1회 추가됩니다."
})
public class SerialMurderer extends AbstractMurderer {
	
	public SerialMurderer(Participant participant) {
		super(participant);
	}
	
	private AttributeModifier modifier;
	private final ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private int killstack = 0;
	private int skillstack = 2;
	private PotionEffect invisible = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, false);
	private static final Vector zerov = new Vector(0, 0, 0);
	private Location startLocation;
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5연쇄살인범", "§f모든 §a시민§f과 §5탐정§f을 연쇄적으로 죽이세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			ac3.update("§3돌진§7: §b" + skillstack + "§f회");
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private final AbilityTimer dashing = new AbilityTimer(1) {
		
		@Override
		public void onStart() {
			if (invincible.isRunning()) invincible.setCount(20);
			else {
				invincible.start();
			}
	    	getPlayer().addPotionEffect(invisible);
	    	getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(10).setY(0)));
			getParticipant().attributes().TARGETABLE.setValue(false);
	   	}
	 
	   	@Override
		public void run(int count) {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.5f);
	   	}
	   	
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
			getPlayer().setVelocity(zerov);
	   		SoundLib.ENTITY_FIREWORK_ROCKET_BLAST.playSound(getPlayer().getLocation());
			getParticipant().attributes().TARGETABLE.setValue(true);
			if (!startLocation.equals(getPlayer().getLocation())) {
    	   		for (Location loc : Line.between(startLocation, getPlayer().getLocation(), (int) Math.min(300, (25 * Math.sqrt(startLocation.distance(getPlayer().getLocation()))))).toLocations(startLocation)) {
    	   			ParticleLib.END_ROD.spawnParticle(loc.add(0, 1, 0), 0, 0, 0, 1, 0);
    	   		}	
    		}
	   	}
	    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer invincible = new AbilityTimer(20) {
		
		@Override
		public void run(int count) {
			ac1.update("§3무적§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac1.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void onStart() {
			modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", killstack * 0.1, Operation.ADD_SCALAR);
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		public void run(int count) {
			ac2.update("§b이동 속도 증가 §e+" + (killstack * 10) + "§f%");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac2.update(null);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}

		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent(priority = 0, onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isMurdererSword(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (!dashing.isRunning()) {
    			if (skillstack > 0) {
					skillstack--;
					startLocation = getPlayer().getLocation().clone();
					ac3.update("§3돌진§7: §b" + skillstack + "§f회");
	    			dashing.start();
				} else getPlayer().sendMessage("§4[§c!§4] §c스킬 사용 가능 횟수를 전부 소모했습니다.");
    		}
    		e.setCancelled(true);
    	}
    }
	
	@SubscribeEvent
	private void onArrowKilled(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && invincible.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().equals(getParticipant()) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) {
			if (passive.isRunning()) passive.stop(false);
			killstack++;
			skillstack += 1;
			ac3.update("§3대시§7: §b" + skillstack + "§f회");
			passive.start();
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled()) {
			if (e.getMurderer().getParticipant().equals(getParticipant())) {
				if (passive.isRunning()) passive.stop(false);
				killstack++;
				skillstack += 1;
				ac3.update("§3대시§7: §b" + skillstack + "§f회");
				passive.start();
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

}
