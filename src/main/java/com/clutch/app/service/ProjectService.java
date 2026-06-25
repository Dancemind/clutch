package com.clutch.app.service;

import com.clutch.app.entity.Project;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.repository.ProjectRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int DEFAULT_PAGE_NUMBER = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ProjectRepository projectRepository;

    /**
     * Add project
     *
     * @param name        project name
     * @param description project description
     * @return project
     */
    public Project addProject(String name, String description) {
        return projectRepository.save(
                Project.builder()
                        .name(name)
                        .description(description)
                        .build()
        );
    }

    /**
     * Get project by uuid
     *
     * @param uuid project uuid
     * @return project
     */
    public Project getProjectByUuid(UUID uuid) {
        return projectRepository.getProjectByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                String.format("Project not found, uuid: %s", uuid)
                        )
                );
    }

    /**
     * Find company projects
     *
     * @return list of all projects of the company
     */
    public List<Project> findCompanyProjects() {
        return projectRepository.findAllByDeletedAtIsNull();
    }

    /**
     * Find all company projects paged
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return list of projects on page
     */
    public Page<Project> findCompanyProjectsPaged(Integer pageNumber, Integer pageSize) {
        int number = isNull(pageNumber) ? DEFAULT_PAGE_NUMBER : pageNumber;
        int size = isNull(pageSize) ? DEFAULT_PAGE_SIZE : pageSize;
        return projectRepository.findAllByDeletedAtIsNull(
                PageRequest.of(number, size)
        );
    }

    /**
     * Find all user projects. User has access to projects.
     *
     * @return list of user projects
     */
    public Project findUserProjects() {
        return null;
    }

    /**
     * Find all user projects. User has access to projects.
     *
     * @return page of user projects
     */
    public Page<Project> findUserProjectsPaged() {
        return null;
    }

    /**
     * Update project: name and description
     * Project name can't be empty
     *
     * @param uuid        project uuid
     * @param name        project name
     * @param description project description
     * @return project updated
     */
    public Project updateProject(UUID uuid, String name, String description) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Project name can't be empty");
        }
        Project project = getProjectByUuid(uuid);
        project.setName(name);
        project.setDescription(description);
        return projectRepository.save(project);
    }

    /**
     * Deactivate project
     *
     * @param uuid project uuid
     * @return project deactivated
     */
    public Project deactivateProject(UUID uuid) {
        Project project = getProjectByUuid(uuid);
        project.setIsActive(false);
        return projectRepository.save(project);
    }

    /**
     * Activate project
     *
     * @param uuid project uuid
     * @return project deactivated
     */
    public Project activateProject(UUID uuid) {
        Project project = getProjectByUuid(uuid);
        project.setIsActive(true);
        return projectRepository.save(project);
    }

    /**
     * Soft delete project - set deletion time
     *
     * @param uuid project uuid
     * @return project marked deleted
     */
    public Project deleteProject(UUID uuid) {
        Project project = getProjectByUuid(uuid);
        project.setDeletedAt(OffsetDateTime.now());
        return projectRepository.save(project);
    }

}
