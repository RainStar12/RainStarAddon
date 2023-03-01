package rainstar.abilitywar.ability;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "일격필살", rank = Rank.A, species = Species.HUMAN, explain = {
		"적마다 가하는 첫 §3근접 §c치명타 피해§f가 §c$[MULTIPLY]%§f의 대미지를 입힙니다."
		},
		summarize = {
		"적마다 가하는 첫 §3근접 §c치명타 피해§f가 §c$[MULTIPLY]%§f의 대미지를 입힙니다."		
		})

public class OneShotOneKill extends AbilityBase {

	public OneShotOneKill(Participant participant) {
		super(participant);
	}
	
	private Set<Player> attacked = new HashSet<>();
	private double multiply = MULTIPLY.getValue() * 0.01;
	private boolean attackCooldown = false;
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	attackCooldownChecker.start();
	    }
	}
	
	public static final SettingObject<Integer> MULTIPLY = abilitySettings.new SettingObject<Integer>(OneShotOneKill.class,
			"multiply", 275, "# 추가 대미지 배율") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final AbilityTimer attackCooldownChecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && !e.isCancelled()) {
			if (e.getEntity() instanceof Player) {
				if (isCriticalHit(getPlayer(), attackCooldown) && !attacked.contains(e.getEntity())) {
					Player player = (Player) e.getEntity();
					new AbilityTimer(5) {
						
						@Override
						public void run(int count) {
							SoundLib.ENTITY_PLAYER_ATTACK_STRONG.playSound(getPlayer(), 1, 1.15f);
						}
						
					}.setPeriod(TimeUnit.TICKS, 1).start();
					SoundLib.ENTITY_POLAR_BEAR_WARNING.playSound(getPlayer().getLocation());
					ParticleLib.CRIT.spawnParticle(player.getLocation().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
					ParticleLib.ITEM_CRACK.spawnParticle(player.getEyeLocation(), .3f, .3f, .3f, 100, 0.5, MaterialX.REDSTONE);
					e.setDamage(e.getDamage() * multiply);	
					attacked.add(player);
				}
			}
		}
	}
	
}