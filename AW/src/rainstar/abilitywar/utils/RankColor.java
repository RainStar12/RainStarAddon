package rainstar.abilitywar.utils;

import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.google.common.collect.ImmutableMap;

public class RankColor {

	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();
	
	public RankColor() {
		super();
	}
	
	public static String getColor(Rank rank) {
		return rankcolor.get(rank);
	}
	
}
