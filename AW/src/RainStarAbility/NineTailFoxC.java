package RainStarAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import RainStarEffect.Charm;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "����ȣ(�а�)", rank = Rank.S, species = Species.HUMAN, explain = {
		"��7�нú� ��8- ��c�а���f: ȸ�� ȿ���� ���� �� �����ϴ�.",
		" ü���� ���� �Ҹ��� �� ��ü ü���� 40%�� ���� ����ȣ�� ���ư��ϴ�.",
		"��7���� ��8- ��c����� ������f: �ٸ� �÷��̾ ������ �� $[STACK_COOL]�ʸ��� ǥ���� �װ�,",
		" ǥ���� $[STACK_MAX]���� �� �� ����� $[CHARM_DURATION]�ʰ� ��Ȥ�մϴ�. ��Ȥ ���߿� ǥ���� ���� ���մϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��c������f: �ٶ󺸴� ���⿡ 10�ʰ� �� ��ƼŬ�� �����,",
		" �ٽ� ��Ŭ�� �� ���� ���� ������ �߽ɿ� ���� ����� ��󿡰� �����մϴ�.",
		" �̶� ���� �� ��� ��󿡰� ��� ���� ������� �����ϴ�. ���� ���� �� ��� ��",
		" ��Ȥ ���� ����� �ִٸ� ��Ȥ�� Ǯ�� ���� �ð��� �ݺ���Ͽ� ���ظ� �����ϴ�.",
		" $[COOLDOWN]",
		"��7�����̻� ��8- ��d��Ȥ��f: ����� ������ ���� �ٶ󺸰� �ǰ�,",
		" ����� ���� �ִ� ���ط��� $[CHARM_DECREASE]% �����մϴ�. ����� ������ ������",
		" �� ���� ������� $[CHARM_HEAL]%��ŭ ü���� ȸ���մϴ�."
		},
		summarize = {
		"��d��Ȥ ���¡�f�� ���� Ÿ���� ȸ���ϴ� �� �ܿ� ȸ�� ȿ���� ���� �� �����ϴ�.",
		"ü���� ���� �Ҹ��ϸ� ��ü ü���� 40%�� ���� ����ȣ�� �ǵ��ư��ϴ�.",
		"�ٸ� �÷��̾ ������ ������ ǥ���� �׾� $[STACK_MAX]���� �� �� ����� ��d��Ȥ��f�մϴ�.",
		"��d��Ȥ��f�� ����� ���� ������ �ٶ󺸸� ���� �ִ� ���ط��� �����մϴ�.",
		"��7ö�� ��Ŭ����f���� ���� ���� ����� ��ġ���� �̵��� �����մϴ�. $[COOLDOWN]"
		})

public class NineTailFoxC extends AbilityBase implements ActiveHandler {
	
