package krasa.translatorGenerator.generator;

import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.siyeh.ig.psiutils.CollectionUtils.isCollectionClassOrInterface;
import static krasa.translatorGenerator.Utils.capitalize;

/**
 * @author Vojtech Krasa
 */
public class MethodCodeGenerator {

   private static final Logger LOG = Logger.getInstance("#" + MethodCodeGenerator.class.getName());

   private PsiClass from;

   private PsiClass to;

   private Context context;

   private PsiMethod psiMethod;
   private Boolean isStatic;

   public MethodCodeGenerator() {

   }

   public MethodCodeGenerator(PsiClass from, PsiClass to, Context context, PsiMethod psiMethod, Boolean isStatic) {
      this.from = from;
      this.to = to;
      this.context = context;
      this.psiMethod = psiMethod;
      this.isStatic = isStatic;
   }

   public String translatorMethod() {

      //this.context.

      String fromClassName = from.getName();
      String fromQualifiedName = from.getQualifiedName();
      String toQualifiedName = to.getQualifiedName();
      String inputName = "input";
      String modifier = "public";


      String s = null;
      if (psiMethod != null) {

         modifier = psiMethod.getModifierList().getText();
         if (modifier.contains("static")) {
            isStatic = true;
         }
         if (psiMethod.getParameterList().getParametersCount() == 1 && psiMethod.getReturnTypeElement() != null) {
            PsiParameter psiParameter = psiMethod.getParameterList().getParameters()[0];
            s = String.format("%s %s %s(%s %s) {\n", modifier, psiMethod.getReturnTypeElement().getText(), psiMethod.getName(),
                  psiParameter.getType().getCanonicalText(), psiParameter.getName());
            inputName = psiMethod.getParameterList().getParameters()[0].getName();
         }
      }
      if (isStatic != null && isStatic) {
         modifier += " static";
      }
      if (s == null) {
         s = String.format("%s %s map%s(%s %s) {\n", modifier, toQualifiedName, fromClassName, fromQualifiedName, inputName);

      }


      if (to.isEnum()) {
         s += String.format("return %s.valueOf(%s);\n", toQualifiedName, inputName);
         s += "}";
         return s;
      }
      s += String.format("if(%s==null){return null;}\n", inputName);

      PsiClass[] toImpls = getImplementingClasses(to);
      PsiClass[] fromImpls = getImplementingClasses(from);

      if (toImpls.length == 1) {
         if (!PsiUtil.isAbstractClass(to)) {
            if (fromImpls.length == 1) {
               // to=1, from=1
               s = translatorBody(s, fromImpls[0], to, inputName);
            } else {
               // to=1, from=N
               s = instanceOfTranslator(s, fromImpls, to);
            }
         } else {
            s += "\n //TODO\n";
         }
      } else {
         if (fromImpls.length == 1) {
            // to=N, from=1
            PsiClass fromImpl = fromImpls[0];
            for (int i = 0; i < toImpls.length; i++) {
               PsiClass toImpl = toImpls[i];
               if (i > 0 && toImpls.length > 2) {
                  s += "else if(TODO){\n";
                  s += String.format("return map%sTo%s(%s);}", capitalize(fromImpl.getName()), toImpl.getName(), inputName);
               } else if (i > 0 && toImpls.length == 2) {
                  s += "else {\n";
                  s += String.format("return map%sTo%s(%s);}", capitalize(fromImpl.getName()), toImpl.getName(), inputName);
               } else {
                  s += "if {\n";
                  s += String.format("return map%sTo%s(%s);}", capitalize(fromImpl.getName()), toImpl.getName(), inputName);
               }


               context.scheduleTranslator(fromImpl, toImpl, isStatic);
               s += "}\n";
            }
         } else {
            if (canTranslate(fromImpls, toImpls)) {
               // to=N, from=N
               s = instanceOfTranslator(s, toImpls, fromImpls);
            } else {
               // to=M, from=N hardcore
               s += "\n //TODO M2N\n";
            }

         }
      }

      s += "}";
      return s;
   }

   public String arrayTranslatorMethod() {
      String fromClassName = from.getName();
      String fromQualifiedName = from.getQualifiedName();
      String toQualifiedName = to.getQualifiedName();

      String s = String.format("public %s[] map%sArray(%s[] input) { ", toQualifiedName, fromClassName, fromQualifiedName);

      s += String.format("%s[] result = new %s[input.length];", to.getQualifiedName(), to.getQualifiedName());
      s += "for (int i = 0; i < input.length; i++) {";

      PsiClassType fromTypeParameter = JavaPsiFacade.getInstance(from.getProject()).getElementFactory().createType(from);
      PsiClassType toTypeParameter = JavaPsiFacade.getInstance(to.getProject()).getElementFactory().createType(to);
      if (context.shouldTranslate(toTypeParameter, fromTypeParameter)) {
         context.scheduleTranslator(fromTypeParameter, toTypeParameter, isStatic);
         s += String.format("result[i] = map%s(input[i]);", fromTypeParameter.getPresentableText());
      } else {
         s += "result[i] = input[i];";
      }

      s += "}";

      s += "return result;}";
      return s;
   }

