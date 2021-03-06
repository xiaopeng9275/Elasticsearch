/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client;

import org.apache.http.Header;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * A wrapper for the {@link RestHighLevelClient} that provides methods for accessing the Cluster API.
 * <p>
 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster.html">Cluster API on elastic.co</a>
 */
public final class ClusterClient {
    private final RestHighLevelClient restHighLevelClient;

    ClusterClient(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    /**
     * Updates cluster wide specific settings using the Cluster Update Settings API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-update-settings.html"> Cluster Update Settings
     * API on elastic.co</a>
     * @param clusterUpdateSettingsRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @throws IOException in case there is a problem sending the request or parsing back the response
     */
    public ClusterUpdateSettingsResponse putSettings(ClusterUpdateSettingsRequest clusterUpdateSettingsRequest, RequestOptions options)
            throws IOException {
        return restHighLevelClient.performRequestAndParseEntity(clusterUpdateSettingsRequest, RequestConverters::clusterPutSettings,
                options, ClusterUpdateSettingsResponse::fromXContent, emptySet());
    }

    /**
     * Updates cluster wide specific settings using the Cluster Update Settings API.
     * <p>
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-update-settings.html"> Cluster Update Settings
     * API on elastic.co</a>
     * @deprecated Prefer {@link #putSettings(ClusterUpdateSettingsRequest, RequestOptions)}
     */
    @Deprecated
    public ClusterUpdateSettingsResponse putSettings(ClusterUpdateSettingsRequest clusterUpdateSettingsRequest, Header... headers)
            throws IOException {
        return restHighLevelClient.performRequestAndParseEntity(clusterUpdateSettingsRequest, RequestConverters::clusterPutSettings,
                ClusterUpdateSettingsResponse::fromXContent, emptySet(), headers);
    }

    /**
     * Asynchronously updates cluster wide specific settings using the Cluster Update Settings API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-update-settings.html"> Cluster Update Settings
     * API on elastic.co</a>
     * @param clusterUpdateSettingsRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     */
    public void putSettingsAsync(ClusterUpdateSettingsRequest clusterUpdateSettingsRequest, RequestOptions options,
                                 ActionListener<ClusterUpdateSettingsResponse> listener) {
        restHighLevelClient.performRequestAsyncAndParseEntity(clusterUpdateSettingsRequest, RequestConverters::clusterPutSettings,
                options, ClusterUpdateSettingsResponse::fromXContent, listener, emptySet());
    }
    /**
     * Asynchronously updates cluster wide specific settings using the Cluster Update Settings API.
     * <p>
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-update-settings.html"> Cluster Update Settings
     * API on elastic.co</a>
     * @deprecated Prefer {@link #putSettingsAsync(ClusterUpdateSettingsRequest, RequestOptions, ActionListener)}
     */
    @Deprecated
    public void putSettingsAsync(ClusterUpdateSettingsRequest clusterUpdateSettingsRequest,
            ActionListener<ClusterUpdateSettingsResponse> listener, Header... headers) {
        restHighLevelClient.performRequestAsyncAndParseEntity(clusterUpdateSettingsRequest, RequestConverters::clusterPutSettings,
                ClusterUpdateSettingsResponse::fromXContent, listener, emptySet(), headers);
    }

    /**
     * Get the cluster wide settings using the Cluster Get Settings API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-get-settings.html"> Cluster Get Settings
     * API on elastic.co</a>
     * @param clusterGetSettingsRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @throws IOException in case there is a problem sending the request or parsing back the response
     */
    public ClusterGetSettingsResponse getSettings(ClusterGetSettingsRequest clusterGetSettingsRequest, RequestOptions options)
        throws IOException {
        return restHighLevelClient.performRequestAndParseEntity(clusterGetSettingsRequest, RequestConverters::clusterGetSettings,
            options, ClusterGetSettingsResponse::fromXContent, emptySet());
    }

    /**
     * Asynchronously get the cluster wide settings using the Cluster Get Settings API.
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-get-settings.html"> Cluster Get Settings
     * API on elastic.co</a>
     * @param clusterGetSettingsRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     */
    public void getSettingsAsync(ClusterGetSettingsRequest clusterGetSettingsRequest, RequestOptions options,
                                 ActionListener<ClusterGetSettingsResponse> listener) {
        restHighLevelClient.performRequestAsyncAndParseEntity(clusterGetSettingsRequest, RequestConverters::clusterGetSettings,
            options, ClusterGetSettingsResponse::fromXContent, listener, emptySet());
    }

    /**
     * Get cluster health using the Cluster Health API.
     * See
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html"> Cluster Health API on elastic.co</a>
     * <p>
     * If timeout occurred, {@link ClusterHealthResponse} will have isTimedOut() == true and status() == RestStatus.REQUEST_TIMEOUT
     * @param healthRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @return the response
     * @throws IOException in case there is a problem sending the request or parsing back the response
     */
    public ClusterHealthResponse health(ClusterHealthRequest healthRequest, RequestOptions options) throws IOException {
        return restHighLevelClient.performRequestAndParseEntity(healthRequest, RequestConverters::clusterHealth, options,
                ClusterHealthResponse::fromXContent, singleton(RestStatus.REQUEST_TIMEOUT.getStatus()));
    }

    /**
     * Asynchronously get cluster health using the Cluster Health API.
     * See
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html"> Cluster Health API on elastic.co</a>
     * If timeout occurred, {@link ClusterHealthResponse} will have isTimedOut() == true and status() == RestStatus.REQUEST_TIMEOUT
     * @param healthRequest the request
     * @param options the request options (e.g. headers), use {@link RequestOptions#DEFAULT} if nothing needs to be customized
     * @param listener the listener to be notified upon request completion
     */
    public void healthAsync(ClusterHealthRequest healthRequest, RequestOptions options, ActionListener<ClusterHealthResponse> listener) {
        restHighLevelClient.performRequestAsyncAndParseEntity(healthRequest, RequestConverters::clusterHealth, options,
                ClusterHealthResponse::fromXContent, listener, singleton(RestStatus.REQUEST_TIMEOUT.getStatus()));
    }
}
