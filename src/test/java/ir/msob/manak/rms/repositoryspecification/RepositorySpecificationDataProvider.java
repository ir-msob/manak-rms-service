package ir.msob.manak.rms.repositoryspecification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import ir.msob.jima.core.commons.id.BaseIdService;
import ir.msob.manak.core.test.jima.crud.base.domain.DomainCrudDataProvider;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecification;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationCriteria;
import ir.msob.manak.domain.model.rms.repositoryspecification.RepositorySpecificationDto;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ir.msob.jima.core.test.CoreTestData.DEFAULT_STRING;
import static ir.msob.jima.core.test.CoreTestData.UPDATED_STRING;

/**
 * This class provides test data for the {@link RepositorySpecification} class. It extends the {@link DomainCrudDataProvider} class
 * and provides methods to create new test data objects, update existing data objects, and generate JSON patches for updates.
 */
@Component
public class RepositorySpecificationDataProvider extends DomainCrudDataProvider<RepositorySpecification, RepositorySpecificationDto, RepositorySpecificationCriteria, RepositorySpecificationRepository, RepositorySpecificationService> {

    private static RepositorySpecificationDto newDto;
    private static RepositorySpecificationDto newMandatoryDto;
    protected RepositorySpecificationDataProvider(BaseIdService idService, ObjectMapper objectMapper, RepositorySpecificationService service) {
        super(idService, objectMapper, service);
    }

    /**
     * Creates a new DTO object with default values.
     */
    public static void createNewDto() {
        newDto = prepareMandatoryDto();
        newDto.setDescription(DEFAULT_STRING);
    }

    /**
     * Creates a new DTO object with mandatory fields set.
     */
    public static void createMandatoryNewDto() {
        newMandatoryDto = prepareMandatoryDto();
    }

    /**
     * Creates a new DTO object with mandatory fields set.
     */
    public static RepositorySpecificationDto prepareMandatoryDto() {
        RepositorySpecificationDto dto = new RepositorySpecificationDto();
        dto.setName(DEFAULT_STRING);
        return dto;
    }

    /**
     * @throws JsonPointerException if there is an error creating the JSON patch.
     */
    @Override
    @SneakyThrows
    public JsonPatch getJsonPatch() {
        List<JsonPatchOperation> operations = getMandatoryJsonPatchOperation();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", RepositorySpecification.FN.description)), new TextNode(UPDATED_STRING)));
        return new JsonPatch(operations);
    }

    /**
     * @throws JsonPointerException if there is an error creating the JSON patch.
     */
    @Override
    @SneakyThrows
    public JsonPatch getMandatoryJsonPatch() {
        return new JsonPatch(getMandatoryJsonPatchOperation());
    }

    /**
     *
     */
    @Override
    public RepositorySpecificationDto getNewDto() {
        return newDto;
    }

    /**
     * Updates the given DTO object with the updated value for the domain field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateDto(RepositorySpecificationDto dto) {
        updateMandatoryDto(dto);
        dto.setDescription(UPDATED_STRING);
    }

    /**
     *
     */
    @Override
    public RepositorySpecificationDto getMandatoryNewDto() {
        return newMandatoryDto;
    }

    /**
     * Updates the given DTO object with the updated value for the mandatory field.
     *
     * @param dto the DTO object to update
     */
    @Override
    public void updateMandatoryDto(RepositorySpecificationDto dto) {
        dto.setName(UPDATED_STRING);
    }

    /**
     * Creates a list of JSON patch operations for updating the mandatory field.
     *
     * @return a list of JSON patch operations
     * @throws JsonPointerException if there is an error creating the JSON pointer.
     */
    public List<JsonPatchOperation> getMandatoryJsonPatchOperation() throws JsonPointerException {
        List<JsonPatchOperation> operations = new ArrayList<>();
        operations.add(new ReplaceOperation(new JsonPointer(String.format("/%s", RepositorySpecification.FN.name)), new TextNode(UPDATED_STRING)));
        return operations;
    }

    @Override
    public void assertMandatoryGet(RepositorySpecificationDto before, RepositorySpecificationDto after) {
        super.assertMandatoryGet(before, after);
        Assertions.assertThat(after.getName()).isEqualTo(before.getName());
    }

    @Override
    public void assertGet(RepositorySpecificationDto before, RepositorySpecificationDto after) {
        super.assertGet(before, after);
        assertMandatoryGet(before, after);

        Assertions.assertThat(after.getDescription()).isEqualTo(before.getDescription());
    }

    @Override
    public void assertMandatoryUpdate(RepositorySpecificationDto dto, RepositorySpecificationDto updatedDto) {
        super.assertMandatoryUpdate(dto, updatedDto);
        Assertions.assertThat(dto.getName()).isEqualTo(DEFAULT_STRING);
        Assertions.assertThat(updatedDto.getName()).isEqualTo(UPDATED_STRING);
    }

    @Override
    public void assertUpdate(RepositorySpecificationDto dto, RepositorySpecificationDto updatedDto) {
        super.assertUpdate(dto, updatedDto);
        assertMandatoryUpdate(dto, updatedDto);
    }

    @Override
    public void assertMandatorySave(RepositorySpecificationDto dto, RepositorySpecificationDto savedDto) {
        super.assertMandatorySave(dto, savedDto);
        assertMandatoryGet(dto, savedDto);
    }

    @Override
    public void assertSave(RepositorySpecificationDto dto, RepositorySpecificationDto savedDto) {
        super.assertSave(dto, savedDto);
        assertGet(dto, savedDto);
    }
}