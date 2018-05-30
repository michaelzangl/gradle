/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state.mirror

import org.gradle.api.internal.changedetection.state.FileContentSnapshot
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.hash.TestFileHasher
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.UsesNativeServices
import org.junit.Rule
import spock.lang.Specification

@UsesNativeServices
class MirrorUpdatingDirectoryWalkerTest extends Specification {
    @Rule
    public final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()

    def fileHasher = new TestFileHasher()
    def walker = new MirrorUpdatingDirectoryWalker(fileHasher)

    def "basic directory walking works"() {
        given:
        def rootDir = tmpDir.createDir("root")
        def rootTextFile = rootDir.file("a.txt").createFile()
        def nestedTextFile = rootDir.file("a/b/c.txt").createFile()
        def notTextFile = rootDir.file("a/b/c.html").createFile()
        def excludedFile = rootDir.file("subdir1/a/b/c.html").createFile()
        def notUnderRoot = tmpDir.createDir("root2").file("a.txt").createFile()
        def doesNotExist = rootDir.file("b.txt")

        def patterns = new PatternSet()
        patterns.include("**/*.txt")
        patterns.exclude("subdir1/**")

        def visited = []

        def root = new PhysicalDirectorySnapshot("some")
        when:
        walker.walkDir(rootDir.toPath(), root)
        root.visitTree(new PhysicalFileVisitor() {
            @Override
            void visit(String name, Iterable<String> relativePath, FileContentSnapshot content) {
                visited << new File(rootDir, relativePath.join("/")).absolutePath
            }
        }, new ArrayDeque<String>())

        then:
        visited.contains(rootTextFile.absolutePath)
        visited.contains(rootTextFile.absolutePath)
        visited.contains(nestedTextFile.absolutePath)
        visited.contains(notTextFile.absolutePath)
        visited.contains(excludedFile.absolutePath)
        !visited.contains(notUnderRoot.absolutePath)
        !visited.contains(doesNotExist.absolutePath)
    }
}
