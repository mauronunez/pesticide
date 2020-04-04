package com.ubertob.pesticide.junit5

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class FailingTest {

    @Test
    fun `this test will fail`() {

        expectThat(3).isEqualTo(4)
    }

    @Test
    fun `this test will fail in another function`() {

        aFailingFun()


    }

    private fun aFailingFun() {
        expectThat(4).isEqualTo(3)
    }


}