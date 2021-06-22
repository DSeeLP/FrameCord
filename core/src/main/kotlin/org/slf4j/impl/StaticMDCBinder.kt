/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

package org.slf4j.impl

import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter

class StaticMDCBinder private constructor() {
    val mDCA: MDCAdapter = NOPMDCAdapter()
    val mDCAdapterClassStr: String = NOPMDCAdapter::class.java.name

    companion object {
        @JvmStatic
        @get:JvmName("getSingleton")
        val singleton = StaticMDCBinder()
    }
}