package com.bumptech.glide.annotation.compiler;

import com.bumptech.glide.annotation.GlideModule;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Generates Indexer classes annotated with {@link Index} for all
 * {@code LibraryGlideModule}s.
 */
final class LibraryModuleProcessor {
  private final ProcessorUtil processorUtil;
  private final IndexerGenerator indexerGenerator;

  LibraryModuleProcessor(ProcessorUtil processorUtil, IndexerGenerator indexerGenerator) {
    this.processorUtil = processorUtil;
    this.indexerGenerator = indexerGenerator;
  }

  boolean processModules(RoundEnvironment env) {
     // Order matters here, if we find an Indexer below, we return before writing the root module.
    // If we fail to add to appModules before then, we might accidentally skip a valid RootModule.
    List<TypeElement> libraryGlideModules = new ArrayList<>();
    //找到{@code GlideModule} 标记的所有类，都是继承于 LibraryGlideModule
    for (TypeElement element : processorUtil.getElementsFor(GlideModule.class, env)) {
      // Root elements are added separately and must be checked separately because they're sub
      // classes of LibraryGlideModules.
      if (processorUtil.isAppGlideModule(element)) {
        //应用 AppGlideModule也是继承的LibraryGlideModule，因此要提前区分开，单独只处理继承 LibraryGlideModule的module
        continue;
      } else if (!processorUtil.isLibraryGlideModule(element)) {
        //处理 GlideModule注解未用在LibraryGlideModule或者AppGlideModule的异常情况
        throw new IllegalStateException("@GlideModule can only be applied to LibraryGlideModule"
            + " and AppGlideModule implementations, not: " + element);
      }

      //存放所有的 LibraryGlideModule(由{@code GlideModule}注解)
      libraryGlideModules.add(element);
    }

    processorUtil.debugLog("got child modules: " + libraryGlideModules);
    if (libraryGlideModules.isEmpty()) {
      return false;
    }

    TypeSpec indexer = indexerGenerator.generate(libraryGlideModules);
    processorUtil.writeIndexer(indexer);
    processorUtil.debugLog("Wrote an Indexer this round, skipping the app module to ensure all "
        + "indexers are found");
     // If I write an Indexer in a round in the target package, then try to find all classes in
    // the target package, my newly written Indexer won't be found. Since we wrote a class with
    // an Annotation handled by this processor, we know we will be called again in the next round
    // and we can safely wait to write our AppGlideModule until then.
    return true;
  }

  Set<String> getSupportedAnnotationTypes() {
    //Collections.singleton创建一个列表，只包含了某个元素的集合，意思是可以有多个相同元素
    return Collections.singleton(GlideModule.class.getName());
  }
}
