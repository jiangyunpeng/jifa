/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.server.service.impl;

import org.eclipse.jifa.common.util.Validate;
import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.condition.StaticSchedulingStrategy;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.eclipse.jifa.server.domain.entity.static_cluster.StaticWorkerEntity;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.repository.FileStaticWorkerBindRepo;
import org.eclipse.jifa.server.repository.StaticWorkerRepo;
import org.eclipse.jifa.server.service.StaticWorkerService;
import org.eclipse.jifa.server.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import static org.eclipse.jifa.common.domain.exception.CommonException.CE;
import static org.eclipse.jifa.common.enums.CommonErrorCode.INTERNAL_ERROR;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.server.Constant.HTTP_API_PREFIX;
import static org.eclipse.jifa.server.enums.ServerErrorCode.STATIC_WORKER_UNAVAILABLE;

@Master
@StaticSchedulingStrategy
@Service
public class StaticWorkerServiceImpl extends AbstractWorkerServiceImpl implements StaticWorkerService {

    private final FileStaticWorkerBindRepo fileStaticWorkerBindRepo;

    private final StaticWorkerRepo staticWorkerRepo;

    public StaticWorkerServiceImpl(UserService userService,
                                   FileStaticWorkerBindRepo fileStaticWorkerBindRepo,
                                   StaticWorkerRepo staticWorkerRepo) {
        super(userService);
        this.fileStaticWorkerBindRepo = fileStaticWorkerBindRepo;
        this.staticWorkerRepo = staticWorkerRepo;
    }

    @Override
    public WorkerEntity resolveForAnalysisApiRequest(FileEntity target) {
        Validate.isTrue(isMaster(), INTERNAL_ERROR);
        return fileStaticWorkerBindRepo.findByFileId(target.getId()).orElseThrow(() -> CE(INTERNAL_ERROR))
                                       .getStaticWorker();
    }

    @Override
    public StaticWorkerEntity selectForFileTransferRequest(FileTransferRequest request) {
        Validate.isTrue(isMaster(), INTERNAL_ERROR);
        return staticWorkerRepo.findFirstByOrderByAvailableSpaceDesc()
                               .orElseThrow(() -> CE(STATIC_WORKER_UNAVAILABLE));
    }

    @Override
    public StaticWorkerEntity selectForFileUpload(FileType type, MultipartFile file) {
        Validate.isTrue(isMaster(), INTERNAL_ERROR);
        return staticWorkerRepo.findFirstByOrderByAvailableSpaceDesc()
                               .orElseThrow(() -> CE(STATIC_WORKER_UNAVAILABLE));
    }

    @Override
    public long handleUploadRequest(WorkerEntity worker, FileType type, MultipartFile file) throws Throwable {
        Validate.isTrue(isMaster(), INTERNAL_ERROR);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            builder.part("file", new ByteArrayResource(file.getBytes()))
                   .filename(file.getOriginalFilename())
                   .contentType(MediaType.APPLICATION_OCTET_STREAM);
            builder.part("type", type.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //发送http请求给worker
        UriBuilder uriBuilder = new DefaultUriBuilderFactory().builder()
                                                              .scheme("http")
                                                              .host(worker.getHostAddress())
                                                              .port(worker.getPort())
                                                              .path(HTTP_API_PREFIX + "/files/upload");

        WebClient.RequestBodySpec spec = webClient.method(HttpMethod.POST)
                                                  .uri(uriBuilder.build());

        String jwtToken = userService.getCurrentUserJwtTokenOrNull();
        if (jwtToken != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
        }

        return spec.contentType(MediaType.MULTIPART_FORM_DATA)
                   .body(BodyInserters.fromMultipartData(builder.build()))
                   .exchangeToMono(response -> {
                       if (!response.statusCode().is2xxSuccessful()) {
                           return response.createError();
                       }
                       return response.bodyToMono(String.class).map(s -> GSON.fromJson(s, Long.class));
                   }).toFuture().get();
    }

    @Override
    public Resource handleDownloadRequest(WorkerEntity worker, long fileId) throws MalformedURLException {
        Validate.isTrue(isMaster(), INTERNAL_ERROR);
        UriBuilder uriBuilder = new DefaultUriBuilderFactory().builder()
                                                              .scheme("http")
                                                              .host(worker.getHostAddress())
                                                              .port(worker.getPort())
                                                              .path(HTTP_API_PREFIX + "/files/" + fileId + "/download");
        return new UrlResource(uriBuilder.build()) {
            @Override
            protected void customizeConnection(@NotNull HttpURLConnection con) throws IOException {
                super.customizeConnection(con);
                String jwtToken = userService.getCurrentUserJwtTokenOrNull();
                if (jwtToken != null) {
                    con.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + userService.getCurrentUserJwtTokenOrNull());
                }
            }
        };
    }
}
