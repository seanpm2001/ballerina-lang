/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerina.compiler.impl.types;

import org.ballerina.compiler.api.element.ModuleID;
import org.ballerina.compiler.api.symbol.BallerinaField;
import org.ballerina.compiler.api.type.BallerinaTypeDescriptor;
import org.ballerina.compiler.api.type.TypeDescKind;
import org.ballerina.compiler.impl.semantic.BallerinaTypeDesc;
import org.ballerina.compiler.impl.semantic.TypesFactory;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Represents a record type descriptor.
 *
 * @since 1.3.0
 */
public class RecordTypeDescriptor extends BallerinaTypeDesc {

    private List<BallerinaField> fieldDescriptors;
    private boolean isInclusive;
    // private TypeDescriptor typeReference;
    private BallerinaTypeDescriptor restTypeDesc;

    public RecordTypeDescriptor(ModuleID moduleID, BRecordType recordType) {
        super(TypeDescKind.RECORD, moduleID, recordType);
        this.isInclusive = !recordType.sealed;
        // TODO: Fix this
        // this.typeReference = null;
    }

    /**
     * Get the list of field descriptors.
     *
     * @return {@link List} of ballerina field
     */
    public List<BallerinaField> getFieldDescriptors() {
        if (this.fieldDescriptors == null) {
            this.fieldDescriptors = new ArrayList<>();
            for (BField field : ((BRecordType) this.getBType()).fields.values()) {
                this.fieldDescriptors.add(new BallerinaField(field));
            }
        }

        return this.fieldDescriptors;
    }

    /**
     * Whether inclusive record ot not.
     *
     * @return {@link Boolean} inclusive or not
     */
    public boolean isInclusive() {
        return isInclusive;
    }

    public Optional<BallerinaTypeDescriptor> getRestTypeDesc() {
        if (this.restTypeDesc == null) {
            this.restTypeDesc = TypesFactory.getTypeDescriptor(((BRecordType) this.getBType()).restFieldType);
        }
        return Optional.ofNullable(this.restTypeDesc);
    }

    @Override
    public String signature() {
        StringJoiner joiner = new StringJoiner(";");
        for (BallerinaField fieldDescriptor : this.getFieldDescriptors()) {
            String ballerinaFieldSignature = fieldDescriptor.signature();
            joiner.add(ballerinaFieldSignature);
        }
        this.getRestTypeDesc().ifPresent(typeDescriptor -> joiner.add(typeDescriptor.signature() + "..."));
//        this.getTypeReference().ifPresent(typeDescriptor -> joiner.add("*" + typeDescriptor.getSignature()));

        StringBuilder signature = new StringBuilder("{");
        if (!this.isInclusive) {
            signature.append("|");
        }
        signature.append(joiner.toString());
        if (!this.isInclusive) {
            signature.append("|");
        }
        signature.append("}");
        return signature.toString();
    }
}
