/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.elasticsearch.client.http;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.HttpClusterAdminActionModule;
import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.action.support.HttpAction;
import org.elasticsearch.client.GenericClient;
import org.elasticsearch.client.support.AbstractClusterAdminClient;
import org.elasticsearch.client.internal.InternalClientSettingsPreparer;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.compress.BasicCompressorFactory;
import org.elasticsearch.common.io.CachedStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.ThreadLocals;
import org.elasticsearch.env.ClientEnvironment;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.client.ClientThreadPool;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;


public class HttpClusterAdminClient extends AbstractClusterAdminClient implements GenericClient {

    private final Settings settings;

    private final ClientEnvironment environment;

    private final ThreadPool threadPool;

    private final HttpClusterAdminActionModule actions = new HttpClusterAdminActionModule();
    
    public HttpClusterAdminClient() throws ElasticSearchException {
        this(ImmutableSettings.Builder.EMPTY_SETTINGS, true);
    }

    public HttpClusterAdminClient(Settings settings) {
        this(settings, true);
    }

    public HttpClusterAdminClient(Settings.Builder settings) {
        this(settings.build(), true);
    }

    public HttpClusterAdminClient(Settings.Builder settings, boolean loadConfigSettings) throws ElasticSearchException {
        this(settings.build(), loadConfigSettings);
    }

    public HttpClusterAdminClient(Settings pSettings, boolean loadConfigSettings) throws ElasticSearchException {
        Tuple<Settings, ClientEnvironment> tuple = InternalClientSettingsPreparer.prepareSettings(pSettings, loadConfigSettings);
        Settings settings = settingsBuilder().put(tuple.v1())
                .put("network.server", false)
                .put("node.client", true)
                .build();
        this.environment = tuple.v2();
        this.settings = settings;

        BasicCompressorFactory.configure(settings);

        threadPool = new ClientThreadPool();
    }

    public ImmutableList<TransportAddress> transportAddresses() {
        return null; //nodesService.transportAddresses();
    }

    public HttpClusterAdminClient addTransportAddress(TransportAddress transportAddress) {
        //nodesService.addTransportAddresses(transportAddress);
        return this;
    }

    public HttpClusterAdminClient addTransportAddresses(TransportAddress... transportAddress) {
        //nodesService.addTransportAddresses(transportAddress);
        return this;
    }

    public HttpClusterAdminClient removeTransportAddress(TransportAddress transportAddress) {
        //nodesService.removeTransportAddress(transportAddress);
        return this;
    }

    @Override
    public ThreadPool threadPool() {
        return threadPool;
    }
    
    @Override
    public void close() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        try {
            threadPool.shutdownNow();
        } catch (Exception e) {
            // ignore
        }

        CachedStreams.clear();
        ThreadLocals.clearReferencesThreadLocals();
    }

    @Override
    public Settings settings() {
        return this.settings;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>, Client extends GenericClient> 
            ActionFuture<Response> execute(Action<Request, Response, RequestBuilder, Client> action, Request request) {
        HttpAction<HttpClusterAdminClient, Request,Response> httpAction = actions.getAction(action.name());
        return httpAction.execute(this, request);
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>, Client extends GenericClient> 
            void execute(Action<Request, Response, RequestBuilder, Client> action, Request request, ActionListener<Response> listener) {
        HttpAction<HttpClusterAdminClient, Request, Response> httpAction = actions.getAction(action.name());
        httpAction.execute(this, request, listener);
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>, Client extends GenericClient> 
            RequestBuilder prepareExecute(Action<Request, Response, RequestBuilder, Client> action) {
        return action.newRequestBuilder((Client)this);
    }

    // ClusterAction interface. Hm.
    
    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> 
            ActionFuture<Response> execute(ClusterAction<Request, Response, RequestBuilder> action, Request request) {
        HttpAction<HttpClusterAdminClient, Request,Response> httpAction = actions.getAction(action.name());
        return httpAction.execute(this, request);
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> 
            void execute(ClusterAction<Request, Response, RequestBuilder> action, Request request, ActionListener<Response> listener) {
        HttpAction<HttpClusterAdminClient, Request, Response> httpAction = actions.getAction(action.name());
        httpAction.execute(this, request, listener);
    }


}