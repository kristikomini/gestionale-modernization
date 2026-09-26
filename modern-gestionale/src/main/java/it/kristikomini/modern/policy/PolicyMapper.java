package it.kristikomini.modern.policy;

import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: entity → response record. MapStruct generates the implementation at
 * <b>compile time</b> (see {@code target/generated-sources}) — no runtime reflection, and a
 * compile error if a field cannot be mapped, which is the safety a hand-written mapper lacks.
 */
@Mapper(componentModel = "spring")
public interface PolicyMapper {

    PolicyResponse toResponse(PolicyEntity entity);

    CoverageResponse toResponse(CoverageEntity entity);

    List<PolicyResponse> toResponseList(List<PolicyEntity> entities);
}