   private String instanceOfTranslator(String s, PsiClass[] toImpls, PsiClass[] fromImpls) {
      for (PsiClass fromImpl : fromImpls) {
         PsiClass to = getMatching(toImpls, fromImpl);
         s = instanceOfTranslator(s, fromImpl, to);
      }
      return s;
   }

   private String instanceOfTranslator(String s, PsiClass[] fromImpls, PsiClass to) {
      for (int i1 = 0; i1 < fromImpls.length; i1++) {
         PsiClass fromImpl = fromImpls[i1];
         s = instanceOfTranslator(s, fromImpl, to);
      }
      return s;
   }

   private String instanceOfTranslator(String s, PsiClass fromImpl, PsiClass to) {
      String inputVariable = "input";
      if (fromImpl != from) {
         inputVariable = "input" + fromImpl.getName();
         s += "if(input instanceof " + fromImpl.getQualifiedName() + "){";
         s += fromImpl.getQualifiedName() + " " + inputVariable + " = (" + fromImpl.getQualifiedName() + ") input;";
         s = translatorBody(s, fromImpl, to, inputVariable);
         s += "}";
      } else {
         if (!PsiUtil.isAbstractClass(to)) {
            inputVariable = "input";
            s = translatorBody(s, fromImpl, to, inputVariable);
         } else {
            s += "throw new java.lang.IllegalArgumentException(\"unable to translate:\"+ input);";
         }
      }
      return s;
   }

   @NotNull
   private PsiClass getMatching(PsiClass[] toImpls, PsiClass fromImpl) {
      for (PsiClass toImpl : toImpls) {
         if (toImpl.getName().equals(fromImpl.getName())) {
            return fromImpl;
         }
      }
      return null;
   }

   private boolean canTranslate(PsiClass[] fromImpls, PsiClass[] toImpls) {
      if (fromImpls.length != toImpls.length) {
         return false;
      }
      Set<String> fromFieldsMap = names(fromImpls);
      Set<String> toStringPsiFieldMap = names(toImpls);

      return fromFieldsMap.equals(toStringPsiFieldMap);
   }

   private Set<String> names(PsiClass[] toImpls) {
      HashSet<String> strings = new HashSet<String>();
      for (PsiClass toImpl : toImpls) {
         strings.add(toImpl.getName());
      }
      return strings;
   }

   private PsiClass[] getImplementingClasses(PsiClass to1) {
      PsiElement[] toImpls = createImplementationsSearcher().searchImplementations(to1, context.getEditor(), true, false);
      toImpls = ArrayUtil.reverseArray(toImpls);
      PsiClass[] psiClasses = new PsiClass[toImpls.length];
      for (int i = 0; i < toImpls.length; i++) {
         psiClasses[i] = (PsiClass) toImpls[i];
      }
      return psiClasses;
   }

   private String translatorBody(String s, PsiClass from, PsiClass to, String inputVariable) {
      PsiField[] toFields = to.getAllFields();
      PsiField[] fromFields = from.getAllFields();
      s += to.getQualifiedName() + " " + "result = new " + to.getQualifiedName() + "();";
      s = translateFields(s, fromFields, toFields, inputVariable);
      s += "return result;";
      return s;
   }

   private String translateFields(String s, PsiField[] fromFields, PsiField[] toFields, String inputVariable) {
      int lastTodo = -1;
      Map<String, PsiField> fromFieldsMap = getStringPsiFieldMap(fromFields);

      for (int i = 0; i < toFields.length; i++) {
         PsiField toField = toFields[i];
         if (Utils.isStatic(toField)) {
            continue;
         }
         PsiMethod toSetter = Utils.setter(toField);
         PsiMethod toGetter = Utils.getter(toField);
         PsiField fromField = getField(fromFieldsMap, toField);
         PsiMethod fromGetter = Utils.getter(fromField);

         if (toSetter == null && toGetter != null && fromGetter != null) {
            if (isCollectionClassOrInterface(toGetter.getReturnType())) {
               s = handleCollection(s, fromField, toField, toGetter, inputVariable, fromGetter);
            } else {
               s += " //TODO result." + toField.getName() + "\n";
            }
         } else if (toSetter != null && fromField == null) {
            s += "\n//result." + toSetter.getName() + "(" + inputVariable + ".get" + capitalize(toField.getName()) + "());\n";
         } else if (toSetter != null && fromGetter != null) {
            String getter = getter(fromGetter, toSetter, inputVariable);
            s += "result." + toSetter.getName() + "(" + getter + ");";
         } else if (toSetter != null) {   //fromField != null && fromGetter == null
            s += "\n//result." + toSetter.getName() + "(" + inputVariable + ".get" + capitalize(toField.getName()) + "());\n";
         } else {
            if (lastTodo != i - 1) {
               s += "\n";
            }
            lastTodo = i;
            s += " //TODO result." + toField.getName() + "\n";
         }
      }
      return s.replace("\n\n", "\n");
   }