	public NineTailFoxC(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "cooldown", 90,
            "# ��Ÿ��") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };
    
	public static final SettingObject<Double> STACK_COOL = 
			abilitySettings.new SettingObject<Double>(NineTailFoxC.class, "stack-cooldown", 1.0,
            "# ������ ���� �� ���� ��Ÿ��") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> STACK_MAX = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "stack-max", 4,
            "# �ִ� ���� ����") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DURATION = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-duration", 4,
            "# ��Ȥ ���� �ð�") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-decrease", 35,
            "# ��Ȥ ���� ����� ������ (����: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Integer> CHARM_HEAL = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-heal", 55,
            "# ��Ȥ�� ��� Ÿ�ݽ� ȸ���� (����: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> DAMAGE = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "damage", 4,
            "# ö�� ��Ŭ�� ���ط�") {

        @Override
        public boolean condition(Integer value) {
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
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final double stackcool = STACK_COOL.getValue();
	private final int maxstack = STACK_MAX.getValue();
	private final int charmduration = CHARM_DURATION.getValue();
	private final int charmdecrease = CHARM_DECREASE.getValue();
	private final int charmheal = CHARM_HEAL.getValue();
	private final int damage = DAMAGE.getValue();
	
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private static final Set<Material> nocheck;
	private Participant target = null;
	private static final Circle circle = Circle.of(5, 70);
	private static final Circle heart = Circle.of(5, 40);
	private static final RGB color = new RGB(243, 97, 166);
	private static final RGB color2 = new RGB(99, 58, 1);
	private Location targetblock;
	
	static { nocheck = ImmutableSet.of(MaterialX.AIR.getMaterial(), MaterialX.GRASS.getMaterial(), MaterialX.WATER.getMaterial(),
			MaterialX.LAVA.getMaterial()); }
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !cool.isCooldown()) {
	    	if (!skill.isRunning()) {
	    		skill.start();
	    	} else {
	    		skill.stop(false);
		    	return true;
	    	}
	    }
		return false;
	}
    
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				getPlayer().sendMessage("��a���� ������� ���� �ʽ��ϴ�.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
				e.setCancelled(true);
			}
	    	if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && !e.isCancelled()) {
	    		e.setCancelled(true);
				double nowhp = ((4 * (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) / 10));
				getPlayer().setHealth(nowhp);
			   	SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer(), 1, 0.7f);
			   	getPlayer().sendMessage("��5[��d!��5] ��c�а��� Ǯ�Ƚ��ϴ�. ����ȣ�� �ǵ��ư��ϴ�. ��7/aw check Ȥ�� /aw sum");
		    	AbilityBase ab = getParticipant().getAbility();
		    	if (ab.getClass().equals(Mix.class)) {
		    		final Mix mix = (Mix) ab;
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					if (this.equals(first)) {
						try {
							mix.setAbility(NineTailFox.class, second.getClass());
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					} else if (this.equals(second)) {
						try {
							mix.setAbility(first.getClass(), NineTailFox.class);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
		    	} else {
			    	try {
						getParticipant().setAbility(NineTailFox.class);
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}	
		    	}
	    	}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			if (!target.hasEffect(Charm.registration)) {
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).getCount() < (120 - (stackcool * 20))) {
						if (stackMap.get(e.getEntity()).addStack()) {
							Charm.apply(target, TimeUnit.SECONDS, charmduration, getPlayer(), charmheal, charmdecrease);
						}	
					}
				} else new Stack((Player) e.getEntity()).start();
			}
		}
		if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				target = getGame().getParticipant((Player) e.getEntity());
				if (!target.hasEffect(Charm.registration)) {
					if (stackMap.containsKey(e.getEntity())) {
						if (stackMap.get(e.getEntity()).getCount() < (120 - (stackcool * 20))) {
							if (stackMap.get(e.getEntity()).addStack()) {
								Charm.apply(target, TimeUnit.SECONDS, charmduration, getPlayer(), charmheal, charmdecrease);
							}	
						}
					} else new Stack((Player) e.getEntity()).start();
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerJoin(PlayerJoinEvent e) {
		for (Stack stack : stackMap.values()) {
			stack.hologram.display(getPlayer());
		}
	}
	
	private final Duration skill = new Duration(200, cool) {

		@Override
		protected void onDurationProcess(int count) {
			targetblock = getPlayer().getTargetBlock(nocheck, 30).getLocation();
			if (count % 2 == 0) {
				for (Location loc : circle.toLocations(targetblock).floor(targetblock.getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}	
			}
			for (Player p : getPlayer().getWorld().getPlayers()) {
				if (LocationUtil.isInCircle(targetblock, p.getLocation(), 5) && predicate.test(p)) {
					if (!p.hasPotionEffect(PotionEffectType.GLOWING)) {
						SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
						PotionEffects.GLOWING.addPotionEffect(p, 9999, 0, true, false, false);	
					}
				} else {
					p.removePotionEffect(PotionEffectType.GLOWING);
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, targetblock, 5, predicate)) {
				if (p != null) {
					p.removePotionEffect(PotionEffectType.GLOWING);
					Player nearest = LocationUtil.getNearestEntity(Player.class, targetblock, predicate);
					new Rush(nearest).start();
				} else {
					cool.setCount(cool.getCount() / 4);
				}
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1);
	
	private class Rush extends AbilityTimer {
		
		private Player player;
		
		private Rush(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
		}
		
		@Override
		protected void run(int count) {
			if (player != null) {
				if (getPlayer().getLocation().distanceSquared(player.getLocation()) >= 4) {
					for (Location loc : Line.between(player.getLocation(), getPlayer().getLocation(), 1).toLocations(getPlayer().getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, color2);
					}
					getPlayer().setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(3)));	
				} else {
					getPlayer().setVelocity(new Vector(0, 0, 0));
					stop(false);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, targetblock, 5, predicate)) {
				if (getGame().getParticipant(p).hasEffect(Charm.registration)) {
					int charmcount = getGame().getParticipant(p).getPrimaryEffect(Charm.registration).getDuration();
					Damages.damageFixed(p, getPlayer(), Math.max(damage, damage * (2 - (charmcount / charmduration))));
					getGame().getParticipant(p).removeEffects(Charm.registration);
				} else {
					Damages.damageFixed(p, getPlayer(), damage);
				}
			}
			for (Location loc : heart.toLocations(targetblock).floor(targetblock.getY())) {
				ParticleLib.HEART.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
		}
		
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(100);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("��d��", stack).concat(Strings.repeat("��d��", maxstack - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack() {
			setCount(100);
			stack++;
			hologram.setText(Strings.repeat("��d��", stack).concat(Strings.repeat("��d��", maxstack - stack)));
			if (stack >= maxstack) {
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