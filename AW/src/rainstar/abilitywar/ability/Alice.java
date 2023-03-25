package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Confusion;

@AbilityManifest(
		name = "앨리스", rank = Rank.A, species = Species.HUMAN, 
		explain = {
		"§7철괴 클릭 §8- §a이상한 나라로§f: 우클릭 시 한 장의 §7트럼프 카드§f를 발사합니다.",
		" 좌클릭 시 다섯 장의 §7트럼프 카드§f를 §d연속§f 발사하고, §c쿨타임§f을 §e100%§f 더 가집니다.",
		" §7트럼프 카드§f는 총 54장으로 구성되어 있으며, 한 번 뽑은 §7카드§f는 나오지 않습니다.",
		" 각 문양별로 특수 효과가 하나씩 존재하며, 1~13의 숫자로 효과 배율이 정해집니다.",
		" 54장을 전부 사용시 덱을 갈아끼웁니다. $[COOLDOWN_CONFIG]",
		" §8♠ §7-§f 카드를 발사해 엔티티를 관통하며 닿은 적에게 §c§n출혈 피해§f를 입힙니다.",
		" §c♥ §7-§f 카드를 발사해 적중 대상과 자신의 체력을 즉시 회복합니다.",
		" §8♣ §7-§f 카드를 발사해 적중 위치를 폭발시킵니다.",
		" §c♦ §7-§f 카드를 발사해 적중 대상에게 추가 피해와 §6§n혼란 효과§f를 부여합니다.",
		" §8Joker §7-§f §8♠ §f+ §8♣ §f효과를 최대 출력으로 동시에 사용합니다.",
		" §cJoker §7-§f §c♥ §f+ §c♦ §f효과를 최대 출력으로 동시에 사용합니다.",
		},
		summarize = {
		"§7철괴를 우클릭§f하면 바라보는 방향으로 카드를 발사해 적중한 적에게",
		"상태이상 및 피해를 입히거나, 적과 자신이 회복됩니다.",
		"문양에 따라 효과가 다르며 숫자가 높을수록 카드 효과가 강력해집니다.",
		"§7철괴를 좌클릭§f하면 5연속 발사하지만 다음 쿨타임이 2배가 됩니다.",
		"$[COOLDOWN_CONFIG]"
		})

public class Alice extends AbilityBase implements ActiveHandler {
	
