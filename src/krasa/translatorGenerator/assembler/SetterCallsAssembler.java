package krasa.translatorGenerator.assembler;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiTreeUtil;
import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.generator.SetterCallsGenerator;

/**
 * @author Vojtech Krasa
 */
public class SetterCallsAssembler extends Assembler {

	private final PsiLocalVariable localVariable;
	private final PsiFacade psiFacade;
	private final Context context;
	private PsiElementFactory elementFactory;

	public SetterCallsAssembler(PsiLocalVariable variable, PsiFacade psiFacade, Context context) {
		super(psiFacade, context);
		this.localVariable = variable;
		this.psiFacade = psiFacade;
		this.context = context;
		elementFactory = JavaPsiFacade.getElementFactory(psiFacade.getProject());
	}

	public void generateSetterCalls() {
		SetterCallsGenerator setterCallsGenerator = new SetterCallsGenerator(localVariable, context);
		PsiDeclarationStatement statement = PsiTreeUtil.getTopmostParentOfType(localVariable,
				PsiDeclarationStatement.class);
		PsiCodeBlock parent = (PsiCodeBlock) statement.getParent();

		for (String s1 : setterCallsGenerator.generateSetterCalls()) {
			PsiStatement setter = elementFactory.createStatementFromText(s1, localVariable.getOriginalElement());
			parent.addAfter(setter, statement);
			psiFacade.shortenClassReferences(setter);
			psiFacade.reformat(setter);
		}

	}

}
