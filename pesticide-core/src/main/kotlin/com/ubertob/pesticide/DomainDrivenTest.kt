package com.ubertob.pesticide

import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import java.time.LocalDate
import java.util.stream.Stream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


typealias DDT = TestFactory

data class Setting<D : DomainUnderTest<*>>(val setUp: DdtStep<D>)

abstract class DomainDrivenTest<D : DomainUnderTest<*>>(private val domains: Iterable<D>) {

    fun play(vararg stepsArray: DdtStep<D>): Scenario<D> =
        Scenario(stepsArray.toList())

    fun Scenario<D>.wip(
        dueDate: LocalDate,
        reason: String = "Work In Progress",
        except: Set<KClass<out DdtProtocol>> = emptySet()
    ): Scenario<D> =
        this.copy(wipData = WipData(dueDate, except, reason))

    val timeoutInMillis = 1000


    fun ddtScenario(
        scenarioBuilder: () -> Scenario<D>
    ): Stream<out DynamicNode> =
        domains.map {
            scenarioBuilder()(it)
        }.toList().stream()


    @JvmField
    val withoutSetting: Setting<D> = Setting(DdtStep("empty stage") { it })

    fun setting(
        block: D.() -> D
    ): Setting<D> = Setting(DdtStep("Preparing", block))


    infix fun Setting<D>.atRise(steps: Scenario<D>): Scenario<D> =
        Scenario(listOf(this.setUp) + steps.steps, steps.wipData) //add source URL

}


class ActorDelegate<D : DomainUnderTest<*>, A : DdtActor<D>>(val actorConstructor: (String) -> A) :
    ReadOnlyProperty<DomainDrivenTest<D>, A> {
    override operator fun getValue(thisRef: DomainDrivenTest<D>, property: KProperty<*>): A =
        actorConstructor(property.name.capitalize())

}