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

package proguard.classfile.kotlin.flags

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinTypeParameterFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinTypeParameterFlagsTest : FreeSpec({
    val clazz = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            fun <T> foo(): Int = 42
            inline fun <reified ReifiedT> bar(): Int = 42
            """
        )
    ).getClass("TestKt")

    "Given a non-reified type parameter" - {

        "Then the flags should be initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("T", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("T", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }

    "Given a reified type parameter" - {

        "Then the flags should be initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("ReifiedT", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe true

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("ReifiedT", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe true

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }
})

private fun createVisitor(typeName: String, typeVisitor: KotlinTypeParameterVisitor): KotlinMetadataVisitor =
    AllTypeParameterVisitor(
        KotlinTypeParameterFilter(
            Predicate { it.name == typeName },
            typeVisitor
        )
    )
