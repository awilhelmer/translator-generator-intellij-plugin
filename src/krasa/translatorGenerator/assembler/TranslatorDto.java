package krasa.translatorGenerator.assembler;

import com.intellij.psi.PsiType;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Vojtech Krasa
 */
public class TranslatorDto {

   private final PsiType from;

   private final PsiType to;

   private boolean isStatic;

   public boolean processed;

   public TranslatorDto(PsiType from, PsiType to, boolean isStatic) {
      this.from = from;
      this.to = to;
      this.isStatic = isStatic;
   }

   public PsiType getFrom() {
      return from;
   }

   public PsiType getTo() {
      return to;
   }

   public boolean isStatic() {
      return isStatic;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      TranslatorDto that = (TranslatorDto) o;

      if (from != null ? !from.equals(that.from) : that.from != null) {
         return false;
      }
      if (to != null ? !to.equals(that.to) : that.to != null) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = from != null ? from.getCanonicalText().hashCode() : 0;
      result = 31 * result + (to != null ? to.getCanonicalText().hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this).append("from", from).append("to", to).append("processed", processed).toString();
   }
}
