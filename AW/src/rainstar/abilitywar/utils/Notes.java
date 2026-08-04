package rainstar.abilitywar.utils;

import org.bukkit.Note;
import org.bukkit.Note.Tone;

public class Notes {

	public static Note getNote(int number) {
		switch (number) {
		case 0:
			return Note.sharp(0, Tone.F);
		case 1:
			return Note.natural(0, Tone.G);
		case 2:
			return Note.sharp(0, Tone.G);
		case 3:
			return Note.natural(0, Tone.A);
		case 4:
			return Note.sharp(0, Tone.A);
		case 5:
			return Note.natural(0, Tone.B);
		case 6:
			return Note.natural(0, Tone.C);
		case 7:
			return Note.sharp(0, Tone.C);
		case 8:
			return Note.natural(0, Tone.D);
		case 9:
			return Note.sharp(0, Tone.D);
		case 10:
			return Note.natural(0, Tone.E);
		case 11:
			return Note.natural(0, Tone.F);
		case 12:
			return Note.sharp(1, Tone.F);
		case 13:
			return Note.natural(1, Tone.G);
		case 14:
			return Note.sharp(1, Tone.G);
		case 15:
			return Note.natural(1, Tone.A);
		case 16:
			return Note.sharp(1, Tone.A);
		case 17:
			return Note.natural(1, Tone.B);
		case 18:
			return Note.natural(1, Tone.C);
		case 19:
			return Note.sharp(1, Tone.C);
		case 20:
			return Note.natural(1, Tone.D);
		case 21:
			return Note.sharp(1, Tone.D);
		case 22:
			return Note.natural(1, Tone.E);
		case 23:
			return Note.natural(1, Tone.F);
		case 24:
			return Note.sharp(2, Tone.F);
		default:
			return null;
		}
	}
	
}
