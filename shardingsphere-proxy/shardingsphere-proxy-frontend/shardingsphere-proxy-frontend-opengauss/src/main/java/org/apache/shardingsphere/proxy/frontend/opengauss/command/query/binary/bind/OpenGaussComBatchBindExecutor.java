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

package org.apache.shardingsphere.proxy.frontend.opengauss.command.query.binary.bind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.db.protocol.opengauss.packet.command.query.binary.bind.OpenGaussComBatchBindPacket;
import org.apache.shardingsphere.db.protocol.packet.DatabasePacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.PostgreSQLPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.command.query.PostgreSQLColumnDescription;
import org.apache.shardingsphere.db.protocol.postgresql.packet.command.query.PostgreSQLRowDescriptionPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.command.query.binary.bind.PostgreSQLBindCompletePacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLCommandCompletePacket;
import org.apache.shardingsphere.infra.binder.SQLStatementContextFactory;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine;
import org.apache.shardingsphere.proxy.backend.communication.DatabaseCommunicationEngine;
import org.apache.shardingsphere.proxy.backend.communication.DatabaseCommunicationEngineFactory;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.impl.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.UpdateResponseHeader;
import org.apache.shardingsphere.proxy.frontend.command.executor.QueryCommandExecutor;
import org.apache.shardingsphere.proxy.frontend.command.executor.ResponseType;
import org.apache.shardingsphere.proxy.frontend.postgresql.command.PostgreSQLConnectionContext;
import org.apache.shardingsphere.proxy.frontend.postgresql.command.query.PostgreSQLCommand;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.EmptyStatement;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Command batch bind executor for openGauss.
 */
@RequiredArgsConstructor
public final class OpenGaussComBatchBindExecutor implements QueryCommandExecutor {
    
    private final PostgreSQLConnectionContext connectionContext;
    
    private final OpenGaussComBatchBindPacket packet;
    
    private final BackendConnection backendConnection;
    
    @Getter
    private volatile ResponseType responseType;
    
    private SQLStatement sqlStatement;
    
    private boolean batchBindComplete;
    
    @Override
    public Collection<DatabasePacket<?>> execute() throws SQLException {
        sqlStatement = parseSql(packet.getSql(), backendConnection.getSchemaName());
        while (packet.hasNextParameters()) {
            List<Object> parameters = packet.readOneGroupOfParameters();
            DatabaseCommunicationEngine databaseCommunicationEngine = newEngine(parameters);
            try {
                ResponseHeader responseHeader = databaseCommunicationEngine.execute();
                if (responseHeader instanceof UpdateResponseHeader) {
                    connectionContext.setUpdateCount(connectionContext.getUpdateCount() + ((UpdateResponseHeader) responseHeader).getUpdateCount());
                }
            } finally {
                backendConnection.closeDatabaseCommunicationEngines(false);
            }
        }
        return Collections.singletonList(new PostgreSQLBindCompletePacket());
    }
    
    private DatabaseCommunicationEngine newEngine(final List<Object> parameter) {
        return DatabaseCommunicationEngineFactory.getInstance().newBinaryProtocolInstance(getSqlStatementContext(parameter), packet.getSql(), parameter, backendConnection);
    }
    
    private SQLStatementContext<?> getSqlStatementContext(final List<Object> parameters) {
        Map<String, ShardingSphereMetaData> metaDataMap = ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaDataMap();
        return SQLStatementContextFactory.newInstance(metaDataMap, parameters, sqlStatement, backendConnection.getDefaultSchemaName());
    }
    
    private SQLStatement parseSql(final String sql, final String schemaName) {
        if (sql.isEmpty()) {
            return new EmptyStatement();
        }
        ShardingSphereSQLParserEngine sqlStatementParserEngine = new ShardingSphereSQLParserEngine(
                DatabaseTypeRegistry.getTrunkDatabaseTypeName(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData(schemaName).getResource().getDatabaseType()));
        return sqlStatementParserEngine.parse(sql, true);
    }
    
    private PostgreSQLRowDescriptionPacket getRowDescriptionPacket(final QueryResponseHeader queryResponseHeader) {
        responseType = ResponseType.QUERY;
        Collection<PostgreSQLColumnDescription> columnDescriptions = createColumnDescriptions(queryResponseHeader);
        return new PostgreSQLRowDescriptionPacket(columnDescriptions.size(), columnDescriptions);
    }
    
    private Collection<PostgreSQLColumnDescription> createColumnDescriptions(final QueryResponseHeader queryResponseHeader) {
        Collection<PostgreSQLColumnDescription> result = new LinkedList<>();
        int columnIndex = 0;
        for (QueryHeader each : queryResponseHeader.getQueryHeaders()) {
            result.add(new PostgreSQLColumnDescription(each.getColumnName(), ++columnIndex, each.getColumnType(), each.getColumnLength(), each.getColumnTypeName()));
        }
        return result;
    }
    
    @Override
    public boolean next() {
        return !batchBindComplete && (batchBindComplete = true);
    }
    
    @Override
    public PostgreSQLPacket getQueryRowPacket() {
        String sqlCommand = PostgreSQLCommand.valueOf(sqlStatement.getClass()).map(PostgreSQLCommand::getTag).orElse("");
        return new PostgreSQLCommandCompletePacket(sqlCommand, connectionContext.getUpdateCount());
    }
}
