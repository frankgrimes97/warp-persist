/**
 * Copyright (C) 2008 Wideplay Interactive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wideplay.warp.persist;

import com.google.inject.matcher.Matcher;

import java.lang.reflect.Method;

/**
 * @author Robbie Vanbrabant
 */
class TransactionMatcher {
    private Matcher<? super Class<?>> txClassMatcher = Defaults.TX_CLASS_MATCHER;
    private Matcher<? super Method> txMethodMatcher = Defaults.TX_METHOD_MATCHER;

    TransactionMatcher() {
    }

    TransactionMatcher(Matcher<? super Class<?>> txClassMatcher) {
        this.txClassMatcher = txClassMatcher;
    }

    TransactionMatcher(Matcher<? super Class<?>> txClassMatcher, Matcher<? super Method> txMethodMatcher) {
        this(txClassMatcher);
        this.txMethodMatcher = txMethodMatcher;
    }

    public Matcher<? super Class<?>> getTxClassMatcher() {
        return txClassMatcher;
    }

    public Matcher<? super Method> getTxMethodMatcher() {
        return txMethodMatcher;
    }
}