/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.requests;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.NotCoordinatorException;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.UnknownServerException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.network.Send;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.InvalidRecordException;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Utils;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequestResponseTest {

    @Test
    public void testSerialization() throws Exception {
        checkRequest(createFindCoordinatorRequest(0));
        checkRequest(createFindCoordinatorRequest(1));
        checkErrorResponse(createFindCoordinatorRequest(0), new UnknownServerException());
        checkErrorResponse(createFindCoordinatorRequest(1), new UnknownServerException());
        checkResponse(createFindCoordinatorResponse(), 0);
        checkResponse(createFindCoordinatorResponse(), 1);
        checkRequest(createControlledShutdownRequest());
        checkResponse(createControlledShutdownResponse(), 1);
        checkErrorResponse(createControlledShutdownRequest(), new UnknownServerException());
        checkRequest(createFetchRequest(4));
        checkResponse(createFetchResponse(), 4);
        checkErrorResponse(createFetchRequest(4), new UnknownServerException());
        checkRequest(createHeartBeatRequest());
        checkErrorResponse(createHeartBeatRequest(), new UnknownServerException());
        checkResponse(createHeartBeatResponse(), 0);
        checkRequest(createJoinGroupRequest(1));
        checkErrorResponse(createJoinGroupRequest(0), new UnknownServerException());
        checkErrorResponse(createJoinGroupRequest(1), new UnknownServerException());
        checkResponse(createJoinGroupResponse(), 0);
        checkRequest(createLeaveGroupRequest());
        checkErrorResponse(createLeaveGroupRequest(), new UnknownServerException());
        checkResponse(createLeaveGroupResponse(), 0);
        checkRequest(createListGroupsRequest());
        checkErrorResponse(createListGroupsRequest(), new UnknownServerException());
        checkResponse(createListGroupsResponse(), 0);
        checkRequest(createDescribeGroupRequest());
        checkErrorResponse(createDescribeGroupRequest(), new UnknownServerException());
        checkResponse(createDescribeGroupResponse(), 0);
        checkRequest(createListOffsetRequest(1));
        checkErrorResponse(createListOffsetRequest(1), new UnknownServerException());
        checkResponse(createListOffsetResponse(1), 1);
        checkRequest(createListOffsetRequest(2));
        checkErrorResponse(createListOffsetRequest(2), new UnknownServerException());
        checkResponse(createListOffsetResponse(2), 2);
        checkRequest(MetadataRequest.Builder.allTopics().build((short) 2));
        checkRequest(createMetadataRequest(1, asList("topic1")));
        checkErrorResponse(createMetadataRequest(1, asList("topic1")), new UnknownServerException());
        checkResponse(createMetadataResponse(), 2);
        checkErrorResponse(createMetadataRequest(2, asList("topic1")), new UnknownServerException());
        checkRequest(createOffsetCommitRequest(2));
        checkErrorResponse(createOffsetCommitRequest(2), new UnknownServerException());
        checkResponse(createOffsetCommitResponse(), 0);
        checkRequest(OffsetFetchRequest.forAllPartitions("group1"));
        checkErrorResponse(OffsetFetchRequest.forAllPartitions("group1"), new NotCoordinatorException("Not Coordinator"));
        checkRequest(createOffsetFetchRequest(0));
        checkRequest(createOffsetFetchRequest(1));
        checkRequest(createOffsetFetchRequest(2));
        checkRequest(OffsetFetchRequest.forAllPartitions("group1"));
        checkErrorResponse(createOffsetFetchRequest(0), new UnknownServerException());
        checkErrorResponse(createOffsetFetchRequest(1), new UnknownServerException());
        checkErrorResponse(createOffsetFetchRequest(2), new UnknownServerException());
        checkResponse(createOffsetFetchResponse(), 0);
        checkRequest(createProduceRequest(2));
        checkErrorResponse(createProduceRequest(2), new UnknownServerException());
        checkRequest(createProduceRequest(3));
        checkErrorResponse(createProduceRequest(3), new UnknownServerException());
        checkResponse(createProduceResponse(), 2);
        checkRequest(createStopReplicaRequest(true));
        checkRequest(createStopReplicaRequest(false));
        checkErrorResponse(createStopReplicaRequest(true), new UnknownServerException());
        checkResponse(createStopReplicaResponse(), 0);
        checkRequest(createLeaderAndIsrRequest());
        checkErrorResponse(createLeaderAndIsrRequest(), new UnknownServerException());
        checkResponse(createLeaderAndIsrResponse(), 0);
        checkRequest(createSaslHandshakeRequest());
        checkErrorResponse(createSaslHandshakeRequest(), new UnknownServerException());
        checkResponse(createSaslHandshakeResponse(), 0);
        checkRequest(createApiVersionRequest());
        checkErrorResponse(createApiVersionRequest(), new UnknownServerException());
        checkResponse(createApiVersionResponse(), 0);
        checkRequest(createCreateTopicRequest(0));
        checkErrorResponse(createCreateTopicRequest(0), new UnknownServerException());
        checkResponse(createCreateTopicResponse(), 0);
        checkRequest(createCreateTopicRequest(1));
        checkErrorResponse(createCreateTopicRequest(1), new UnknownServerException());
        checkResponse(createCreateTopicResponse(), 1);
        checkRequest(createDeleteTopicsRequest());
        checkErrorResponse(createDeleteTopicsRequest(), new UnknownServerException());
        checkResponse(createDeleteTopicsResponse(), 0);

        checkRequest(createInitPidRequest());
        checkErrorResponse(createInitPidRequest(), new UnknownServerException());
        checkResponse(createInitPidResponse(), 0);

        checkRequest(createAddPartitionsToTxnRequest());
        checkResponse(createAddPartitionsToTxnResponse(), 0);
        checkErrorResponse(createAddPartitionsToTxnRequest(), new UnknownServerException());
        checkRequest(createAddOffsetsToTxnRequest());
        checkResponse(createAddOffsetsToTxnResponse(), 0);
        checkErrorResponse(createAddOffsetsToTxnRequest(), new UnknownServerException());
        checkRequest(createEndTxnRequest());
        checkResponse(createEndTxnResponse(), 0);
        checkErrorResponse(createEndTxnRequest(), new UnknownServerException());
        checkRequest(createWriteTxnMarkersRequest());
        checkResponse(createWriteTxnMarkersResponse(), 0);
        checkErrorResponse(createWriteTxnMarkersRequest(), new UnknownServerException());
        checkRequest(createTxnOffsetCommitRequest());
        checkResponse(createTxnOffsetCommitResponse(), 0);
        checkErrorResponse(createTxnOffsetCommitRequest(), new UnknownServerException());

        checkOlderFetchVersions();
        checkResponse(createMetadataResponse(), 0);
        checkResponse(createMetadataResponse(), 1);
        checkErrorResponse(createMetadataRequest(1, asList("topic1")), new UnknownServerException());
        checkRequest(createOffsetCommitRequest(0));
        checkErrorResponse(createOffsetCommitRequest(0), new UnknownServerException());
        checkRequest(createOffsetCommitRequest(1));
        checkErrorResponse(createOffsetCommitRequest(1), new UnknownServerException());
        checkRequest(createJoinGroupRequest(0));
        checkRequest(createUpdateMetadataRequest(0, null));
        checkErrorResponse(createUpdateMetadataRequest(0, null), new UnknownServerException());
        checkRequest(createUpdateMetadataRequest(1, null));
        checkRequest(createUpdateMetadataRequest(1, "rack1"));
        checkErrorResponse(createUpdateMetadataRequest(1, null), new UnknownServerException());
        checkRequest(createUpdateMetadataRequest(2, "rack1"));
        checkRequest(createUpdateMetadataRequest(2, null));
        checkErrorResponse(createUpdateMetadataRequest(2, "rack1"), new UnknownServerException());
        checkRequest(createUpdateMetadataRequest(3, "rack1"));
        checkRequest(createUpdateMetadataRequest(3, null));
        checkErrorResponse(createUpdateMetadataRequest(3, "rack1"), new UnknownServerException());
        checkResponse(createUpdateMetadataResponse(), 0);
        checkRequest(createListOffsetRequest(0));
        checkErrorResponse(createListOffsetRequest(0), new UnknownServerException());
        checkResponse(createListOffsetResponse(0), 0);
        checkRequest(createLeaderEpochRequest());
        checkResponse(createLeaderEpochResponse(), 0);
        checkErrorResponse(createLeaderEpochRequest(), new UnknownServerException());
        checkRequest(createAddPartitionsToTxnRequest());
        checkErrorResponse(createAddPartitionsToTxnRequest(), new UnknownServerException());
        checkResponse(createAddPartitionsToTxnResponse(), 0);
        checkRequest(createAddOffsetsToTxnRequest());
        checkErrorResponse(createAddOffsetsToTxnRequest(), new UnknownServerException());
        checkResponse(createAddOffsetsToTxnResponse(), 0);
        checkRequest(createEndTxnRequest());
        checkErrorResponse(createEndTxnRequest(), new UnknownServerException());
        checkResponse(createEndTxnResponse(), 0);
        checkRequest(createWriteTxnMarkersRequest());
        checkErrorResponse(createWriteTxnMarkersRequest(), new UnknownServerException());
        checkResponse(createWriteTxnMarkersResponse(), 0);
        checkRequest(createTxnOffsetCommitRequest());
        checkErrorResponse(createTxnOffsetCommitRequest(), new UnknownServerException());
        checkResponse(createTxnOffsetCommitResponse(), 0);
    }

    @Test
    public void testRequestHeader() {
        RequestHeader header = createRequestHeader();
        ByteBuffer buffer = toBuffer(header.toStruct());
        RequestHeader deserialized = RequestHeader.parse(buffer);
        assertEquals(header.apiVersion(), deserialized.apiVersion());
        assertEquals(header.apiKey(), deserialized.apiKey());
        assertEquals(header.clientId(), deserialized.clientId());
        assertEquals(header.correlationId(), deserialized.correlationId());
    }

    @Test
    public void testResponseHeader() {
        ResponseHeader header = createResponseHeader();
        ByteBuffer buffer = toBuffer(header.toStruct());
        ResponseHeader deserialized = ResponseHeader.parse(buffer);
        assertEquals(header.correlationId(), deserialized.correlationId());
    }

    private void checkOlderFetchVersions() throws Exception {
        int latestVersion = ApiKeys.FETCH.latestVersion();
        for (int i = 0; i < latestVersion; ++i) {
            checkErrorResponse(createFetchRequest(i), new UnknownServerException());
            checkRequest(createFetchRequest(i));
            checkResponse(createFetchResponse(), i);
        }
    }

    private void checkErrorResponse(AbstractRequest req, Throwable e) throws Exception {
        checkResponse(req.getErrorResponse(e), req.version());
    }

    private void checkRequest(AbstractRequest req) throws Exception {
        // Check that we can serialize, deserialize and serialize again
        // We don't check for equality or hashCode because it is likely to fail for any request containing a HashMap
        Struct struct = req.toStruct();
        AbstractRequest deserialized = (AbstractRequest) deserialize(req, struct, req.version());
        deserialized.toStruct();
    }

    private void checkResponse(AbstractResponse response, int version) throws Exception {
        // Check that we can serialize, deserialize and serialize again
        // We don't check for equality or hashCode because it is likely to fail for any response containing a HashMap
        Struct struct = response.toStruct((short) version);
        AbstractResponse deserialized = (AbstractResponse) deserialize(response, struct, (short) version);
        deserialized.toStruct((short) version);
    }

    private AbstractRequestResponse deserialize(AbstractRequestResponse req, Struct struct, short version) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ByteBuffer buffer = toBuffer(struct);
        Method deserializer = req.getClass().getDeclaredMethod("parse", ByteBuffer.class, Short.TYPE);
        return (AbstractRequestResponse) deserializer.invoke(null, buffer, version);
    }

    private ByteBuffer toBuffer(Struct struct) {
        ByteBuffer buffer = ByteBuffer.allocate(struct.sizeOf());
        struct.writeTo(buffer);
        buffer.rewind();
        return buffer;
    }

    @Test(expected = UnsupportedVersionException.class)
    public void cannotUseFindCoordinatorV0ToFindTransactionCoordinator() {
        FindCoordinatorRequest.Builder builder = new FindCoordinatorRequest.Builder(FindCoordinatorRequest.CoordinatorType.TRANSACTION, "foobar");
        builder.build((short) 0);
    }

    @Test
    public void produceRequestToStringTest() {
        ProduceRequest request = createProduceRequest(ApiKeys.PRODUCE.latestVersion());
        assertEquals(1, request.partitionRecordsOrFail().size());
        assertFalse(request.toString(false).contains("partitionSizes"));
        assertTrue(request.toString(false).contains("numPartitions=1"));
        assertTrue(request.toString(true).contains("partitionSizes"));
        assertFalse(request.toString(true).contains("numPartitions"));

        request.clearPartitionRecords();
        try {
            request.partitionRecordsOrFail();
            fail("partitionRecordsOrFail should fail after clearPartitionRecords()");
        } catch (IllegalStateException e) {
            // OK
        }

        // `toString` should behave the same after `clearPartitionRecords`
        assertFalse(request.toString(false).contains("partitionSizes"));
        assertTrue(request.toString(false).contains("numPartitions=1"));
        assertTrue(request.toString(true).contains("partitionSizes"));
        assertFalse(request.toString(true).contains("numPartitions"));
    }

    @Test
    public void produceRequestGetErrorResponseTest() {
        ProduceRequest request = createProduceRequest(ApiKeys.PRODUCE.latestVersion());
        Set<TopicPartition> partitions = new HashSet<>(request.partitionRecordsOrFail().keySet());

        ProduceResponse errorResponse = (ProduceResponse) request.getErrorResponse(new NotEnoughReplicasException());
        assertEquals(partitions, errorResponse.responses().keySet());
        ProduceResponse.PartitionResponse partitionResponse = errorResponse.responses().values().iterator().next();
        assertEquals(Errors.NOT_ENOUGH_REPLICAS, partitionResponse.error);
        assertEquals(ProduceResponse.INVALID_OFFSET, partitionResponse.baseOffset);
        assertEquals(RecordBatch.NO_TIMESTAMP, partitionResponse.logAppendTime);

        request.clearPartitionRecords();

        // `getErrorResponse` should behave the same after `clearPartitionRecords`
        errorResponse = (ProduceResponse) request.getErrorResponse(new NotEnoughReplicasException());
        assertEquals(partitions, errorResponse.responses().keySet());
        partitionResponse = errorResponse.responses().values().iterator().next();
        assertEquals(Errors.NOT_ENOUGH_REPLICAS, partitionResponse.error);
        assertEquals(ProduceResponse.INVALID_OFFSET, partitionResponse.baseOffset);
        assertEquals(RecordBatch.NO_TIMESTAMP, partitionResponse.logAppendTime);
    }

    @Test
    public void produceResponseVersionTest() {
        Map<TopicPartition, ProduceResponse.PartitionResponse> responseData = new HashMap<>();
        responseData.put(new TopicPartition("test", 0), new ProduceResponse.PartitionResponse(Errors.NONE,
                10000, RecordBatch.NO_TIMESTAMP));
        ProduceResponse v0Response = new ProduceResponse(responseData);
        ProduceResponse v1Response = new ProduceResponse(responseData, 10);
        ProduceResponse v2Response = new ProduceResponse(responseData, 10);
        assertEquals("Throttle time must be zero", 0, v0Response.getThrottleTime());
        assertEquals("Throttle time must be 10", 10, v1Response.getThrottleTime());
        assertEquals("Throttle time must be 10", 10, v2Response.getThrottleTime());
        assertEquals("Should use schema version 0", ApiKeys.PRODUCE.responseSchema((short) 0),
                v0Response.toStruct((short) 0).schema());
        assertEquals("Should use schema version 1", ApiKeys.PRODUCE.responseSchema((short) 1),
                v1Response.toStruct((short) 1).schema());
        assertEquals("Should use schema version 2", ApiKeys.PRODUCE.responseSchema((short) 2),
                v2Response.toStruct((short) 2).schema());
        assertEquals("Response data does not match", responseData, v0Response.responses());
        assertEquals("Response data does not match", responseData, v1Response.responses());
        assertEquals("Response data does not match", responseData, v2Response.responses());
    }

    @Test(expected = InvalidRecordException.class)
    public void produceRequestV3ShouldContainOnlyOneRecordBatch() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        MemoryRecordsBuilder builder = MemoryRecords.builder(buffer, CompressionType.NONE, TimestampType.CREATE_TIME, 0L);
        builder.append(10L, null, "a".getBytes());
        builder.close();

        builder = MemoryRecords.builder(buffer, CompressionType.NONE, TimestampType.CREATE_TIME, 1L);
        builder.append(11L, "1".getBytes(), "b".getBytes());
        builder.append(12L, null, "c".getBytes());
        builder.close();

        buffer.flip();

        Map<TopicPartition, MemoryRecords> produceData = new HashMap<>();
        produceData.put(new TopicPartition("test", 0), MemoryRecords.readableRecords(buffer));
        new ProduceRequest.Builder(RecordBatch.CURRENT_MAGIC_VALUE, (short) 1, 5000, produceData).build().toStruct();
    }

    @Test(expected = InvalidRecordException.class)
    public void produceRequestV3CannotHaveNoRecordBatches() {
        Map<TopicPartition, MemoryRecords> produceData = new HashMap<>();
        produceData.put(new TopicPartition("test", 0), MemoryRecords.EMPTY);
        new ProduceRequest.Builder(RecordBatch.CURRENT_MAGIC_VALUE, (short) 1, 5000, produceData).build().toStruct();
    }

    @Test(expected = InvalidRecordException.class)
    public void produceRequestV3CannotUseMagicV0() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        MemoryRecordsBuilder builder = MemoryRecords.builder(buffer, RecordBatch.MAGIC_VALUE_V0, CompressionType.NONE,
                TimestampType.NO_TIMESTAMP_TYPE, 0L);
        builder.append(10L, null, "a".getBytes());

        Map<TopicPartition, MemoryRecords> produceData = new HashMap<>();
        produceData.put(new TopicPartition("test", 0), builder.build());
        new ProduceRequest.Builder(RecordBatch.CURRENT_MAGIC_VALUE, (short) 1, 5000, produceData).build().toStruct();
    }

    @Test(expected = InvalidRecordException.class)
    public void produceRequestV3CannotUseMagicV1() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        MemoryRecordsBuilder builder = MemoryRecords.builder(buffer, RecordBatch.MAGIC_VALUE_V1, CompressionType.NONE,
                TimestampType.CREATE_TIME, 0L);
        builder.append(10L, null, "a".getBytes());

        Map<TopicPartition, MemoryRecords> produceData = new HashMap<>();
        produceData.put(new TopicPartition("test", 0), builder.build());
        new ProduceRequest.Builder(RecordBatch.CURRENT_MAGIC_VALUE, (short) 1, 5000, produceData).build().toStruct();
    }

    @Test
    public void fetchResponseVersionTest() {
        LinkedHashMap<TopicPartition, FetchResponse.PartitionData> responseData = new LinkedHashMap<>();

        MemoryRecords records = MemoryRecords.readableRecords(ByteBuffer.allocate(10));
        responseData.put(new TopicPartition("test", 0), new FetchResponse.PartitionData(Errors.NONE, 1000000,
                FetchResponse.INVALID_LAST_STABLE_OFFSET, 0L, null, records));

        FetchResponse v0Response = new FetchResponse(responseData, 0);
        FetchResponse v1Response = new FetchResponse(responseData, 10);
        assertEquals("Throttle time must be zero", 0, v0Response.throttleTimeMs());
        assertEquals("Throttle time must be 10", 10, v1Response.throttleTimeMs());
        assertEquals("Should use schema version 0", ApiKeys.FETCH.responseSchema((short) 0),
                v0Response.toStruct((short) 0).schema());
        assertEquals("Should use schema version 1", ApiKeys.FETCH.responseSchema((short) 1),
                v1Response.toStruct((short) 1).schema());
        assertEquals("Response data does not match", responseData, v0Response.responseData());
        assertEquals("Response data does not match", responseData, v1Response.responseData());
    }

    @Test
    public void testFetchResponseV4() {
        LinkedHashMap<TopicPartition, FetchResponse.PartitionData> responseData = new LinkedHashMap<>();

        MemoryRecords records = MemoryRecords.readableRecords(ByteBuffer.allocate(10));

        List<FetchResponse.AbortedTransaction> abortedTransactions = asList(
                new FetchResponse.AbortedTransaction(10, 100),
                new FetchResponse.AbortedTransaction(15, 50)
        );
        responseData.put(new TopicPartition("bar", 0), new FetchResponse.PartitionData(Errors.NONE, 100000,
                FetchResponse.INVALID_LAST_STABLE_OFFSET, FetchResponse.INVALID_LOG_START_OFFSET, abortedTransactions, records));
        responseData.put(new TopicPartition("bar", 1), new FetchResponse.PartitionData(Errors.NONE, 900000,
                5, FetchResponse.INVALID_LOG_START_OFFSET, null, records));
        responseData.put(new TopicPartition("foo", 0), new FetchResponse.PartitionData(Errors.NONE, 70000,
                6, FetchResponse.INVALID_LOG_START_OFFSET, Collections.<FetchResponse.AbortedTransaction>emptyList(), records));

        FetchResponse response = new FetchResponse(responseData, 10);
        FetchResponse deserialized = FetchResponse.parse(toBuffer(response.toStruct((short) 4)), (short) 4);
        assertEquals(responseData, deserialized.responseData());
    }

    @Test
    public void verifyFetchResponseFullWrite() throws Exception {
        FetchResponse fetchResponse = createFetchResponse();
        RequestHeader header = new RequestHeader(ApiKeys.FETCH.id, ApiKeys.FETCH.latestVersion(),
                "client", 15);

        Send send = fetchResponse.toSend("1", header);
        ByteBufferChannel channel = new ByteBufferChannel(send.size());
        send.writeTo(channel);
        channel.close();

        ByteBuffer buf = channel.buf;

        // read the size
        int size = buf.getInt();
        assertTrue(size > 0);

        // read the header
        ResponseHeader responseHeader = ResponseHeader.parse(channel.buf);
        assertEquals(header.correlationId(), responseHeader.correlationId());

        // read the body
        Struct responseBody = ApiKeys.FETCH.responseSchema(header.apiVersion()).read(buf);
        assertEquals(fetchResponse.toStruct(header.apiVersion()), responseBody);

        assertEquals(size, responseHeader.sizeOf() + responseBody.sizeOf());
    }

    @Test
    public void testControlledShutdownResponse() {
        ControlledShutdownResponse response = createControlledShutdownResponse();
        short version = ApiKeys.CONTROLLED_SHUTDOWN_KEY.latestVersion();
        Struct struct = response.toStruct(version);
        ByteBuffer buffer = toBuffer(struct);
        ControlledShutdownResponse deserialized = ControlledShutdownResponse.parse(buffer, version);
        assertEquals(response.error(), deserialized.error());
        assertEquals(response.partitionsRemaining(), deserialized.partitionsRemaining());
    }

    @Test
    public void testRequestHeaderWithNullClientId() {
        RequestHeader header = new RequestHeader((short) 10, (short) 1, null, 10);
        Struct headerStruct = header.toStruct();
        ByteBuffer buffer = toBuffer(headerStruct);
        RequestHeader deserialized = RequestHeader.parse(buffer);
        assertEquals(header.apiKey(), deserialized.apiKey());
        assertEquals(header.apiVersion(), deserialized.apiVersion());
        assertEquals(header.correlationId(), deserialized.correlationId());
        assertEquals("", deserialized.clientId()); // null is defaulted to ""
    }

    @Test(expected = UnsupportedVersionException.class)
    public void testCreateTopicRequestV0FailsIfValidateOnly() {
        createCreateTopicRequest(0, true);
    }

    @Test
    public void testFetchRequestMaxBytesOldVersions() throws Exception {
        final short version = 1;
        FetchRequest fr = createFetchRequest(version);
        FetchRequest fr2 = new FetchRequest(fr.toStruct(), version);
        assertEquals(fr2.maxBytes(), fr.maxBytes());
    }

    @Test
    public void testFetchRequestIsolationLevel() throws Exception {
        FetchRequest request = createFetchRequest(4, IsolationLevel.READ_COMMITTED);
        Struct struct = request.toStruct();
        FetchRequest deserialized = (FetchRequest) deserialize(request, struct, request.version());
        assertEquals(request.isolationLevel(), deserialized.isolationLevel());

        request = createFetchRequest(4, IsolationLevel.READ_UNCOMMITTED);
        struct = request.toStruct();
        deserialized = (FetchRequest) deserialize(request, struct, request.version());
        assertEquals(request.isolationLevel(), deserialized.isolationLevel());
    }

    @Test
    public void testJoinGroupRequestVersion0RebalanceTimeout() throws Exception {
        final short version = 0;
        JoinGroupRequest jgr = createJoinGroupRequest(version);
        JoinGroupRequest jgr2 = new JoinGroupRequest(jgr.toStruct(), version);
        assertEquals(jgr2.rebalanceTimeout(), jgr.rebalanceTimeout());
    }

    private RequestHeader createRequestHeader() {
        return new RequestHeader((short) 10, (short) 1, "", 10);
    }

    private ResponseHeader createResponseHeader() {
        return new ResponseHeader(10);
    }

    private FindCoordinatorRequest createFindCoordinatorRequest(int version) {
        return new FindCoordinatorRequest.Builder(FindCoordinatorRequest.CoordinatorType.GROUP, "test-group")
                .build((short) version);
    }

    private FindCoordinatorResponse createFindCoordinatorResponse() {
        return new FindCoordinatorResponse(Errors.NONE, new Node(10, "host1", 2014));
    }

    private FetchRequest createFetchRequest(int version, IsolationLevel isolationLevel) {
        LinkedHashMap<TopicPartition, FetchRequest.PartitionData> fetchData = new LinkedHashMap<>();
        fetchData.put(new TopicPartition("test1", 0), new FetchRequest.PartitionData(100, 0L, 1000000));
        fetchData.put(new TopicPartition("test2", 0), new FetchRequest.PartitionData(200, 0L, 1000000));
        return FetchRequest.Builder.forConsumer(100, 100000, fetchData, isolationLevel).setMaxBytes(1000).build((short) version);
    }

    private FetchRequest createFetchRequest(int version) {
        LinkedHashMap<TopicPartition, FetchRequest.PartitionData> fetchData = new LinkedHashMap<>();
        fetchData.put(new TopicPartition("test1", 0), new FetchRequest.PartitionData(100, 0L, 1000000));
        fetchData.put(new TopicPartition("test2", 0), new FetchRequest.PartitionData(200, 0L, 1000000));
        return FetchRequest.Builder.forConsumer(100, 100000, fetchData).setMaxBytes(1000).build((short) version);
    }

    private FetchResponse createFetchResponse() {
        LinkedHashMap<TopicPartition, FetchResponse.PartitionData> responseData = new LinkedHashMap<>();
        MemoryRecords records = MemoryRecords.withRecords(CompressionType.NONE, new SimpleRecord("blah".getBytes()));
        responseData.put(new TopicPartition("test", 0), new FetchResponse.PartitionData(Errors.NONE,
                1000000, FetchResponse.INVALID_LAST_STABLE_OFFSET, 0L, null, records));

        List<FetchResponse.AbortedTransaction> abortedTransactions = Collections.singletonList(
                new FetchResponse.AbortedTransaction(234L, 999L));
        responseData.put(new TopicPartition("test", 1), new FetchResponse.PartitionData(Errors.NONE,
                1000000, FetchResponse.INVALID_LAST_STABLE_OFFSET, 0L, abortedTransactions, MemoryRecords.EMPTY));

        return new FetchResponse(responseData, 25);
    }

    private HeartbeatRequest createHeartBeatRequest() {
        return new HeartbeatRequest.Builder("group1", 1, "consumer1").build();
    }

    private HeartbeatResponse createHeartBeatResponse() {
        return new HeartbeatResponse(Errors.NONE);
    }

    private JoinGroupRequest createJoinGroupRequest(int version) {
        ByteBuffer metadata = ByteBuffer.wrap(new byte[] {});
        List<JoinGroupRequest.ProtocolMetadata> protocols = new ArrayList<>();
        protocols.add(new JoinGroupRequest.ProtocolMetadata("consumer-range", metadata));
        if (version == 0) {
            return new JoinGroupRequest.Builder("group1", 30000, "consumer1", "consumer", protocols).
                    build((short) version);
        } else {
            return new JoinGroupRequest.Builder("group1", 10000, "consumer1", "consumer", protocols).
                    setRebalanceTimeout(60000).build();
        }
    }

    private JoinGroupResponse createJoinGroupResponse() {
        Map<String, ByteBuffer> members = new HashMap<>();
        members.put("consumer1", ByteBuffer.wrap(new byte[]{}));
        members.put("consumer2", ByteBuffer.wrap(new byte[]{}));
        return new JoinGroupResponse(Errors.NONE, 1, "range", "consumer1", "leader", members);
    }

    private ListGroupsRequest createListGroupsRequest() {
        return new ListGroupsRequest.Builder().build();
    }

    private ListGroupsResponse createListGroupsResponse() {
        List<ListGroupsResponse.Group> groups = Collections.singletonList(new ListGroupsResponse.Group("test-group", "consumer"));
        return new ListGroupsResponse(Errors.NONE, groups);
    }

    private DescribeGroupsRequest createDescribeGroupRequest() {
        return new DescribeGroupsRequest.Builder(singletonList("test-group")).build();
    }

    private DescribeGroupsResponse createDescribeGroupResponse() {
        String clientId = "consumer-1";
        String clientHost = "localhost";
        ByteBuffer empty = ByteBuffer.allocate(0);
        DescribeGroupsResponse.GroupMember member = new DescribeGroupsResponse.GroupMember("memberId",
                clientId, clientHost, empty, empty);
        DescribeGroupsResponse.GroupMetadata metadata = new DescribeGroupsResponse.GroupMetadata(Errors.NONE,
                "STABLE", "consumer", "roundrobin", asList(member));
        return new DescribeGroupsResponse(Collections.singletonMap("test-group", metadata));
    }

    private LeaveGroupRequest createLeaveGroupRequest() {
        return new LeaveGroupRequest.Builder("group1", "consumer1").build();
    }

    private LeaveGroupResponse createLeaveGroupResponse() {
        return new LeaveGroupResponse(Errors.NONE);
    }

    @SuppressWarnings("deprecation")
    private ListOffsetRequest createListOffsetRequest(int version) {
        if (version == 0) {
            Map<TopicPartition, ListOffsetRequest.PartitionData> offsetData = Collections.singletonMap(
                    new TopicPartition("test", 0),
                    new ListOffsetRequest.PartitionData(1000000L, 10));
            return ListOffsetRequest.Builder
                    .forConsumer(false, IsolationLevel.READ_UNCOMMITTED)
                    .setOffsetData(offsetData)
                    .build((short) version);
        } else if (version == 1) {
            Map<TopicPartition, Long> offsetData = Collections.singletonMap(
                    new TopicPartition("test", 0), 1000000L);
            return ListOffsetRequest.Builder
                    .forConsumer(true, IsolationLevel.READ_UNCOMMITTED)
                    .setTargetTimes(offsetData)
                    .build((short) version);
        } else if (version == 2) {
            Map<TopicPartition, Long> offsetData = Collections.singletonMap(
                    new TopicPartition("test", 0), 1000000L);
            return ListOffsetRequest.Builder
                    .forConsumer(true, IsolationLevel.READ_COMMITTED)
                    .setTargetTimes(offsetData)
                    .build((short) version);
        } else {
            throw new IllegalArgumentException("Illegal ListOffsetRequest version " + version);
        }
    }

    @SuppressWarnings("deprecation")
    private ListOffsetResponse createListOffsetResponse(int version) {
        if (version == 0) {
            Map<TopicPartition, ListOffsetResponse.PartitionData> responseData = new HashMap<>();
            responseData.put(new TopicPartition("test", 0),
                    new ListOffsetResponse.PartitionData(Errors.NONE, asList(100L)));
            return new ListOffsetResponse(responseData);
        } else if (version == 1 || version == 2) {
            Map<TopicPartition, ListOffsetResponse.PartitionData> responseData = new HashMap<>();
            responseData.put(new TopicPartition("test", 0),
                    new ListOffsetResponse.PartitionData(Errors.NONE, 10000L, 100L));
            return new ListOffsetResponse(responseData);
        } else {
            throw new IllegalArgumentException("Illegal ListOffsetResponse version " + version);
        }
    }

    private MetadataRequest createMetadataRequest(int version, List<String> topics) {
        return new MetadataRequest.Builder(topics).build((short) version);
    }

    private MetadataResponse createMetadataResponse() {
        Node node = new Node(1, "host1", 1001);
        List<Node> replicas = asList(node);
        List<Node> isr = asList(node);

        List<MetadataResponse.TopicMetadata> allTopicMetadata = new ArrayList<>();
        allTopicMetadata.add(new MetadataResponse.TopicMetadata(Errors.NONE, "__consumer_offsets", true,
                asList(new MetadataResponse.PartitionMetadata(Errors.NONE, 1, node, replicas, isr))));
        allTopicMetadata.add(new MetadataResponse.TopicMetadata(Errors.LEADER_NOT_AVAILABLE, "topic2", false,
                Collections.<MetadataResponse.PartitionMetadata>emptyList()));

        return new MetadataResponse(asList(node), null, MetadataResponse.NO_CONTROLLER_ID, allTopicMetadata);
    }

    private OffsetCommitRequest createOffsetCommitRequest(int version) {
        Map<TopicPartition, OffsetCommitRequest.PartitionData> commitData = new HashMap<>();
        commitData.put(new TopicPartition("test", 0), new OffsetCommitRequest.PartitionData(100, ""));
        commitData.put(new TopicPartition("test", 1), new OffsetCommitRequest.PartitionData(200, null));
        return new OffsetCommitRequest.Builder("group1", commitData)
                .setGenerationId(100)
                .setMemberId("consumer1")
                .setRetentionTime(1000000)
                .build((short) version);
    }

    private OffsetCommitResponse createOffsetCommitResponse() {
        Map<TopicPartition, Errors> responseData = new HashMap<>();
        responseData.put(new TopicPartition("test", 0), Errors.NONE);
        return new OffsetCommitResponse(responseData);
    }

    private OffsetFetchRequest createOffsetFetchRequest(int version) {
        return new OffsetFetchRequest.Builder("group1", singletonList(new TopicPartition("test11", 1)))
                .build((short) version);
    }

    private OffsetFetchResponse createOffsetFetchResponse() {
        Map<TopicPartition, OffsetFetchResponse.PartitionData> responseData = new HashMap<>();
        responseData.put(new TopicPartition("test", 0), new OffsetFetchResponse.PartitionData(100L, "", Errors.NONE));
        responseData.put(new TopicPartition("test", 1), new OffsetFetchResponse.PartitionData(100L, null, Errors.NONE));
        return new OffsetFetchResponse(Errors.NONE, responseData);
    }

    private ProduceRequest createProduceRequest(int version) {
        if (version < 2)
            throw new IllegalArgumentException("Produce request version 2 is not supported");

        byte magic = version == 2 ? RecordBatch.MAGIC_VALUE_V1 : RecordBatch.MAGIC_VALUE_V2;
        MemoryRecords records = MemoryRecords.withRecords(magic, CompressionType.NONE, new SimpleRecord("woot".getBytes()));
        Map<TopicPartition, MemoryRecords> produceData = Collections.singletonMap(new TopicPartition("test", 0), records);
        return new ProduceRequest.Builder(magic, (short) 1, 5000, produceData).build((short) version);
    }

    private ProduceResponse createProduceResponse() {
        Map<TopicPartition, ProduceResponse.PartitionResponse> responseData = new HashMap<>();
        responseData.put(new TopicPartition("test", 0), new ProduceResponse.PartitionResponse(Errors.NONE,
                10000, RecordBatch.NO_TIMESTAMP));
        return new ProduceResponse(responseData, 0);
    }

    private StopReplicaRequest createStopReplicaRequest(boolean deletePartitions) {
        Set<TopicPartition> partitions = Utils.mkSet(new TopicPartition("test", 0));
        return new StopReplicaRequest.Builder(0, 1, deletePartitions, partitions).build();
    }

    private StopReplicaResponse createStopReplicaResponse() {
        Map<TopicPartition, Errors> responses = new HashMap<>();
        responses.put(new TopicPartition("test", 0), Errors.NONE);
        return new StopReplicaResponse(Errors.NONE, responses);
    }

    private ControlledShutdownRequest createControlledShutdownRequest() {
        return new ControlledShutdownRequest.Builder(10).build();
    }

    private ControlledShutdownResponse createControlledShutdownResponse() {
        Set<TopicPartition> topicPartitions = Utils.mkSet(
                new TopicPartition("test2", 5),
                new TopicPartition("test1", 10)
        );
        return new ControlledShutdownResponse(Errors.NONE, topicPartitions);
    }

    private LeaderAndIsrRequest createLeaderAndIsrRequest() {
        Map<TopicPartition, PartitionState> partitionStates = new HashMap<>();
        List<Integer> isr = asList(1, 2);
        List<Integer> replicas = asList(1, 2, 3, 4);
        partitionStates.put(new TopicPartition("topic5", 105),
                new PartitionState(0, 2, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));
        partitionStates.put(new TopicPartition("topic5", 1),
                new PartitionState(1, 1, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));
        partitionStates.put(new TopicPartition("topic20", 1),
                new PartitionState(1, 0, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));

        Set<Node> leaders = Utils.mkSet(
                new Node(0, "test0", 1223),
                new Node(1, "test1", 1223)
        );

        return new LeaderAndIsrRequest.Builder(1, 10, partitionStates, leaders).build();
    }

    private LeaderAndIsrResponse createLeaderAndIsrResponse() {
        Map<TopicPartition, Errors> responses = new HashMap<>();
        responses.put(new TopicPartition("test", 0), Errors.NONE);
        return new LeaderAndIsrResponse(Errors.NONE, responses);
    }

    private UpdateMetadataRequest createUpdateMetadataRequest(int version, String rack) {
        Map<TopicPartition, PartitionState> partitionStates = new HashMap<>();
        List<Integer> isr = asList(1, 2);
        List<Integer> replicas = asList(1, 2, 3, 4);
        partitionStates.put(new TopicPartition("topic5", 105),
                new PartitionState(0, 2, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));
        partitionStates.put(new TopicPartition("topic5", 1),
                new PartitionState(1, 1, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));
        partitionStates.put(new TopicPartition("topic20", 1),
                new PartitionState(1, 0, 1, new ArrayList<>(isr), 2, new HashSet<>(replicas)));

        SecurityProtocol plaintext = SecurityProtocol.PLAINTEXT;
        List<UpdateMetadataRequest.EndPoint> endPoints1 = new ArrayList<>();
        endPoints1.add(new UpdateMetadataRequest.EndPoint("host1", 1223, plaintext,
                ListenerName.forSecurityProtocol(plaintext)));

        List<UpdateMetadataRequest.EndPoint> endPoints2 = new ArrayList<>();
        endPoints2.add(new UpdateMetadataRequest.EndPoint("host1", 1244, plaintext,
                ListenerName.forSecurityProtocol(plaintext)));
        if (version > 0) {
            SecurityProtocol ssl = SecurityProtocol.SSL;
            endPoints2.add(new UpdateMetadataRequest.EndPoint("host2", 1234, ssl,
                    ListenerName.forSecurityProtocol(ssl)));
            endPoints2.add(new UpdateMetadataRequest.EndPoint("host2", 1334, ssl,
                    new ListenerName("CLIENT")));
        }

        Set<UpdateMetadataRequest.Broker> liveBrokers = Utils.mkSet(
                new UpdateMetadataRequest.Broker(0, endPoints1, rack),
                new UpdateMetadataRequest.Broker(1, endPoints2, rack)
        );
        return new UpdateMetadataRequest.Builder((short) version, 1, 10, partitionStates,
                liveBrokers).build();
    }

    private UpdateMetadataResponse createUpdateMetadataResponse() {
        return new UpdateMetadataResponse(Errors.NONE);
    }

    private SaslHandshakeRequest createSaslHandshakeRequest() {
        return new SaslHandshakeRequest("PLAIN");
    }

    private SaslHandshakeResponse createSaslHandshakeResponse() {
        return new SaslHandshakeResponse(Errors.NONE, singletonList("GSSAPI"));
    }

    private ApiVersionsRequest createApiVersionRequest() {
        return new ApiVersionsRequest.Builder().build();
    }

    private ApiVersionsResponse createApiVersionResponse() {
        List<ApiVersionsResponse.ApiVersion> apiVersions = asList(new ApiVersionsResponse.ApiVersion((short) 0, (short) 0, (short) 2));
        return new ApiVersionsResponse(Errors.NONE, apiVersions);
    }

    private CreateTopicsRequest createCreateTopicRequest(int version) {
        return createCreateTopicRequest(version, version >= 1);
    }

    private CreateTopicsRequest createCreateTopicRequest(int version, boolean validateOnly) {
        CreateTopicsRequest.TopicDetails request1 = new CreateTopicsRequest.TopicDetails(3, (short) 5);

        Map<Integer, List<Integer>> replicaAssignments = new HashMap<>();
        replicaAssignments.put(1, asList(1, 2, 3));
        replicaAssignments.put(2, asList(2, 3, 4));

        Map<String, String> configs = new HashMap<>();
        configs.put("config1", "value1");

        CreateTopicsRequest.TopicDetails request2 = new CreateTopicsRequest.TopicDetails(replicaAssignments, configs);

        Map<String, CreateTopicsRequest.TopicDetails> request = new HashMap<>();
        request.put("my_t1", request1);
        request.put("my_t2", request2);
        return new CreateTopicsRequest.Builder(request, 0, validateOnly).build((short) version);
    }

    private CreateTopicsResponse createCreateTopicResponse() {
        Map<String, CreateTopicsResponse.Error> errors = new HashMap<>();
        errors.put("t1", new CreateTopicsResponse.Error(Errors.INVALID_TOPIC_EXCEPTION, null));
        errors.put("t2", new CreateTopicsResponse.Error(Errors.LEADER_NOT_AVAILABLE, "Leader with id 5 is not available."));
        return new CreateTopicsResponse(errors);
    }

    private DeleteTopicsRequest createDeleteTopicsRequest() {
        return new DeleteTopicsRequest.Builder(Utils.mkSet("my_t1", "my_t2"), 10000).build();
    }

    private DeleteTopicsResponse createDeleteTopicsResponse() {
        Map<String, Errors> errors = new HashMap<>();
        errors.put("t1", Errors.INVALID_TOPIC_EXCEPTION);
        errors.put("t2", Errors.TOPIC_AUTHORIZATION_FAILED);
        return new DeleteTopicsResponse(errors);
    }

    private InitPidRequest createInitPidRequest() {
        return new InitPidRequest.Builder(null, 100).build();
    }

    private InitPidResponse createInitPidResponse() {
        return new InitPidResponse(0, Errors.NONE, 3332, (short) 3);
    }


    private OffsetsForLeaderEpochRequest createLeaderEpochRequest() {
        Map<TopicPartition, Integer> epochs = new HashMap<>();

        epochs.put(new TopicPartition("topic1", 0), 1);
        epochs.put(new TopicPartition("topic1", 1), 1);
        epochs.put(new TopicPartition("topic2", 2), 3);

        return new OffsetsForLeaderEpochRequest.Builder(epochs).build();
    }

    private OffsetsForLeaderEpochResponse createLeaderEpochResponse() {
        Map<TopicPartition, EpochEndOffset> epochs = new HashMap<>();

        epochs.put(new TopicPartition("topic1", 0), new EpochEndOffset(Errors.NONE, 0));
        epochs.put(new TopicPartition("topic1", 1), new EpochEndOffset(Errors.NONE, 1));
        epochs.put(new TopicPartition("topic2", 2), new EpochEndOffset(Errors.NONE, 2));

        return new OffsetsForLeaderEpochResponse(epochs);
    }

    private AddPartitionsToTxnRequest createAddPartitionsToTxnRequest() {
        return new AddPartitionsToTxnRequest.Builder("tid", 21L, (short) 42,
            Collections.singletonList(new TopicPartition("topic", 73))).build();
    }

    private AddPartitionsToTxnResponse createAddPartitionsToTxnResponse() {
        return new AddPartitionsToTxnResponse(0, Errors.NONE);
    }

    private AddOffsetsToTxnRequest createAddOffsetsToTxnRequest() {
        return new AddOffsetsToTxnRequest.Builder("tid", 21L, (short) 42, "gid").build();
    }

    private AddOffsetsToTxnResponse createAddOffsetsToTxnResponse() {
        return new AddOffsetsToTxnResponse(0, Errors.NONE);
    }

    private EndTxnRequest createEndTxnRequest() {
        return new EndTxnRequest.Builder("tid", 21L, (short) 42, TransactionResult.COMMIT).build();
    }

    private EndTxnResponse createEndTxnResponse() {
        return new EndTxnResponse(0, Errors.NONE);
    }

    private WriteTxnMarkersRequest createWriteTxnMarkersRequest() {
        return new WriteTxnMarkersRequest.Builder(
            Collections.singletonList(new WriteTxnMarkersRequest.TxnMarkerEntry(21L, (short) 42, 73, TransactionResult.ABORT,
                                                                                Collections.singletonList(new TopicPartition("topic", 73))))).build();
    }

    private WriteTxnMarkersResponse createWriteTxnMarkersResponse() {
        final Map<TopicPartition, Errors> errorPerPartitions = new HashMap<>();
        errorPerPartitions.put(new TopicPartition("topic", 73), Errors.NONE);
        final Map<Long, Map<TopicPartition, Errors>> response = new HashMap<>();
        response.put(21L, errorPerPartitions);
        return new WriteTxnMarkersResponse(response);
    }

    private TxnOffsetCommitRequest createTxnOffsetCommitRequest() {
        final Map<TopicPartition, TxnOffsetCommitRequest.CommittedOffset> offsets = new HashMap<>();
        offsets.put(new TopicPartition("topic", 73),
                    new TxnOffsetCommitRequest.CommittedOffset(100, null));
        return new TxnOffsetCommitRequest.Builder("gid", 21L, (short) 42, 73, offsets).build();
    }

    private TxnOffsetCommitResponse createTxnOffsetCommitResponse() {
        final Map<TopicPartition, Errors> errorPerPartitions = new HashMap<>();
        errorPerPartitions.put(new TopicPartition("topic", 73), Errors.NONE);
        return new TxnOffsetCommitResponse(0, errorPerPartitions);
    }

    private static class ByteBufferChannel implements GatheringByteChannel {
        private final ByteBuffer buf;
        private boolean closed = false;

        private ByteBufferChannel(long size) {
            if (size > Integer.MAX_VALUE)
                throw new IllegalArgumentException("size should be not be greater than Integer.MAX_VALUE");
            this.buf = ByteBuffer.allocate((int) size);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            int position = buf.position();
            for (int i = 0; i < length; i++) {
                ByteBuffer src = srcs[i].duplicate();
                if (i == 0)
                    src.position(offset);
                buf.put(src);
            }
            return buf.position() - position;
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            return write(srcs, 0, srcs.length);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int position = buf.position();
            buf.put(src);
            return buf.position() - position;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() throws IOException {
            buf.flip();
            closed = true;
        }
    }
}
