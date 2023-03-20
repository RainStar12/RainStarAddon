package rainstar.abilitywar.ability;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "꼬마", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a보호본능§f: 자신을 공격한 적에게 §b망설임§f을 1 부여합니다.",
		" §b망설임§f × $[CHANCE]%의 확률로 대상은 시야가 뒤틀리고, §b망설임§f이 초기화됩니다.",
		"§7철괴 우클릭 §8- §c방범벨§f: 적들을 §b망설임§f × $[STUN]초간 §e§n기절§f시킵니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §6Ddang_67"
		},
		summarize = {
		"자신을 공격한 적을 확률적으로 시야를 돌립니다.",
		"§7철괴 우클릭 시§f 공격한 적들 중 확률 실패 대상을 비례 §e§n기절§f시킵니다."
		})
public class Kid extends AbilityBase implements ActiveHandler {
	
	public Kid(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE = 
			abilitySettings.new SettingObject<Integer>(Kid.class, "chance", 20,
			"# 망설임 당 시야가 뒤틀릴 확률") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> STUN = 
			abilitySettings.new SettingObject<Double>(Kid.class, "stun-duration", 1.0,
			"# 기절 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Kid.class, "cooldown", 60,
			"# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
		
	};
	
	private final int chanceper = CHANCE.getValue();
	private final int stunper = (int) (STUN.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private Map<Participant, Stack> stackMap = new HashMap<>();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getEntity())) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (damager != null && getGame().isParticipating(damager)) {
				Participant participant = getGame().getParticipant(damager);
				if (stackMap.containsKey(participant)) stackMap.get(participant).addStack();
				else new Stack(participant).start();
			}
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			if (stackMap.size() > 0) {
				new AbilityTimer(10) {
					
					@Override
					public void run(int count) {
						SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
					}
					
				}.setPeriod(TimeUnit.TICKS, 2).start();
				
				for (Stack stack : stackMap.values()) {
					stack.stunApply();
				}
				return cooldown.start();
			} else getPlayer().sendMessage("§4[§c!§4] §f망설이는 적이 없습니다.");
		}
		return false;
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final Participant participant;
		private final IHologram hologram;
		private int stack = 0;
		private final Random random = new Random();
		
		private Stack(Participant participant) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 4);
			this.participant = participant;
			this.player = participant.getPlayer();
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§b;", stack));
			hologram.display(getPlayer());
			stackMap.put(participant, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private void addStack() {
			stack++;
			hologram.setText(Strings.repeat("§b;", stack));
			if (random.nextInt(100) < stack * chanceper) {
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer().getLocation(), 1, 1.632f);
				float yaw = random.nextInt(361) - 180, pitch = random.nextInt(181) - 90;
				for (Player players : Bukkit.getOnlinePlayers()) {
					NMS.rotateHead(players, player, yaw, pitch);	
				}
				stop(false);
			}
		}
		
		private void stunApply() {
			Stun.apply(participant, TimeUnit.TICKS, stack * stunper);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(participant);
		}
		
	}

}
