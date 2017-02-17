package krasa.translatorGenerator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import krasa.translatorGenerator.generator.MethodCodeGenerator;

/**
 * @author Vojtech Krasa
 */
public class PsiBuilder {

   private Context context;

   private PsiElementFactory elementFactory;

   public PsiBuilder(Context context, Project project) {
      this.context = context;
      elementFactory = JavaPsiFacade.getElementFactory(project);
   }

   public PsiClass createTranslatorClass(PsiClass from, PsiClass to) {
      return elementFactory.createClassFromText("public static class " + from.getName() + "To" + to.getName() + "Translator {}", null)
            .getInnerClasses()[0];
   }

   public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to, PsiMethod psiMethod) {
      PsiMethod methodFromText = elementFactory.createMethodFromText(new MethodCodeGenerator(from, to, context, psiMethod, false).translatorMethod(), builderClass);
      context.markTranslatorMethodProcessed(PsiUtil.getTypeByPsiElement(from), PsiUtil.getTypeByPsiElement(to), false);
      return methodFromText;
   }

   private PsiMethod createArrayTranslatorMethod(PsiClass builderClass, PsiClass from, PsiClass to, PsiMethod psiMethod) {
      PsiMethod methodFromText = elementFactory.createMethodFromText(new MethodCodeGenerator(from, to, context, psiMethod,false).arrayTranslatorMethod(),
            builderClass);
      context.markTranslatorMethodProcessed(PsiUtil.getTypeByPsiElement(from), PsiUtil.getTypeByPsiElement(to), false);
      return methodFromText;
   }

   public PsiMethod createTranslatorMethod(PsiClass builderClass, PsiType fromType, PsiType toType, PsiMethod psiMethod) {
      if (fromType instanceof PsiArrayType) {
         fromType = fromType.getDeepComponentType();
         toType = toType.getDeepComponentType();
         PsiClassType from = (PsiClassType) fromType;
         PsiClassType to = (PsiClassType) toType;
         return createArrayTranslatorMethod(builderClass, from.resolve(), to.resolve(), psiMethod);
      }

      PsiClassType from = (PsiClassType) fromType;
      PsiClassType to = (PsiClassType) toType;
      return createTranslatorMethod(builderClass, from.resolve(), to.resolve(), psiMethod);
   }
}
