package com.clutch.app.service;

import com.clutch.app.entity.BaseEntity;
import com.clutch.app.exceptions.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class BaseService<T extends BaseEntity, UUID> {

    protected abstract JpaRepository<T, UUID> getRepository();

    protected abstract String getEntityName();

    protected void validateEntity(UUID id) {
        getByIdOrThrow(id);
    }

    protected T getByIdOrThrow(UUID id) {
        T entity = getRepository().findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("%s not found. Id: %s", getEntityName(), id)));

        if (entity.isDeleted()) {
            throw new EntityNotFoundException(
                    String.format("%s was deleted. Id: %s", getEntityName(), id));
        }

        return entity;
    }

}