   private PsiField getField(Map<String, PsiField> fromFieldsMap, PsiField toField) {
      PsiField psiField = fromFieldsMap.get(toField.getName());
      if (psiField == null) {
         PsiField lastField = null;
         Double lastPercentage = null;
         for (String fromFieldName : fromFieldsMap.keySet()) {
            PsiField checkField = fromFieldsMap.get(fromFieldName);
            if (checkField.getType().isAssignableFrom(toField.getType())) {
               double percentage = compareStrings(toField.getName(), fromFieldName);
               if (percentage >= 0.4d) {
                  if (lastField == null || (lastPercentage < percentage)) {
                     lastField = checkField;
                     lastPercentage = percentage;
                  }
               }
            }

         }
         psiField = lastField;
      }
      return psiField;
   }



   protected Map<String, PsiField> getStringPsiFieldMap(PsiField[] fromFields) {
      Map<String, PsiField> fromFieldsMap = new HashMap<String, PsiField>();
      for (PsiField fromField : fromFields) {
         fromFieldsMap.put(fromField.getName(), fromField);
      }
      return fromFieldsMap;
   }

   protected static ImplementationSearcher createImplementationsSearcher() {
      return new ImplementationSearcher() {

         protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements, int offset) {
            return MethodCodeGenerator.filterElements(targetElements);
         }
      };
   }

   private static PsiElement[] filterElements(final PsiElement[] targetElements) {
      final Set<PsiElement> unique = new LinkedHashSet<PsiElement>(Arrays.asList(targetElements));
      for (final PsiElement elt : targetElements) {
         ApplicationManager.getApplication().runReadAction(new Runnable() {

            @Override
            public void run() {
               final PsiFile containingFile = elt.getContainingFile();
               LOG.assertTrue(containingFile != null, elt);
               PsiFile psiFile = containingFile.getOriginalFile();
               if (psiFile.getVirtualFile() == null) {
                  unique.remove(elt);
               }
            }
         });
      }
      // special case for Python (PY-237)
      // if the definition is the tree parent of the target element, filter
      // out the target element
      for (int i = 1; i < targetElements.length; i++) {
         final PsiElement targetElement = targetElements[i];
         if (ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {

            @Override
            public Boolean compute() {
               return PsiTreeUtil.isAncestor(targetElement, targetElements[0], true);
            }
         })) {
            unique.remove(targetElements[0]);
            break;
         }
      }
      return PsiUtilCore.toPsiElementArray(unique);
   }

   protected String handleCollection(String s, PsiField fromField, PsiField toField, PsiMethod toGetter, String methodInputVariable,
         PsiMethod fromGetter) {
      if (toGetter.getReturnType() instanceof PsiClassReferenceType) {
         PsiClassReferenceType toGetterType = (PsiClassReferenceType) toGetter.getReturnType();
         String toType = toGetterType.getCanonicalText();
         PsiType[] toGetterTypeParameters = toGetterType.getReference().getTypeParameters();

         PsiClassReferenceType fromGetterType = (PsiClassReferenceType) fromGetter.getReturnType();
         String fromType = fromGetterType.getCanonicalText();
         PsiType[] fromGetterTypeParameters = fromGetterType.getReference().getTypeParameters();

         String inputVariable = "input" + capitalize(fromField.getName());
         s += fromType + " " + inputVariable + " =  " + methodInputVariable + "." + fromGetter.getName() + "();";
         String resultVariable = "result" + capitalize(toField.getName());
         s += toType + " " + resultVariable + " =  result." + toGetter.getName() + "();";

         String objectType = "Object";
         String itemGetter = "item";
         if (toGetterTypeParameters.length == 1 && fromGetterTypeParameters.length == 1) { // List/Set
            PsiType toGetterTypeParameter = toGetterTypeParameters[0];
            PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];
            objectType = fromGetterTypeParameter.getCanonicalText();
            if (context.shouldTranslate(toGetterTypeParameter, fromGetterTypeParameter)) {
               context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter, isStatic);
               itemGetter = "map" + fromGetterTypeParameter.getPresentableText() + "(item)";
            }
         } else if (toGetterTypeParameters.length == 2 && fromGetterTypeParameters.length == 2) { // Map
            inputVariable += ".entrySet()";
            PsiType toGetterTypeParameter = toGetterTypeParameters[0];
            PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];
            objectType =
                  "java.util.Map.Entry<" + fromGetterTypeParameters[0].getCanonicalText() + "," + fromGetterTypeParameters[1].getCanonicalText()
                        + ">";

            String key = "item.getKey()";
            String value = "item.getValue()";
            if (context.shouldTranslate(toGetterTypeParameters[0], fromGetterTypeParameters[0])) {
               context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter, isStatic);
               key = "map" + fromGetterTypeParameter.getPresentableText() + "(item.getKey())";
            }
            if (context.shouldTranslate(toGetterTypeParameters[1], fromGetterTypeParameters[1])) {
               context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter, isStatic);
               value = "map" + fromGetterTypeParameter.getPresentableText() + "(item.getValue())";
            }
            itemGetter = key + ", " + value;
         }
         s += "for(" + objectType + " item : " + inputVariable + "){";
         if (toGetterTypeParameters.length == 2) {
            s += resultVariable + ".put(" + itemGetter + ");";
         } else {
            s += resultVariable + ".add(" + itemGetter + ");";
         }
         s += "}";
      } else {
         s += "\n//todo " + toGetter.getReturnType() + "\n";
      }
      return s;
   }

   protected String getter(PsiMethod getter, PsiMethod setter, String inputVariable) {
      // todo
      PsiType getterType = getter.getReturnType();
      PsiParameter psiParameter = setter.getParameterList().getParameters()[0];
      PsiType setterType = psiParameter.getType();

      if (getterType instanceof PsiPrimitiveType) {
         return inputVariable + "." + getter.getName() + "()";
      } else {
         if (getterType instanceof PsiClassReferenceType) {
            PsiClassReferenceType getterRefType = (PsiClassReferenceType) getterType;
            PsiClassReferenceType setterRefType = (PsiClassReferenceType) setterType;
            if (context.shouldTranslate(getterRefType, setterRefType)) {
               String className = getterRefType.getClassName();
               context.scheduleTranslator(getterType, setterType, isStatic);
               return "map" + className + "(" + inputVariable + "." + getter.getName() + "())";
            } else {
               return inputVariable + "." + getter.getName() + "()";
            }
         } else if (getterType instanceof PsiArrayType) {
            PsiArrayType psiArrayType = (PsiArrayType) getterType;
            // todo check type
            PsiArrayType setterRefType = (PsiArrayType) setterType;
            if (context.shouldTranslate(psiArrayType.getComponentType(), setterRefType.getComponentType())) {
               String className = psiArrayType.getComponentType().getPresentableText() + "Array";
               context.scheduleTranslator(getterType, setterType, isStatic);
               return "map" + className + "(" + inputVariable + "." + getter.getName() + "())";
            } else {
               return inputVariable + "." + getter.getName() + "()";
            }
         } else {
            return "\n //todo " + getterType + "\n";
         }
      }
   }


   private double compareStrings(String str1, String str2) {
      List<String> pairs1 = wordLetterPairs(str1.toUpperCase());
      List<String> pairs2 = wordLetterPairs(str2.toUpperCase());

      int intersection = 0;
      int union = pairs1.size() + pairs2.size();

      for (int i = 0; i < pairs1.size(); i++) {
         for (int j = 0; j < pairs2.size(); j++) {
            if (pairs1.get(i).equals(pairs2.get(j))) {
               intersection++;
               pairs2.remove(j);//Must remove the match to prevent "GGGG" from appearing to match "GG" with 100% success

               break;
            }
         }
      }

      return (2.0 * intersection) / union;
   }

   /// <summary>
   /// Gets all letter pairs for each
   /// individual word in the string
   /// </summary>
   /// <param name="str"></param>
   /// <returns></returns>
   private List<String> wordLetterPairs(String str) {
      List<String> AllPairs = new ArrayList<String>();

      // Tokenize the string and put the tokens/words into an array
      String[] Words = str.split(" ");

      // For each word
      for (int w = 0; w < Words.length; w++) {
         if (Words[w] != null && !Words[w].isEmpty()) {
            // Find the pairs of characters
            String[] PairsInWord = letterPairs(Words[w]);

            for (int p = 0; p < PairsInWord.length; p++) {
               AllPairs.add(PairsInWord[p]);
            }
         }
      }

      return AllPairs;
   }

   /// <summary>
   /// Generates an array containing every
   /// two consecutive letters in the input string
   /// </summary>
   /// <param name="str"></param>
   /// <returns></returns>
   private String[] letterPairs(String str) {
      int numPairs = str.length() - 1;

      String[] pairs = new String[numPairs];

      for (int i = 0; i < numPairs; i++) {
         pairs[i] = str.substring(i, i + 2);
      }

      return pairs;
   }
}
