package krasa.translatorGenerator.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.PsiFacade;
import krasa.translatorGenerator.assembler.TranslatorMethodAssembler;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

/**
 * @author Vojtech Krasa
 */
public class GenerateTranslatorMethodAction extends TranslatorAction {

   protected void execute(AnActionEvent e, PsiFacade psiFacade) {
      PsiElement psiElement = psiFacade.getPsiElement(e);
      PsiMethod psiMethod = getPsiMethod(psiElement);
      Context context = new Context(e.getProject(), EDITOR.getData(e.getDataContext()));

      if (psiMethod != null) {
         PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
         if (parameters.length == 1) {
            generateTranslatorMethod(psiMethod, psiFacade, context);
         }
      }
   }

   private void generateTranslatorMethod(final PsiMethod psiMethod, final PsiFacade psiFacade, final Context context) {
      new WriteCommandAction.Simple(psiMethod.getProject()) {

         @Override
         protected void run() throws Throwable {
            new TranslatorMethodAssembler(psiMethod, psiFacade, context).assemble();
         }
      }.execute();
   }

   @Override
   public void update(AnActionEvent e) {
      super.update(e);

      PsiFacade psiFacade = new PsiFacade(e.getProject());
      PsiElement psiElement = psiFacade.getPsiElement(e);
      PsiMethod psiMethod = getPsiMethod(psiElement);
      // Make the generation active
      e.getPresentation()
            .setEnabledAndVisible(isEnabled(psiMethod));
   }

   private boolean isEnabled(PsiMethod psiMethod) {
      boolean result = false;

      if (psiMethod != null && psiMethod.getParameterList().getParametersCount() == 1 && !PsiType.VOID.equals(psiMethod.getReturnType())) {
         if (psiMethod.getBody() == null  || psiMethod.getBody().getStatements().length < 2) {
            result = true;
         }
      }

      return result;
   }

   private PsiMethod getPsiMethod(PsiElement psiElement) {
      if (psiElement != null) {
         PsiElement parent = psiElement.getParent();
         if (parent != null) {
            if (parent instanceof PsiMethod) {
               return (PsiMethod) parent;
            } else {
               return getPsiMethod(parent);
            }
         }
      }
      return null;
   }
}
