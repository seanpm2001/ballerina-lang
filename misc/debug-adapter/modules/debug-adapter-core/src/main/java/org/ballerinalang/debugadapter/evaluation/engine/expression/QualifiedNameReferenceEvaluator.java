/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.ballerinalang.debugadapter.evaluation.engine.expression;

import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import org.ballerinalang.debugadapter.EvaluationContext;
import org.ballerinalang.debugadapter.evaluation.BExpressionValue;
import org.ballerinalang.debugadapter.evaluation.EvaluationException;
import org.ballerinalang.debugadapter.evaluation.engine.Evaluator;
import org.ballerinalang.debugadapter.evaluation.utils.VariableUtils;

import java.util.Optional;

import static org.ballerinalang.debugadapter.evaluation.EvaluationException.createEvaluationException;
import static org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind.IMPORT_RESOLVING_ERROR;
import static org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind.INTERNAL_ERROR;
import static org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind.NON_PUBLIC_OR_UNDEFINED_ACCESS;
import static org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind.QUALIFIED_VARIABLE_RESOLVING_FAILED;
import static org.ballerinalang.debugadapter.evaluation.engine.EvaluationTypeResolver.isPublicSymbol;

/**
 * Ballerina qualified name reference evaluator implementation.
 *
 * @since 2.0.0
 */
public class QualifiedNameReferenceEvaluator extends Evaluator {

    private final String nameReference;
    private final QualifiedNameReferenceNode syntaxNode;

    public QualifiedNameReferenceEvaluator(EvaluationContext context, QualifiedNameReferenceNode node) {
        super(context);
        this.syntaxNode = node;
        this.nameReference = node.identifier().text().trim();
    }

    @Override
    public BExpressionValue evaluate() throws EvaluationException {
        try {
            String modulePrefix = syntaxNode.modulePrefix().text();
            if (!resolvedImports.containsKey(modulePrefix)) {
                throw createEvaluationException(IMPORT_RESOLVING_ERROR, modulePrefix);
            }
            ModuleSymbol moduleSymbol = resolvedImports.get(modulePrefix).getResolvedSymbol();

            // Validates whether the given module has a public constant, using semantic API.
            Optional<BExpressionValue> constant = searchForModuleConstant(modulePrefix);
            if (constant.isPresent()) {
                return constant.get();
            }

            // Validates whether the given module has a public variable, using semantic API.
            Optional<BExpressionValue> moduleVariable = searchForModuleVariable(moduleSymbol, modulePrefix);
            if (moduleVariable.isEmpty()) {
                throw createEvaluationException(QUALIFIED_VARIABLE_RESOLVING_FAILED, modulePrefix, nameReference);
            }
            return moduleVariable.get();
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw createEvaluationException(INTERNAL_ERROR, syntaxNode.toSourceCode().trim());
        }
    }

    private Optional<BExpressionValue> searchForModuleConstant(String modulePrefix) throws EvaluationException {
        ModuleSymbol moduleSymbol = resolvedImports.get(modulePrefix).getResolvedSymbol();
        Optional<ConstantSymbol> constSymbol = moduleSymbol.constants().stream()
                .filter(constantSymbol -> constantSymbol.getName().isPresent()
                        && constantSymbol.getName().get().equals(nameReference))
                .findAny();

        if (constSymbol.isPresent()) {
            if (!isPublicSymbol(constSymbol.get())) {
                throw createEvaluationException(NON_PUBLIC_OR_UNDEFINED_ACCESS, nameReference);
            }
            Optional<BExpressionValue> moduleVariable = VariableUtils.getModuleVariable(context, moduleSymbol,
                    nameReference);
            if (moduleVariable.isEmpty()) {
                throw createEvaluationException(QUALIFIED_VARIABLE_RESOLVING_FAILED, modulePrefix, nameReference);
            }
            return moduleVariable;
        }

        return Optional.empty();
    }

    private Optional<BExpressionValue> searchForModuleVariable(ModuleSymbol moduleSymbol, String modulePrefix)
            throws EvaluationException {
        Optional<VariableSymbol> variableSymbol = moduleSymbol.allSymbols().stream()
                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE
                        && symbol.getName().isPresent()
                        && symbol.getName().get().equals(nameReference))
                .map(symbol -> (VariableSymbol) symbol)
                .findAny();

        if (variableSymbol.isEmpty()) {
            throw createEvaluationException(NON_PUBLIC_OR_UNDEFINED_ACCESS, modulePrefix, nameReference);
        } else if (!isPublicSymbol(variableSymbol.get())) {
            throw createEvaluationException(NON_PUBLIC_OR_UNDEFINED_ACCESS, modulePrefix, nameReference);
        }

        return VariableUtils.getModuleVariable(context, variableSymbol.get().getModule().orElseThrow(), nameReference);
    }
}
