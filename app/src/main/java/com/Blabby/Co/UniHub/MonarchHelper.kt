package com.Blabby.Co.UniHub

import io.github.dingyi222666.monarch.languages.BatLanguage
import io.github.dingyi222666.monarch.languages.CppLanguage
import io.github.dingyi222666.monarch.languages.CsharpLanguage
import io.github.dingyi222666.monarch.languages.CssLanguage
import io.github.dingyi222666.monarch.languages.DartLanguage
import io.github.dingyi222666.monarch.languages.GoLanguage
import io.github.dingyi222666.monarch.languages.HtmlLanguage
import io.github.dingyi222666.monarch.languages.JavascriptLanguage
import io.github.dingyi222666.monarch.languages.JavaLanguage
import io.github.dingyi222666.monarch.languages.KotlinLanguage
import io.github.dingyi222666.monarch.languages.LuaLanguage
import io.github.dingyi222666.monarch.languages.MarkdownLanguage
import io.github.dingyi222666.monarch.languages.PascalLanguage
import io.github.dingyi222666.monarch.languages.PerlLanguage
import io.github.dingyi222666.monarch.languages.PhpLanguage
import io.github.dingyi222666.monarch.languages.PythonLanguage
import io.github.dingyi222666.monarch.languages.RubyLanguage
import io.github.dingyi222666.monarch.languages.RustLanguage
import io.github.dingyi222666.monarch.languages.ScalaLanguage
import io.github.dingyi222666.monarch.languages.ShellLanguage
import io.github.dingyi222666.monarch.languages.SqlLanguage
import io.github.dingyi222666.monarch.languages.SwiftLanguage
import io.github.dingyi222666.monarch.languages.TypescriptLanguage
import io.github.dingyi222666.monarch.languages.XmlLanguage
import io.github.dingyi222666.monarch.languages.YamlLanguage
import io.github.rosemoe.sora.langs.monarch.registry.MonarchGrammarRegistry
import io.github.rosemoe.sora.langs.monarch.registry.dsl.monarchLanguages

object MonarchHelper {
    fun setupGrammars() {
        val registry = MonarchGrammarRegistry.INSTANCE
        if (registry.findGrammar("source.java") != null) return

        val allDefinitions = monarchLanguages {
            language("source.java") { monarchLanguage = JavaLanguage }
            language("source.kotlin") { monarchLanguage = KotlinLanguage }
            language("source.python") { monarchLanguage = PythonLanguage }
            language("source.javascript") { monarchLanguage = JavascriptLanguage }
            language("source.typescript") { monarchLanguage = TypescriptLanguage }
            language("text.html.basic") { monarchLanguage = HtmlLanguage }
            language("source.css") { monarchLanguage = CssLanguage }
            language("text.xml") { monarchLanguage = XmlLanguage }
            language("source.yaml") { monarchLanguage = YamlLanguage }
            language("source.shell") { monarchLanguage = ShellLanguage }
            language("source.cpp") { monarchLanguage = CppLanguage }
            language("source.cs") { monarchLanguage = CsharpLanguage }
            language("source.rust") { monarchLanguage = RustLanguage }
            language("source.go") { monarchLanguage = GoLanguage }
            language("source.ruby") { monarchLanguage = RubyLanguage }
            language("source.swift") { monarchLanguage = SwiftLanguage }
            language("source.php") { monarchLanguage = PhpLanguage }
            language("source.sql") { monarchLanguage = SqlLanguage }
            language("source.lua") { monarchLanguage = LuaLanguage }
            language("source.scala") { monarchLanguage = ScalaLanguage }
            language("source.dart") { monarchLanguage = DartLanguage }
            language("text.html.markdown") { monarchLanguage = MarkdownLanguage }
            language("source.bat") { monarchLanguage = BatLanguage }
            language("source.perl") { monarchLanguage = PerlLanguage }
            language("source.pascal") { monarchLanguage = PascalLanguage }
        }.build()

        for (def in allDefinitions) {
            try {
                registry.loadGrammar(def)
            } catch (e: Exception) {
                android.util.Log.e("MonarchHelper", "Failed to load grammar: ${def.scopeName}", e)
            }
        }
    }
}
