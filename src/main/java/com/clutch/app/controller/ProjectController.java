package com.clutch.app.controller;

import com.clutch.app.dto.request.ProjectCreateDto;
import com.clutch.app.dto.response.ProjectDto;
import com.clutch.app.mappers.ClutchMapper;
import com.clutch.app.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ClutchMapper clutchMapper;
    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    public ProjectDto createProject(@RequestBody ProjectCreateDto projectCreateDto) {
        return clutchMapper.toProjectDto(
                projectService.addProject(projectCreateDto.name(), projectCreateDto.description())
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ProjectDto getProjectByUuid(@RequestParam("id") UUID uuid) {
        return clutchMapper.toProjectDto(
                projectService.getProjectByUuid(uuid)
        );
    }

    @GetMapping("all")
    public List<ProjectDto> findCompanyProjects() {
        return clutchMapper.toProjectDto(
                projectService.findCompanyProjects()
        );
    }

    @GetMapping("all/paged")
    public Page<ProjectDto> findCompanyProjectsPaged(@RequestParam("pageNumber") Integer pageNumber,
                                                     @RequestParam("pageSize") Integer pageSize) {
        return clutchMapper.toProjectDto(
                projectService.findCompanyProjectsPaged(pageNumber, pageSize)
        );
    }

    @PatchMapping
    public ProjectDto updateProject(@RequestBody ProjectDto projectDto) {
        return clutchMapper.toProjectDto(
                projectService.updateProject(
                        projectDto.uuid(),
                        projectDto.name(),
                        projectDto.description()
                )
        );
    }

    @PatchMapping("deactivate")
    public ProjectDto deactivateProject(@RequestParam("id") UUID uuid) {
        return clutchMapper.toProjectDto(
                projectService.deactivateProject(uuid)
        );
    }

    @PatchMapping("activate")
    public ProjectDto activateProject(@RequestParam("id") UUID uuid) {
        return clutchMapper.toProjectDto(
                projectService.activateProject(uuid)
        );
    }

    @DeleteMapping()
    public ProjectDto deleteProject(@RequestParam("id") UUID uuid) {
        return clutchMapper.toProjectDto(
                projectService.deleteProject(uuid)
        );
    }

}
