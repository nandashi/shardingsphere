/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shadow.route.future.engine;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.infra.binder.LogicSQL;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.shadow.route.future.engine.dml.ShadowInsertStatementRoutingEngine;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.InsertStatement;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ShadowRouteEngineFactoryTest {
    
    @Test
    public void assertNewInstance() {
        assertThat(ShadowRouteEngineFactory.newInstance(new LogicSQL(createSqlStatementContext(), "", Lists.newArrayList())) instanceof ShadowInsertStatementRoutingEngine, is(true));
    }
    
    private SQLStatementContext<InsertStatement> createSqlStatementContext() {
        SQLStatementContext<InsertStatement> result = mock(SQLStatementContext.class);
        when(result.getSqlStatement()).thenReturn(mock(InsertStatement.class));
        return result;
    }
}
