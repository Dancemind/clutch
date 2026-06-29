package com.clutch.app.mappers;

import com.clutch.app.dto.FormFieldDto;
import com.clutch.app.dto.FormInfoDto;
import com.clutch.app.dto.FormMetadataDto;
import com.clutch.app.dto.response.AuditLogDto;
import com.clutch.app.dto.response.ProjectDto;
import com.clutch.app.entity.Form;
import com.clutch.app.entity.FormColumn;
import com.clutch.app.entity.Project;
import com.clutch.app.entity.audit.AuditLog;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClutchMapper {

    ProjectDto toProjectDto(Project project);

    List<ProjectDto> toProjectDto(List<Project> projects);

    FormInfoDto toFormInfoDto(Form form);

    List<FormInfoDto> toFormInfoDto(List<Form> form);

    AuditLogDto toAuditLogDto(AuditLog auditLog);

    default Page<ProjectDto> toProjectDto(Page<Project> projects) {
        if (projects == null) {
            return null;
        }
        return projects.map(this::toProjectDto);
    }

    default Page<AuditLogDto> toAuditLogDto(Page<AuditLog> auditLog) {
        if (auditLog == null) {
            return null;
        }
        return auditLog.map(this::toAuditLogDto);
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
