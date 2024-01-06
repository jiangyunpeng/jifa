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
package org.eclipse.jifa.server.service;

import org.eclipse.jifa.common.domain.vo.PageView;
import org.eclipse.jifa.server.domain.dto.FileTransferProgress;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.dto.FileView;
import org.eclipse.jifa.server.domain.dto.NamedResource;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.eclipse.jifa.server.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileService {

    PageView<FileView> getUserFileViews(FileType type, int page, int pageSize);

    FileView getFileViewById(long fileId);

    FileView getFileViewByUniqueName(String uniqueName);

    void deleteById(long fileId);

    //处理从外部存储传输
    long handleTransferRequest(FileTransferRequest request) throws Throwable;

    FileTransferProgress getTransferProgress(long transferringFileId);

    //处理文件上传
    long handleUploadRequest(FileType type, MultipartFile file) throws Throwable;

    String handleLocalFileRequest(FileType type, Path path) throws IOException;

    NamedResource handleDownloadRequest(long fileId) throws Throwable;

    FileEntity getFileByUniqueName(String uniqueName, FileType expectedFileType);

    void deleteOldestFile();
}
