package RainStarSynergy;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.LimitedPushingList;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "��ī�� ���ڵ�", rank = Rank.L, species = Species.OTHERS, explain = {
		"��7�нú� ��8- ��3�α� �����f: �ٸ� �÷��̾ ��Ƽ�� �ɷ��� ����� ������",
		" ��� �÷��̾ ����� �ɷ� �� ����� ��ġ�� �� �� �ֽ��ϴ�.",
		"��7�нú� ��8- ��2���� �����f: �÷��̾� �� �нú� ȿ������ ���� �ɷµ� ��",
		" �ϳ��� �нú긦 ȹ���մϴ�. ���� �ٸ� �нú� ȿ������ ������ �ɷ� �����ڰ�",
		" ������� ���, �ش� �нú� ȿ���� ��ȯ�˴ϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��a����� ������f: ���� �ֱٿ� ���� �ɷ� �� ���� ��� ����մϴ�.",
		" �� �ɷ��� ��Ÿ���� ��� �÷��̾ ������ �ִ� ��Ÿ���� ��հ���",
		" �ִ� 100�ʱ��� ����մϴ�. ���� ��Ÿ���� 0���� ���,",
		" ��� �⺻ ��Ÿ���� �����ϴ�. $[COOLDOWN_CONFIG]",
		"��7ö�� ��Ŭ�� ��8- ��b�ʿ��� �����f: ����� �������� ���� �ɷ� �� ��Ÿ����",
		" ���� �ð��� �� �� �ֽ��ϴ�. ���� ��ũ�� ä�� ��Ŭ�� ��",
		" �α� ������ �˸��� ���� �� �� �ֽ��ϴ�."
		})

public class AkashicRecords extends Synergy implements ActiveHandler {
	
