/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.shell.cli.jline.validator;

import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.shell.cli.utils.IncompleteInputFinder;

/**
 *  Validates user input completes as an expression.
 *
 * @since 2.0.0
 */
public class ExpressionValidator implements Validator {

    private Validator nextInValidator;

    public ExpressionValidator() {
        nextInValidator = null;
    }

    @Override
    public void setNextValidator(Validator nextValidator) {
        this.nextInValidator = nextValidator;
    }

    @Override
    public boolean evaluate(String source) {
        IncompleteInputFinder incompleteInputFinder = new IncompleteInputFinder();
        boolean isIncomplete = NodeParser.parseExpression(source).apply(incompleteInputFinder);
        if (!NodeParser.parseExpression(source).hasDiagnostics()) {
            return false;
        } else {
            if (!source.endsWith(";") && !NodeParser.parseExpression(source + ";").hasDiagnostics()) {
                return false;
            }
        }

        if (!isIncomplete) {
            return nextInValidator.evaluate(source);
        } else {
            return true;
        }

    }
}
