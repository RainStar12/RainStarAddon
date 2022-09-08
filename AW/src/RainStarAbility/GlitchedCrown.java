package RainStarAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "글리치드 크라운", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
        "§7철괴 좌클릭 §8- §c?????§f: 최대 10개까지, 무작위 능력을 배열에 집어넣습니다.",
        " 배열에 능력이 많을수록 순환 속도는 점점 더 빨라집니다.",
        "§7철괴 우클릭 §8- §7§k?????§f: 빠르게 순환하고 있는 능력들 중, 우클릭한 타이밍의",
        " 현재 능력을 자신의 능력으로 변경합니다. 이후 §e§o글리치드 크라운§f 효과는 사라집니다."
        },
        summarize = {
        "§7철괴 좌클릭§f 시 배열에 §a능력§f을 하나 추가하고 순환 속도를 가속합니다. §8(§7최대 10§8)",
        "§7철괴 우클릭§f 시 그 타이밍의 §a능력§f을 자신의 §a능력§f으로 변경합니다."
        })
public class GlitchedCrown extends AbilityBase implements ActiveHandler {
	
	public GlitchedCrown(Participant participant) {
		super(participant);
	}
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();
	
	private final Random random = new Random();
	private int period = 20;
	private int stack = 0;
	private List<AbilityRegistration> abilities = new ArrayList<>();
	private AbilityRegistration nowAbility;
	
	public AbilityRegistration getRandomAbility() {
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> !Configuration.Settings.isBlacklisted(ability.getManifest().name()) && !(abilities.contains(ability))
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void onStart() {
			abilities.add(getRandomAbility());
			nowAbility = abilities.get(0);
		}
		
		@Override
		public void run(int count) {
			if (count % period == 0) {
				stack = (stack < (abilities.size() - 1) ? stack + 1 : 0);
				nowAbility = abilities.get(stack);
				NMS.sendTitle(getPlayer(), rankcolor.get(nowAbility.getManifest().rank()) + nowAbility.getManifest().name(), "§e?????", 0, 10, 0);
			}
		}
		
		@Override
		public void onEnd() {
			getPlayer().sendMessage("§6[§e!§6§k] §f당신의 능력이 " + rankcolor.get(nowAbility.getManifest().rank()) + nowAbility.getManifest().name() + "§f으로 변경§k되§f었습니§k다§f.");
			NMS.clearTitle(getPlayer());
			if (getParticipant().getAbility().getClass().equals(Mix.class)) {
				Mix mix = (Mix) getParticipant().getAbility();
				AbilityBase first = mix.getFirst(), second = mix.getSecond();
				if (first.getClass().equals(GlitchedCrown.class)) {
					try {
						mix.setAbility(nowAbility.getAbilityClass(), second.getClass());
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				} else if (second.getClass().equals(GlitchedCrown.class)) {
					try {
						mix.setAbility(first.getClass(), nowAbility.getAbilityClass());
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					getParticipant().setAbility(nowAbility);
				} catch (UnsupportedOperationException | ReflectiveOperationException e) {
					e.printStackTrace();
				}	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT) {
			if (clicktype == ClickType.RIGHT_CLICK) {
				passive.stop(false);
				return true;
			} else if (clicktype == ClickType.LEFT_CLICK) {
				if (abilities.size() < 9) {
					abilities.add(getRandomAbility());
					switch(abilities.size()) {
					case 0:
						period = 20;
						break;
					case 1:
						period = 18;
						break;
					case 2:
						period = 15;
						break;
					case 3:
						period = 13;
						break;
					case 4:
						period = 10;
						break;
					case 5:
						period = 7;
						break;
					case 6:
						period = 5;
						break;
					case 7:
						period = 4;
						break;
					case 8:
						period = 2;
						break;
					case 9:
						period = 1;
						break;
					}
				} else getPlayer().sendMessage("§6[§e!§6] §f이미 넣을 수 있는 능력이 최대치입니다.");
			}
		}
		return false;
	}

}
