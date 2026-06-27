package com.clutch.app.mappers;

import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.ProjectDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.entity.Project;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClutchMapper {

    ProjectDto toProjectDto(Project project);

    List<ProjectDto> toProjectDto(List<Project> projects);

    default Page<ProjectDto> toProjectDto(Page<Project> projects) {
        if (projects == null) {
            return null;
        }
        return projects.map(this::toProjectDto);
    }

    default FormMetadataDto toFormMetadataDto(Form form, List<FormColumn> formColumns) {
        return new FormMetadataDto(
                form.getUuid(),
                form.getName(),
                form.getDescription(),
                formColumns.stream()
                        .map(column ->
                                new FormFieldDto(
                                        column.getUuid(),
                                        column.getUserKey(),
                                        column.getFieldType(),
                                        column.getOrderNumber()
                                )
                        ).toList()
        );
    }

}