	public AkashicRecords(Participant participant) {
		super(participant);
	}
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "��e")
			.put(Rank.B, "��b")
			.put(Rank.A, "��a")
			.put(Rank.S, "��d")
			.put(Rank.L, "��6")
			.put(Rank.SPECIAL, "��c")
			.build();
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = synergySettings.new SettingObject<Integer>(AkashicRecords.class,
			"cooldown", 60, "# ��Ÿ��") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};

	private LimitedPushingList<AbilityBase> logability = new LimitedPushingList<>(2);
	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue(), "��ī�� ���ڵ�");
	private AbilityBase ability = null;
	private boolean alarm = true;
	private final ActionbarChannel ac = newActionbarChannel();

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			logability.clear();
			ability = null;
		}
	}
	
	@SubscribeEvent
	private void onAbilityActiveSkill(AbilityActiveSkillEvent e) {
		if (!e.getParticipant().equals(getParticipant()) && alarm) {
	    	AbilityBase ab = e.getParticipant().getAbility();
	    	final Mix mix = (Mix) ab;
			int getx = (int) e.getParticipant().getPlayer().getLocation().getX();
			int gety = (int) e.getParticipant().getPlayer().getLocation().getY();
			int getz = (int) e.getParticipant().getPlayer().getLocation().getZ();
	    	if (mix.hasSynergy()) {
	    		if (mix.getSynergy().getClass().equals(AkashicRecords.class)) {
	    			getPlayer().sendMessage("[��c!��f] ��e" + e.getPlayer().getName() + "��f���� ��6��ī�� ���ڵ��f�� �����Ͽ����ϴ�.");
	    		} else {
		    		getPlayer().sendMessage("[��c!��f] ��e" + e.getPlayer().getName() + "��f���� ��3�ó�����f �ɷ��� ����Ͽ����ϴ�.");
					getPlayer().sendMessage(" ��7- ��e��� �ɷ¡�f: " + rankcolor.get(mix.getSynergy().getRank()) + mix.getSynergy().getDisplayName()
					+ " ��7| ��e��� ��ġ��f: ��3" + getx + "��f, ��3" + gety + "��f, ��3" + getz);
					try {
						this.logability.add(AbilityBase.create(mix.getSynergy().getClass(), getParticipant())); 
						this.logability.get(0).setRestricted(false);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}	
	    		}
	    	} else {
				final AbilityBase first = mix.getFirst(), second = mix.getSecond();
				getPlayer().sendMessage("[��c!��f] ��e" + e.getPlayer().getName() + "��f���� �ɷ��� ����Ͽ����ϴ�.");
				getPlayer().sendMessage(" ��7- ��e��� �ɷ¡�f: " + rankcolor.get(first.getRank()) + first.getDisplayName() + "��f + " + rankcolor.get(second.getRank()) + 
						second.getDisplayName() + " ��7| ��e��� ��ġ��f: ��3" + getx + "��f, ��3" + gety + "��f, ��3" + getz);
				if (first instanceof ActiveHandler && !(second instanceof ActiveHandler)) {
					try {
						this.logability.add(AbilityBase.create(first.getClass(), getParticipant())); 
						this.logability.get(0).setRestricted(false);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				} else if (!(first instanceof ActiveHandler) && second instanceof ActiveHandler) {
					try {
						this.logability.add(AbilityBase.create(second.getClass(), getParticipant())); 
						this.logability.get(0).setRestricted(false);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				} else if (first instanceof ActiveHandler && second instanceof ActiveHandler) {
					Random random = new Random();
					if (random.nextBoolean() == true) {
						try {
							this.logability.add(AbilityBase.create(first.getClass(), getParticipant())); 
							this.logability.get(0).setRestricted(false);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					} else {
						try {
							this.logability.add(AbilityBase.create(second.getClass(), getParticipant())); 
							this.logability.get(0).setRestricted(false);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
				}	
	    	}
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getGame().getParticipant(e.getEntity()).hasAbility()) {
	    	AbilityBase ab = getGame().getParticipant(e.getEntity()).getAbility();
	    	final Mix mix = (Mix) ab;
	    	if (mix.hasSynergy()) {
	    		if (!(mix.getSynergy() instanceof TargetHandler) && !(mix.getSynergy() instanceof ActiveHandler)) {
	    			if (!mix.getSynergy().getClass().equals(AkashicRecords.class)) {
						try {
							if (ability != null) {
								ability.destroy();
							}
							this.ability = AbilityBase.create(mix.getSynergy().getClass(), getParticipant());
							this.ability.setRestricted(false);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}	
	    			}	
	    		}
	    	} else {
				final AbilityBase first = mix.getFirst(), second = mix.getSecond();
				if (!(first instanceof TargetHandler) && !(first instanceof ActiveHandler) && !(second instanceof TargetHandler) && !(second instanceof ActiveHandler)) {
					Random random = new Random();
					if (random.nextBoolean() == true) {
						try {
							if (ability != null) {
								ability.destroy();
							}
							this.ability = AbilityBase.create(first.getClass(), getParticipant());
							this.ability.setRestricted(false);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					} else {
						try {
							if (ability != null) {
								ability.destroy();
							}
							this.ability = AbilityBase.create(second.getClass(), getParticipant());
							this.ability.setRestricted(false);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
				} else if (!(first instanceof TargetHandler) && !(first instanceof ActiveHandler)) {
					try {
						if (ability != null) {
							ability.destroy();
						}
						this.ability = AbilityBase.create(first.getClass(), getParticipant());
						this.ability.setRestricted(false);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				} else if (!(second instanceof TargetHandler) && !(second instanceof ActiveHandler)) {
					try {
						if (ability != null) {
							ability.destroy();
						}
						this.ability = AbilityBase.create(second.getClass(), getParticipant());
						this.ability.setRestricted(false);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				}
	    	}
	    	if (ability != null) {
	    		ac.update("��b�нú� ��f: " + rankcolor.get(ability.getRank()) + ability.getDisplayName());
	    	}
		}
	}
	
	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}
	
	@Override
	public boolean usesMaterial(Material material) {
		if (ability != null && logability.get(0) != null && logability.get(1) != null) {
			return ability.usesMaterial(material) && logability.get(0).usesMaterial(material) && logability.get(1).usesMaterial(material);
		}
		return super.usesMaterial(material);
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {	
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			if (logability.size() == 2) {
				ActiveHandler active0 = (ActiveHandler) logability.get(0);
				ActiveHandler active1 = (ActiveHandler) logability.get(1);
				active0.ActiveSkill(Material.IRON_INGOT, ClickType.RIGHT_CLICK);
				active0.ActiveSkill(Material.IRON_INGOT, ClickType.LEFT_CLICK);
				active0.ActiveSkill(Material.GOLD_INGOT, ClickType.RIGHT_CLICK);
				active0.ActiveSkill(Material.GOLD_INGOT, ClickType.LEFT_CLICK);
				active1.ActiveSkill(Material.IRON_INGOT, ClickType.RIGHT_CLICK);
				active1.ActiveSkill(Material.IRON_INGOT, ClickType.LEFT_CLICK);
				active1.ActiveSkill(Material.GOLD_INGOT, ClickType.RIGHT_CLICK);
				active1.ActiveSkill(Material.GOLD_INGOT, ClickType.LEFT_CLICK);
				int coolvalue = 0;
				for (Participant p : getGame().getParticipants()) {
					if (p.hasAbility() && !p.getAbility().isRestricted()) {
						AbilityBase ab = p.getAbility();
						for (GameTimer t : ab.getRunningTimers()) {
							if (t instanceof Cooldown.CooldownTimer) {
								coolvalue = coolvalue + t.getCount();
							}
						}
					}
				}
				if (coolvalue != 0) {
					cooldown.setCount(Math.min(100, (coolvalue / getGame().getParticipants().size())));
				}
				return cooldown.start();	
			} else {
				getPlayer().sendMessage("��4[��c!��4] ��f���� ��Ͽ� �ۼ��� ���� �����ϴ�.");
			}

		}
		
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK) {
			if (getPlayer().isSneaking()) {
				if (alarm) {
					getPlayer().sendMessage("��f[��c!��f] ��3�α� �����f �˸��� �����մϴ�.");
					alarm = false;
				} else {
					getPlayer().sendMessage("��f[��c!��f] ��3�α� �����f �˸��� �۵��մϴ�.");
					alarm = true;
				}

			} else {
				int coolvalue = 0;
				for (Participant p : getGame().getParticipants()) {
					if (p.hasAbility() && !p.getAbility().isRestricted()) {
						AbilityBase ab = p.getAbility();
						for (GameTimer t : ab.getRunningTimers()) {
							if (t instanceof Cooldown.CooldownTimer) {
								coolvalue = coolvalue + t.getCount();
							}
						}
					}
				}
				if (logability.size() == 2) {
					getPlayer().sendMessage("��3[��b!��3] ��e����� �ɷ¡�f: " + rankcolor.get(logability.get(0).getRank()) + logability.get(0).getDisplayName() + "��f + "
							+ rankcolor.get(logability.get(1).getRank()) + logability.get(1).getDisplayName());
				} else {
					getPlayer().sendMessage("��3[��b!��3] ��e����� �ɷ��� �����ϴ�.");
				}

				if (coolvalue == 0) {
					getPlayer().sendMessage("��3[��b!��3] ��c����� ��Ÿ�ӡ�f: 60��");
				} else {
					getPlayer().sendMessage("��3[��b!��3] ��c����� ��Ÿ�ӡ�f: " + Math.min(100, (coolvalue / getGame().getParticipants().size())) + "��");
				}
			
			}
		}
		return false;
	}
	
}
