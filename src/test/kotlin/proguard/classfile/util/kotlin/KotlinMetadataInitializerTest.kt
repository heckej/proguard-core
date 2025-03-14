/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.classfile.util.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.MethodSignature
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinDeclarationContainerFilter
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.visitor.MultiClassVisitor
import testutils.ClassPoolBuilder
import testutils.JavaSource
import testutils.KotlinSource
import java.util.function.BiConsumer

class KotlinMetadataInitializerTest : FreeSpec({

    "Given a file facade containing 1 class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
            val property: String = "FOO"

            class Foo
                """.trimIndent()
            )
        )

        "Then the file facade ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("TestKt"),
                    withArg {
                        it.ownerClassName = "TestKt"
                    }
                )
            }
        }

        "Then the class ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "Foo",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("Foo"),
                    withArg {
                        it.ownerClassName = "Foo"
                    }
                )
            }
        }
    }

    "Given a class with Kotlin 1.4 metadata" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestKotlin1dot4Metadata.java",
                """
                    @kotlin.Metadata(
                        bv = {1, 0, 3},
                        d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                        d2 = {"LTestKotlin1dot4Metadata;", "", "()V"},
                        k = 1,
                        mv = {1, 4, 0}
                    )
                    public class TestKotlin1dot4Metadata { }
                """.trimIndent()
            )
        )
        "Then the metadata should be parsed correctly" {
            val visitor = spyk<KotlinMetadataVisitor>()
            programClassPool.classesAccept(
                MultiClassVisitor(
                    KotlinMetadataInitializer { _, _ -> },
                    ReferencedKotlinMetadataVisitor(visitor)
                )
            )

            verify {
                visitor.visitKotlinClassMetadata(
                    programClassPool.getClass("TestKotlin1dot4Metadata"),
                    withArg {
                        it.className shouldBe "TestKotlin1dot4Metadata"
                        it.mv shouldBe arrayOf(1, 4, 0)
                    }
                )
            }
        }
    }

    "Given an anonymous lambda function" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                fun foo(x: () -> Unit) { println(x) }
                fun main() {
                    foo { }
                }
                """.trimIndent()
            )
        )

        "When the jvmSignature method name is <anonymous>" - {
            val visitor = spyk<KotlinFunctionVisitor>()
            val clazz = programClassPool.getClass("TestKt\$main\$1")

            clazz.kotlinMetadataAccept(
                AllFunctionVisitor(
                    { _, _, func -> func.jvmSignature = MethodSignature(func.jvmSignature.className, "<anonymous>", func.jvmSignature.descriptor) }
                )
            )
            val logger: BiConsumer<Clazz, String> = spyk()
            clazz.accept(ClassReferenceInitializer(programClassPool, libraryClassPool))
            clazz.kotlinMetadataAccept(AllFunctionVisitor(visitor))

            "Then the referencedMethod should be initialized" {
                verify(exactly = 1) {
                    visitor.visitSyntheticFunction(
                        clazz,
                        ofType<KotlinSyntheticClassKindMetadata>(),
                        withArg {
                            it.name shouldBe "<anonymous>"
                            it.referencedMethod shouldBe clazz.findMethod("invoke", null)
                        }
                    )
                }

                verify(exactly = 0) {
                    logger.accept(
                        ofType<Clazz>(),
                        ofType<String>()
                    )
                }
            }
        }
    }
})
