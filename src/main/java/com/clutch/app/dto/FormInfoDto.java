package com.clutch.app.dto;

import java.util.UUID;

public record FormInfoDto(

        UUID uuid,
        String formName,
        String formDescription,
        UUID companyUuid,
        UUID projectUuid

)
{
}
