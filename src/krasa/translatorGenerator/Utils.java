package krasa.translatorGenerator;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtil;

import java.util.List;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

/**
 * @author Vojtech Krasa
 */
public class Utils {

	private static String camelCase(String s) {
		return toLowerCase(s.charAt(0)) + s.substring(1);
	}

	public static PsiMethod getter(PsiField field) {
		if (field == null) {
			return null;
		}
		List<PsiMethod> setters = PropertyUtil.getGetters(field.getContainingClass(), field.getName());
		if (setters.size() == 1) {
			return setters.get(0);
		}
		return null;
	}

	public static PsiMethod setter(PsiField field) {
		List<PsiMethod> setters = PropertyUtil.getSetters(field.getContainingClass(), field.getName());
		if (setters.size() == 1) {
			return setters.get(0);
		}
		return null;
	}

	public static String capitalize(String fieldName) {
		return toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
	}

	public static boolean isStatic(PsiField field) {
		return field.getModifierList() != null && field.getModifierList().hasExplicitModifier("static");
	}
}
