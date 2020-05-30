/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi;

import static guru.nidi.codeassert.junit.CodeAssertMatchers.hasNoCheckstyleIssues;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

import guru.nidi.codeassert.checkstyle.CheckstyleAnalyzer;
import guru.nidi.codeassert.checkstyle.CheckstyleResult;
import guru.nidi.codeassert.checkstyle.StyleChecks;
import guru.nidi.codeassert.checkstyle.StyleEventCollector;
import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.config.In;

public class SourceCodeTest {

    @Test
    public void testCheckstyle() {
        checkstyleGoogle();
        checkstyleSun();
    }

    public void checkstyleGoogle() {
        // Analyze all sources in src/main/java
        AnalyzerConfig config = AnalyzerConfig.maven().main();

        // Only treat issues with severity WARNING or higher
        StyleEventCollector collector = new StyleEventCollector().severity(SeverityLevel.WARNING)
                .just(In.everywhere().ignore("abbreviation", "empty.line.separator", "indentation", "import",
                        "javadoc.paragraph", "javadoc.missing", "maxLineLen", "name", "summary",
                        "variable.declaration.usage.distance"));

        // use google checks, but adjust max line length
        final StyleChecks checks = StyleChecks.google().maxLineLen(120);
        final CheckstyleResult result = new CheckstyleAnalyzer(config, checks, collector).analyze();
        assertThat(result, hasNoCheckstyleIssues());
    }

    public void checkstyleSun() {
        // Analyze all sources in src/main/java
        AnalyzerConfig config = AnalyzerConfig.maven().main();

        // Only treat issues with severity WARNING or higher
        StyleEventCollector collector = new StyleEventCollector().severity(SeverityLevel.WARNING)
                .just(In.everywhere().ignore("assignment.inner.avoid", "design.forExtension", "final.parameter",
                        "hidden.field", "import.unused", "inline.conditional.avoid", "interface.type",
                        "javadoc.missing", "javadoc.noPeriod", "magic.number", "maxLineLen", "maxLen.method",
                        "maxParam", "name.invalidPattern", "noNewlineAtEOF", "variable.notPrivate", "ws.followed"));

        // use sun checks also , but adjust max line length
        final StyleChecks checks = StyleChecks.sun().maxLineLen(120);
        final CheckstyleResult result = new CheckstyleAnalyzer(config, checks, collector).analyze();
        assertThat(result, hasNoCheckstyleIssues());
    }
}
