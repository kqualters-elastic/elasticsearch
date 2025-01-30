/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.enrich.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeReadProjectAction;
import org.elasticsearch.cluster.ProjectState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.project.ProjectResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.enrich.EnrichPolicy;
import org.elasticsearch.xpack.core.enrich.action.GetEnrichPolicyAction;
import org.elasticsearch.xpack.enrich.EnrichStore;

import java.util.HashMap;
import java.util.Map;

public class TransportGetEnrichPolicyAction extends TransportMasterNodeReadProjectAction<
    GetEnrichPolicyAction.Request,
    GetEnrichPolicyAction.Response> {

    @Inject
    public TransportGetEnrichPolicyAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        ProjectResolver projectResolver,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            GetEnrichPolicyAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            GetEnrichPolicyAction.Request::new,
            projectResolver,
            GetEnrichPolicyAction.Response::new,
            EsExecutors.DIRECT_EXECUTOR_SERVICE
        );
    }

    @Override
    protected void masterOperation(
        Task task,
        GetEnrichPolicyAction.Request request,
        ProjectState state,
        ActionListener<GetEnrichPolicyAction.Response> listener
    ) throws Exception {
        Map<String, EnrichPolicy> policies;
        if (request.getNames() == null || request.getNames().isEmpty()) {
            policies = EnrichStore.getPolicies(state.metadata());
        } else {
            policies = new HashMap<>();
            for (String name : request.getNames()) {
                if (name.isEmpty() == false) {
                    EnrichPolicy policy = EnrichStore.getPolicy(name, state.metadata());
                    if (policy != null) {
                        policies.put(name, policy);
                    }
                }
            }
        }
        listener.onResponse(new GetEnrichPolicyAction.Response(policies));
    }

    @Override
    protected ClusterBlockException checkBlock(GetEnrichPolicyAction.Request request, ProjectState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
