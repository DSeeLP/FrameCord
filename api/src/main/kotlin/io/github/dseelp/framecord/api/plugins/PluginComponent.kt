/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
 *
 * MIT License
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

package io.github.dseelp.framecord.api.plugins

import io.github.dseelp.framecord.api.utils.koin.BaseKoinComponent
import io.github.dseelp.framecord.api.utils.koin.registerKoinModules
import org.koin.core.Koin
import org.koin.core.module.Module

interface PluginComponent<P : Plugin> : BaseKoinComponent {
    val plugin: P

    @OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
    override fun getKoin(): Koin = plugin.koinApp.koin

    @OptIn(io.github.dseelp.framecord.api.InternalFrameCordApi::class)
    override fun loadKoinModules(modules: List<Module>) = plugin.registerKoinModules(modules)

    override fun unloadKoinModules(modules: List<Module>) = throw UnsupportedOperationException()
}