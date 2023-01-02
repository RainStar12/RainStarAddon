package rainstar.abilitywar.utils;

import static com.google.common.base.Preconditions.checkArgument;

import org.bukkit.util.Vector;

import daybreak.abilitywar.utils.base.math.FastMath;

@SuppressWarnings("serial")
public class Arc extends Shape {

	private static final double
			RADIANS_15 = Math.toRadians(15),
			RADIANS_150 = Math.toRadians(150),
			RADIANS_165 = Math.toRadians(165),
			RADIANS_210 = Math.toRadians(210);

	private Arc(double radius, double frequency) {
		super();
		checkArgument(frequency >= 1, "The frequency must be 1 or greater.");
		checkArgument(radius > 0, "The radius must be positive");
		checkArgument(!Double.isNaN(radius) && Double.isFinite(radius));
		double divided = RADIANS_150 / frequency;
		for (double radians = RADIANS_15; radians <= RADIANS_165; radians += divided) {
			add(new Vector(FastMath.cos(radians) * radius, 0, FastMath.sin(radians) * radius));
		}
		divided = RADIANS_210 / frequency;
	}

	private Arc(int amount) {
		super(amount);
	}

	public static Arc of(double radius, double frequency) {
		return new Arc(radius, frequency);
	}

	@Override
	public Arc clone() {
		Arc Arc = new Arc(size());
		for (Vector vector : this) {
			Arc.add(vector.clone());
		}
		return Arc;
	}

}