	public Alice(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = 
			abilitySettings.new SettingObject<Integer>(Alice.class, "cooldown", 15,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	public static final SettingObject<Integer> DELAY_CONFIG = 
			abilitySettings.new SettingObject<Integer>(Alice.class, "shoot-delay", 3,
            "# 철괴 좌클릭 연속발사 딜레이", "# 단위: 틱", "# 20틱 = 1초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	refill();
	    	passive.start();
	    } 
	}
	
	private List<Cards> deck = new ArrayList<>();
	private ActionbarChannel ac = newActionbarChannel();
	@SuppressWarnings("unused")
	private CardBullet bullet = null;
	
	private final int shotdelay = DELAY_CONFIG.getValue();
	private final Cooldown cool = new Cooldown(COOLDOWN_CONFIG.getValue(), "카드", CooldownDecrease._50);
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		ac.update("§7[" + deck.get(0).toString() + "§7]");
    	}
    
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer continuity = new AbilityTimer(5) {
    	
    	@Override
		public void run(int count) {
			new CardBullet(getPlayer(), getPlayer().getEyeLocation().clone().subtract(0, 0.5, 0), getPlayer().getEyeLocation().getDirection().setY(0).normalize(), getPlayer().getLocation().getYaw(), deck.get(0).getSuit(), deck.get(0).getRank()).start();
			deck.remove(0);
			if (deck.isEmpty()) {
				refill();
			}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		cool.start();
    		cool.setCount((int) (cool.getCount() * 2));
    	}
    
    }.setPeriod(TimeUnit.TICKS, shotdelay).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && !cool.isCooldown() && !continuity.isRunning()) {
			if (clicktype == ClickType.RIGHT_CLICK) {
				new CardBullet(getPlayer(), getPlayer().getEyeLocation().clone().subtract(0, 0.5, 0), getPlayer().getEyeLocation().getDirection().setY(0).normalize(), getPlayer().getLocation().getYaw(), deck.get(0).getSuit(), deck.get(0).getRank()).start();
				deck.remove(0);
				if (deck.isEmpty()) {
					refill();
				}
				return cool.start();	
			} else if (clicktype == ClickType.LEFT_CLICK) {
				continuity.start();
				return true;
			}
		}
		return false;
	}
    
	public void refill() {
		deck.clear();
		for (int a = 0; a < 4; a++) {
			for (int b = 0; b < 13; b++) {
            	deck.add(new Cards(a, b));
            }
		}
        deck.add(new Cards(4, 13));
        deck.add(new Cards(5, 13));
        Collections.shuffle(deck);
        getPlayer().sendMessage("§8[§c!§8] §f새로운 트럼프 카드를 꺼내 덱을 셔플하였습니다.");
        new AbilityTimer(15) {
        	
        	@Override
        	public void run(int count) {
        		if (count % 2 == 0) {
                    SoundLib.BLOCK_WOODEN_TRAPDOOR_CLOSE.playSound(getPlayer(), 1, 1.8f);
        		} else {
                    SoundLib.BLOCK_WOODEN_TRAPDOOR_OPEN.playSound(getPlayer(), 1, 1.8f);	
        		}
        	}
        	
        }.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	class Cards {
		
		private final int rank;
        private final int suit;
        private final String[] ranks = {"A","2","3","4","5","6","7","8","9","10","J","Q","K", "Joker"};
        private final String[] suits = {"§8♠ §f","§c♥ §f","§8♣ §f","§c♦ §f", "§8", "§c"};

        public Cards(int suit, int values) {
            this.rank = values;
            this.suit = suit;
        }

        public String toString() {
            return getSuitName() + getRankName();
        }

        public String getRankName() {
            return ranks[rank];
        }

        public String getSuitName() {
            return suits[suit];
        }
        
        public int getRank() {
        	return rank;
        }
        
        public int getSuit() {
        	return suit;
        }
        
	}
	
	public class CardBullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final int suit;
		private final int rank;
		private Set<Damageable> checkhit = new HashSet<>();
		private final float yaw;

		private RGB black = RGB.of(1, 1, 1), white = RGB.of(254, 254, 254), red = RGB.of(254, 1, 1);
		private Location lastLocation;
		
		private final Points SPADE1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, false, true, false, true, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points SPADE2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, true, false, true, false, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points HEART1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, true, false, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points HEART2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, false, true, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points CLUB1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, false, true, false, true, false, false},
			{false, true, true, true, true, true, true, true, false},
			{false, false, true, false, true, false, true, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points CLUB2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, true, false, true, false, true, true},
			{true, false, false, false, false, false, false, false, true},
			{true, true, false, true, false, true, false, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points DIAMOND1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points DIAMOND2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points JOKER1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, false, false, false, true, false, false},
			{false, false, true, false, false, false, true, false, false},
			{false, false, false, false, false, false, true, false, false},
			{false, false, false, false, false, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points JOKER2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, true, true, true, false, true, true},
			{true, true, false, true, true, true, false, true, true},
			{true, true, true, true, true, true, false, true, true},
			{true, true, true, true, true, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private CardBullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, float yaw, int suit, int rank) {
			super(5);
			setPeriod(TimeUnit.TICKS, 1);
			Alice.this.bullet = this;
			this.shooter = shooter;
			this.entity = new CardBullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.25, -0.5, -1.25, 1.25, 0.5, 1.25);
			this.forward = arrowVelocity.multiply(3);
			this.lastLocation = startLocation;
			this.suit = suit;
			this.rank = rank;
			this.yaw = yaw;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void onStart() {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(shooter.getLocation(), 1, 2f);
			SPADE1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			SPADE2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			HEART1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			HEART2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CLUB1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CLUB2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			DIAMOND1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			DIAMOND2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			JOKER1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			JOKER2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
		}
		
		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(1.5);
				private final int amount = (int) (vectorBetween.length() / 1.5);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				entity.setLocation(location);
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					if (suit == 2) {
						ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
						shooter.getWorld().createExplosion(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), (float) (1 + ((rank + 1) * 0.1)), false, true);
					}
					if (suit == 4) {
						ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
						shooter.getWorld().createExplosion(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), 2.3f, false, true);
					}
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable) && !checkhit.contains(damageable)) {
						checkhit.add(damageable);
						if (suit == 0) {
							Damages.damageArrow(damageable, shooter, (float) (8 + ((rank + 1) * 0.3)));
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Bleed.apply(getGame().getParticipant(p), TimeUnit.TICKS, (rank + 1) * 10, 10);
							}
						}
						if (suit == 1) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, ((rank + 1) * 0.3), RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									Healths.setHealth(p, p.getHealth() + event.getAmount());
									SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p, 1, 1.2f);
									ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
								}
							}
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(shooter, ((rank + 1) * 0.5), RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth((Player) shooter, shooter.getHealth() + event.getAmount());
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound((Player) shooter, 1, 1.2f);
								ParticleLib.HEART.spawnParticle(shooter.getLocation(), 0.5, 1, 0.5, 10, 1);
							}
							stop(false);
							return;
						}
						if (suit == 2) {
							ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
							shooter.getWorld().createExplosion(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), (float) (1 + ((rank + 1) * 0.1)), false, true);
							stop(false);
							return;
						}
						if (suit == 3) {
							Damages.damageArrow(damageable, shooter, (float) (6 + ((rank + 1) * 0.7)));
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Confusion.apply(getGame().getParticipant(p), TimeUnit.TICKS, (rank + 1) * 10, 10);
							}
							stop(false);
							return;
						}
						if (suit == 4) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Bleed.apply(getGame().getParticipant(p), TimeUnit.TICKS, 130, 10);
							}
							ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
							shooter.getWorld().createExplosion(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), 2.4f, false, true);
						}
						if (suit == 5) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, 3.9, RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									Healths.setHealth(p, p.getHealth() + event.getAmount());
									SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p, 1, 1.2f);
									ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
								}
								Confusion.apply(getGame().getParticipant(p), TimeUnit.TICKS, 130, 10);
							}
							Damages.damageArrow(damageable, shooter, 17.1f);
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(shooter, 6.5, RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth((Player) shooter, shooter.getHealth() + event.getAmount());
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound((Player) shooter, 1, 1.2f);
								ParticleLib.HEART.spawnParticle(shooter.getLocation(), 0.5, 1, 0.5, 10, 1);
							}
							stop(false);
							return;
						}
					}
				}
				if (suit == 0) {
					for (Location loc : SPADE1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : SPADE2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 1) {
					for (Location loc : HEART1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : HEART2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 2) {
					for (Location loc : CLUB1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : CLUB2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 3) {
					for (Location loc : DIAMOND1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : DIAMOND2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 4) {
					for (Location loc : JOKER1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : JOKER2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 5) {
					for (Location loc : JOKER1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : JOKER2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
			Alice.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			Alice.this.bullet = null;
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				final Player deflectedPlayer = deflector.getPlayer();
				new CardBullet(deflectedPlayer, lastLocation, newDirection, (yaw - 180), suit, rank).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}
		
	}

}
