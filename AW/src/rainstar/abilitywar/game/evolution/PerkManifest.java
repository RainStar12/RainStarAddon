package rainstar.abilitywar.game.evolution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PerkManifest {

	enum Quality {
		COMMON("§7일반 등급"),
		RARE("§a희귀 등급"),
		EPIC("§b영웅 등급"),
		LEGENDARY("§e전설 등급");
		
		private final String qualityName;
		
		Quality(final String qualityName) {
			this.qualityName = qualityName;
		}
		
		public String getQualityName() {
			return qualityName;
		}
	}
	
	String name();
	Quality quailty();
	int level();
	String[] explain() default {};
	
}
