package com.ubertob.pesticide.core

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.opentest4j.TestAbortedException
import java.time.LocalDate
import kotlin.streams.asStream

/**
 * DdtScenario is the class that keeps together the information to create a scenario test. It can generate the {@code DynamicTest}
 *
 * Normally it shouldn't be created directly but using the {@code ddtScenario} method of {@code DomainDrivenTest}
 */
data class DdtScenario<D : DomainInterpreter<*>>(
    val setting: Setting<D>,
    val steps: Iterable<DdtStep<D, *>>,
    val wipData: WipData? = null
) : (D) -> DynamicContainer {

    val ALREADY_FAILED = "_alreadyFailed"
    val contextStore = ContextStore()

    override fun invoke(domainInterpreter: D): DynamicContainer {
        assertEquals(Ready, domainInterpreter.prepare(), "Protocol ${domainInterpreter.protocol.desc} ready")

        val tests = trapUnexpectedExceptions {
            createTests(domainInterpreter)
        }

        val inWip = getDueDate(wipData, domainInterpreter.protocol)?.let { "WIP till $it - " } ?: ""

        return DynamicContainer.dynamicContainer(
            "$inWip${domainInterpreter.description()}",
            tests.asSequence().asStream()
        )
    }


    private fun createTests(domain: D): List<DynamicNode> =
        createTest(setting.asStep(), domain)
        {
            contextStore.clear()
            contextStore.store(ALREADY_FAILED, false)
            decorateExecution(domain, setting.asStep(), StepContext(setting.asStep().actor.name, contextStore))
        } prependTo steps.map { step ->
            createTest(step, domain)
            { decorateExecution(domain, step, StepContext(step.actor.name, contextStore)) }
        }

    private fun <C : Any> createTest(
        step: DdtStep<D, C>,
        domain: D,
        executable: () -> Unit
    ): DynamicTest = dynamicTest(decorateTestName(domain, step), step.testSourceURI(), executable)

    private fun <C : Any> decorateExecution(
        interpreter: D,
        step: DdtStep<D, C>,
        stepContext: StepContext<C>
    ) {
        checkWIP(wipData, interpreter.protocol) {
            Assumptions.assumeFalse(
                contextStore.get(ALREADY_FAILED) as Boolean
                , "Skipped because of previous failures"
            )

            try {
                step.action(interpreter, stepContext)
            } catch (t: Throwable) {
                contextStore.store(ALREADY_FAILED, true)
                throw t
            }

        }
    }


    private fun decorateTestName(domainUnderTest: D, step: DdtStep<D, *>) =
        "${domainUnderTest.protocol.desc} - ${step.description}"


    private fun checkWIP(wipData: WipData?, protocol: DdtProtocol, testBlock: () -> Unit) =
        getDueDate(wipData, protocol)
            ?.let { executeInWIP(it, testBlock) }
            ?: testBlock()


    private fun getDueDate(wipData: WipData?, protocol: DdtProtocol): LocalDate? =
        if (wipData == null || wipData.shouldWorkFor(protocol))
            null
        else
            wipData.dueDate

    private fun <T : Any> trapUnexpectedExceptions(block: () -> T): T =
        try {
            block()
        } catch (t: Throwable) {
            fail(
                "Unexpected Exception while creating the tests. Have you forgotten to use generateStep in your actors? ",
                t
            )
        }

    fun DomainInterpreter<*>.description(): String = javaClass.simpleName


    private fun executeInWIP(
        due: LocalDate,
        testBlock: () -> Unit
    ) {
        if (due < LocalDate.now()) {
            fail("Due date expired $due")
        } else {
            try {
                testBlock()
            } catch (aborted: TestAbortedExceptionWIP) {
                throw aborted //nothing to do here
            } catch (t: Throwable) {
                //all the rest
                throw TestAbortedExceptionWIP(
                    "Test failed but this is ok until $due",
                    t
                )
            }
            throw TestAbortedExceptionWIP("Test succeded but you have to remove the WIP marker!")
        }
    }

    data class TestAbortedExceptionWIP(override val message: String, val throwable: Throwable? = null) :
        TestAbortedException(message, throwable)

    infix fun <T : Any> T.prependTo(list: List<T>) = listOf(this) + list

}



