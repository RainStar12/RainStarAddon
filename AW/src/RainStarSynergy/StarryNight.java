package RainStarSynergy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(
		name = "", rank = Rank.A, species = Species.OTHERS, explain = {
		""
		})

public class StarryNight extends Synergy {
	
	public StarryNight(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> STACK_COOL = 
			synergySettings.new SettingObject<Double>(StarryNight.class, "stack-cooldown", 1.0,
            "# 스택을 쌓을 때 내부 쿨타임") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final ImmutableMap<Boolean, String> type = ImmutableMap.<Boolean, String>builder()
			.put(true, "§e✭")
			.put(false, "§e🌙")
			.build();
	
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final Random random = new Random();
	private final double stackcool = STACK_COOL.getValue();
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Participant target;
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			if (stackMap.containsKey(e.getEntity())) {
				if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
					if (stackMap.get(e.getEntity()).addStack(true)) {
						
					}	
				}
			} else new Stack((Player) e.getEntity(), true).start();
		}
		if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				target = getGame().getParticipant((Player) e.getEntity());
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
						if (stackMap.get(e.getEntity()).addStack(false)) {
							
						}	
					}
				} else new Stack((Player) e.getEntity(), false).start();
			}
		}
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		private List<Boolean> stacks = new ArrayList<>();
		
		private Stack(Player player, boolean stacktype) {
			super(300);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					"§7????");
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack(stacktype);
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack(boolean stacktype) {
			setCount(300);
			stack++;
			stacks.add(stacktype);
			String string = "";
			for (Boolean booleans : stacks) {
				string = string + type.get(booleans);
			}
			hologram.setText(string.concat(Strings.repeat("§7?", 4 - stack)));
			if (stack >= 4) {
				stop(false);
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}

}
