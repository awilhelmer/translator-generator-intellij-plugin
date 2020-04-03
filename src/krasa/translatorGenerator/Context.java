package krasa.translatorGenerator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.refactoring.typeCook.Util;
import com.intellij.util.containers.ContainerUtil;
import krasa.translatorGenerator.assembler.TranslatorDto;

import java.util.Set;

/**
 * @author Vojtech Krasa
 */
public class Context {

   private static final Logger LOG = Logger.getInstance(Context.class.getName());

   public Set<TranslatorDto> scheduled = ContainerUtil.newConcurrentSet();

   public boolean replaceMethods = true;

   private Project project;

   private Editor editor;

   public Context(Project project, Editor editor) {
      this.project = project;
      this.editor = editor;
   }

   public Project getProject() {
      return project;
   }

   public boolean shouldTranslate(PsiType getter, PsiType setter) {
      if (HACK.isTranslationExcluded(getter)) {
         return false;
      }
      if (!getter.getCanonicalText().equals(setter.getCanonicalText())) {
         return true;
      }
      return HACK.shouldTranslate(getter.getCanonicalText());
   }

   public void scheduleTranslator(PsiType from, PsiType to, boolean  isStatic) {
      add(new TranslatorDto(from, to, isStatic));
   }

   private void add(TranslatorDto translatorDto) {
      if (!scheduled.contains(translatorDto)) {
         LOG.info("scheduling " + translatorDto);
         scheduled.add(translatorDto);
      }
   }

   public void scheduleTranslator(PsiClass fromImpl, PsiClass toImpl, boolean  isStatic) {
      TranslatorDto translatorDto = new TranslatorDto(Util.getType(fromImpl), Util.getType(toImpl), isStatic);
      add(translatorDto);
   }

   public boolean hasAnyScheduled() {
      for (TranslatorDto translatorDto : scheduled) {
         if (!translatorDto.processed) {
            return true;
         }
      }
      return false;
   }

   public void markTranslatorMethodProcessed(PsiType from, PsiType to, boolean  isStatic) {
      TranslatorDto e = new TranslatorDto(from, to, isStatic);
      e.processed = true;
      scheduled.add(e);
   }

   public Editor getEditor() {
      return editor;
   }

}
