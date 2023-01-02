package rainstar.abilitywar.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.BackseatGaming;

@AbilityManifest(
		name = "티배깅", rank = Rank.C, species = Species.HUMAN, explain = {
		"누군가가 나를 보고 있을 때 웅크리기를 연타할 경우 피해를 줍니다.",
		"만일 상대가 내 뒷모습을 보고 있을 때는 피해량이 더 강해집니다.",
		"나를 죽인 대상은 §c훈수§f 상태이상을 받습니다."
		},
		summarize = {
		"웅크리기 연타 시 나를 바라보는 적은 피해를 입습니다. §8(§7후방 2배§8)",
		"나를 죽인 대상은 §c훈수§f 상태이상을 받습니다."
		})

public class Teabagging extends AbilityBase {

	public Teabagging(Participant participant) {
		super(participant);
	}
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    } 
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Predicate<Entity> predicate2 = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private boolean isBehind(Player p, Player target) {
	    Location eye = p.getEyeLocation();
	    Vector toEntity = target.getEyeLocation().toVector().subtract(eye.toVector());
	    double dot = toEntity.normalize().dot(eye.getDirection());
	    return dot < -0.5D;
	}
	
	private Map<Player, Integer> teabaggingcount = new HashMap<>();
	private Set<Player> bagged = new HashSet<>();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && e.getDamager() instanceof Player) {
			BackseatGaming.apply(getGame().getParticipant((Player) e.getDamager()), TimeUnit.SECONDS, 1);
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		for (Participant participant : getGame().getParticipants()) {
    			if (predicate.test(participant.getPlayer())) {
        			Player player = participant.getPlayer();
        			if (getPlayer().equals(LocationUtil.getEntityLookingAt(Player.class, player, 10, predicate2))) {
        				if (getPlayer().isSneaking() && !bagged.contains(player)) {
        					bagged.add(player);
        					if (teabaggingcount.containsKey(player)) {
        						teabaggingcount.put(player, teabaggingcount.get(player) + 1);
        					} else {
        						teabaggingcount.put(player, 1);
        					}
        					if (teabaggingcount.get(player) >= 3) {
        						if (isBehind(getPlayer(), player)) {
            						player.damage(6, getPlayer());	
        						} else {
            						player.damage(3, getPlayer());	
        						}
        						Random random = new Random();
        						if (random.nextInt(10) == 0) {
            						SoundLib.ENTITY_WITCH_AMBIENT.playSound(player, 1, 1.5f);
        							switch(random.nextInt(8)) {
        							case 0: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」cせせ 醤 加閤劃?");
        							break;
        							case 1: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」cez");
        							break;
        							case 2: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」c公説相?");
        							break;
        							case 3: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」cせ");
        							break;
        							case 4: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」c荘~");
        							break;
        							case 5: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」c冊弾嬢推? 嬢村虞壱推~");
        							break;
        							case 6: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」cさぉずし~");
        							break;
        							case 7: player.sendMessage("」3[」b管径」3] 」e" + getPlayer().getName() + " 」b>」e " + player.getName() + "」7: 」c滴滴欠紙薩四~");
        							break;
        							}
        						}
        						teabaggingcount.put(player, 0);
        					}
        				}
        			}
    			}
    		}
    		if (!getPlayer().isSneaking()) {
    			bagged.clear();
    		}
    	}
    
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
}