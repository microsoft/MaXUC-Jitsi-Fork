// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

public abstract class TableConstraint
{
    public abstract void appendCreateString(StringBuilder sb);
}
