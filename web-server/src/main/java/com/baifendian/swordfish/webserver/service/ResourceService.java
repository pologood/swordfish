/*
 * Copyright (C) 2017 Baifendian Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baifendian.swordfish.webserver.service;

import com.baifendian.swordfish.common.config.BaseConfig;
import com.baifendian.swordfish.common.hadoop.HdfsClient;
import com.baifendian.swordfish.common.utils.CommonUtil;
import com.baifendian.swordfish.dao.mapper.ProjectMapper;
import com.baifendian.swordfish.dao.mapper.ResourceMapper;
import com.baifendian.swordfish.dao.model.Project;
import com.baifendian.swordfish.dao.model.Resource;
import com.baifendian.swordfish.dao.model.User;
import com.baifendian.swordfish.webserver.service.storage.FileSystemStorageService;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

  private static Logger logger = LoggerFactory.getLogger(ResourceService.class.getName());

  @Autowired
  private ResourceMapper resourceMapper;

  @Autowired
  private ProjectService projectService;

  @Autowired
  private ProjectMapper projectMapper;

  @Autowired
  private FileSystemStorageService fileSystemStorageService;

  /**
   * 创建资源:<br>
   * 1) 先下载资源到本地, 为了避免并发问题, 随机生成一个文件名称<br>
   * 2) 拷贝到 hdfs 中, 给予合适的文件名称(包含后缀)<p>
   *
   * @param operator
   * @param projectName
   * @param name
   * @param desc
   * @param file
   * @param response
   * @return
   */
  @Transactional(value = "TransactionManager")
  public Resource createResource(User operator,
                                 String projectName,
                                 String name,
                                 String desc,
                                 MultipartFile file,
                                 HttpServletResponse response) {

    // 文件为空
    if (file.isEmpty()) {
      logger.error("File is empty: {}", file.getOriginalFilename());
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    // 判断是否具备相应的权限, 必须具备写权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasWritePerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 判断文件是否已经存在
    Resource resource = resourceMapper.queryResource(project.getId(), name);

    if (resource != null) {
      logger.error("Resource {} does not empty", name);
      response.setStatus(HttpStatus.SC_CONFLICT);
      return null;
    }

    // 插入数据
    resource = new Resource();
    Date now = new Date();

    resource.setName(name);

    resource.setOriginFilename(file.getOriginalFilename());
    resource.setDesc(desc);
    resource.setOwnerId(operator.getId());
    resource.setOwner(operator.getName());
    resource.setProjectId(project.getId());
    resource.setProjectName(projectName);
    resource.setCreateTime(now);
    resource.setModifyTime(now);

    try {
      resourceMapper.insert(resource);
    } catch (DuplicateKeyException e) {
      logger.error("Resource has exist, can't create again.", e);
      response.setStatus(HttpStatus.SC_CONFLICT);
      return null;
    }

    // 上传失败
    if (!upload(project, name, file)) {
      logger.error("upload resource: {} file: {} failed.", name, file.getOriginalFilename());
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      throw new IllegalArgumentException("file suffix must the same with resource name suffix");
    }

    response.setStatus(HttpStatus.SC_CREATED);
    return resource;
  }

  /**
   * 修改资源, 不存在则创建
   *
   * @param operator
   * @param projectName
   * @param name
   * @param desc
   * @param file
   * @param response
   * @return
   */
  public Resource putResource(User operator,
                              String projectName,
                              String name,
                              String desc,
                              MultipartFile file,
                              HttpServletResponse response) {
    Resource resource = resourceMapper.queryByProjectAndResName(projectName, name);

    if (resource == null) {
      return createResource(operator, projectName, name, desc, file, response);
    }

    return modifyResource(operator, projectName, name, desc, file, response);
  }

  /**
   * 修改资源
   *
   * @param operator
   * @param projectName
   * @param name
   * @param desc
   * @param file
   * @param response
   * @return
   */
  @Transactional(value = "TransactionManager")
  public Resource modifyResource(User operator,
                                 String projectName,
                                 String name,
                                 String desc,
                                 MultipartFile file,
                                 HttpServletResponse response) {
    // 文件为空
    if (file != null && file.isEmpty()) {
      logger.error("File does not null but empty: {}", file.getOriginalFilename());
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    // 判断是否具备相应的权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasWritePerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 判断资源是否已经存在, 如果不存在, 直接返回
    Resource resource = resourceMapper.queryResource(project.getId(), name);

    if (resource == null) {
      logger.error("Resource {} does not exist", name);
      response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      return null;
    }

    Date now = new Date();

    if (file != null) {
      resource.setOriginFilename(file.getOriginalFilename());
    }

    if (desc != null) {
      resource.setDesc(desc);
    }

    resource.setOwnerId(operator.getId());
    resource.setModifyTime(now);

    int count = resourceMapper.update(resource);

    if (count <= 0) {
      logger.error("Resource {} upload failed", name);
      response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      return null;
    }

    // 上传失败
    if (!upload(project, name, file)) {
      logger.error("upload resource: {} file: {} failed.", name, file.getOriginalFilename());
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      throw new IllegalArgumentException("file suffix must the same with resource name suffix");
    }

    return resource;
  }

  /**
   * 上传文件到 hdfs 中
   *
   * @param project
   * @param name
   * @param file
   */
  private boolean upload(Project project, String name, MultipartFile file) {
    // 保存到本地
    String fileSuffix = CommonUtil.fileSuffix(file.getOriginalFilename()); // file suffix
    String nameSuffix = CommonUtil.fileSuffix(name);

    // 判断后缀
    if (!StringUtils.equals(fileSuffix, nameSuffix)) {
      return false;
    }

    String localFilename = BaseConfig.getLocalResourceFilename(project.getId(), UUID.randomUUID().toString()); // 随机的一个文件名称

    fileSystemStorageService.store(file, localFilename);

    // 保存到 hdfs 并删除源文件
    String hdfsFilename = BaseConfig.getHdfsResourcesFilename(project.getId(), name);
    HdfsClient.getInstance().copyLocalToHdfs(localFilename, hdfsFilename, true, true);

    return true;
  }

  /**
   * 拷贝资源
   *
   * @param operator
   * @param projectName
   * @param srcResName
   * @param destResName
   * @param desc
   * @param response
   * @return
   */
  @Transactional(value = "TransactionManager")
  public Resource copyResource(User operator,
                               String projectName,
                               String srcResName,
                               String destResName,
                               String desc,
                               HttpServletResponse response) {

    // 源和目标不能是一样的名称
    if (StringUtils.equalsIgnoreCase(srcResName, destResName)) {
      logger.error("Src resource mustn't the same to dest resource.");
      response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      return null;
    }

    // 源和目标后缀必须一样
    if (!StringUtils.equals(CommonUtil.fileSuffix(srcResName), CommonUtil.fileSuffix(destResName))) {
      logger.error("Src resource suffix must the same to dest resource suffix.");
      response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      return null;
    }

    // 判断是否具备相应的权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasWritePerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 判断源是否存在
    Resource srcResource = resourceMapper.queryResource(project.getId(), srcResName);

    if (srcResource == null) {
      logger.error("Resource {} does not exist", srcResName);
      response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      return null;
    }

    // 判断目标是否存在, 不存在则需要创建
    Resource destResource = resourceMapper.queryResource(project.getId(), destResName);

    Date now = new Date();

    // 不存在
    if (destResource == null) {
      destResource = new Resource();

      destResource.setName(destResName);
      destResource.setOriginFilename(srcResource.getOriginFilename());
      destResource.setDesc(desc);
      destResource.setOwnerId(operator.getId());
      destResource.setOwner(operator.getName());
      destResource.setProjectId(project.getId());
      destResource.setProjectName(projectName);
      destResource.setCreateTime(now);
      destResource.setModifyTime(now);

      try {
        resourceMapper.insert(destResource);
      } catch (DuplicateKeyException e) {
        logger.error("Resource has exist, can't create again.", e);
        response.setStatus(HttpStatus.SC_CONFLICT);
        return null;
      }
    } else { // 存在
      if (desc != null) {
        destResource.setDesc(desc);
      }

      destResource.setOwnerId(operator.getId());
      destResource.setModifyTime(now);

      resourceMapper.update(destResource);
    }

    // 开始拷贝文件
    String srcHdfsFilename = BaseConfig.getHdfsResourcesFilename(project.getId(), srcResName);
    String destHdfsFilename = BaseConfig.getHdfsResourcesFilename(project.getId(), destResName);

    HdfsClient.getInstance().copy(srcHdfsFilename, destHdfsFilename, false, true);

    return destResource;
  }

  /**
   * 删除资源
   *
   * @param operator
   * @param projectName
   * @param name
   * @param response
   * @return
   */
  @Transactional(value = "TransactionManager")
  public void deleteResource(User operator,
                             String projectName,
                             String name,
                             HttpServletResponse response) {

    // 判断是否具备相应的权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    if (!projectService.hasWritePerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return;
    }

    // 查询先
    Resource resource = resourceMapper.queryResource(project.getId(), name);

    if (resource == null) {
      return;
    }

    // 删除数据库
    resourceMapper.delete(resource.getId());

    // 删除资源信息
    String hdfsFilename = BaseConfig.getHdfsResourcesFilename(project.getId(), name);

    HdfsClient.getInstance().delete(hdfsFilename, false);
  }

  /**
   * 得到资源列表
   *
   * @param operator
   * @param projectName
   * @param response
   * @return
   */
  public List<Resource> getResources(User operator, String projectName, HttpServletResponse response) {

    // 判断是否具备相应的权限, 必须具备读权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasReadPerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 查询资源
    List<Resource> resources = resourceMapper.queryResourceDetails(project.getId());

    return resources;
  }

  /**
   * 得到某个资源的向详情信息
   *
   * @param operator
   * @param projectName
   * @param name
   * @param response
   * @return
   */
  public Resource getResource(User operator, String projectName, String name, HttpServletResponse response) {

    // 判断是否具备相应的权限, 必须具备读权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasReadPerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 查询资源
    Resource resource = resourceMapper.queryResourceDetail(project.getId(), name);

    if (resource == null) {
      logger.error("Resource {} does not exist", name);
      response.setStatus(HttpStatus.SC_NOT_FOUND);
      return null;
    }

    return resource;
  }

  /**
   * 下载资源文件
   *
   * @param operator
   * @param projectName
   * @param name
   * @param response
   * @return
   */
  public org.springframework.core.io.Resource downloadResource(User operator,
                                                               String projectName,
                                                               String name,
                                                               HttpServletResponse response) {
    // 判断是否具备相应的权限, 必须具备读权限
    Project project = projectMapper.queryByName(projectName);

    if (project == null) {
      logger.error("Project does not exist: {}", projectName);
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      return null;
    }

    if (!projectService.hasReadPerm(operator.getId(), project)) {
      logger.error("User {} has no right permission for the project {}", operator.getName(), projectName);
      response.setStatus(HttpStatus.SC_UNAUTHORIZED);
      return null;
    }

    // 下载文件
    Resource resource = resourceMapper.queryResource(project.getId(), name);

    if (resource == null) {
      logger.error("Download file not exist, project {}, resource {}", projectName, name);
      return null;
    }

    String localFilename = BaseConfig.getLocalDownloadFilename(project.getId(), name);
    String hdfsFilename = BaseConfig.getHdfsResourcesFilename(project.getId(), name);

    HdfsClient.getInstance().copyHdfsToLocal(hdfsFilename, localFilename, false, true);

    org.springframework.core.io.Resource file = fileSystemStorageService.loadAsResource(localFilename);

    return file;
  }
}
