/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.meveo.api.rest.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.PersistenceDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.rest.RestUtils;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.elresolver.ELException;
import org.meveo.interfaces.Entity;
import org.meveo.interfaces.EntityOrRelation;
import org.meveo.interfaces.EntityRelation;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.storage.Repository;
import org.meveo.persistence.CrossStorageService;
import org.meveo.persistence.scheduler.*;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/{repository}/persistence")
public class PersistenceRs {

    protected static final Logger LOGGER = LoggerFactory.getLogger(PersistenceRs.class);

    @Inject
    private SchedulingService schedulingService;

    @Inject
    private ScheduledPersistenceService<CrossStorageService> scheduledPersistenceService;

    @Inject
    private CrossStorageService crossStorageService;

    @Inject
    private CustomFieldsCacheContainerProvider cache;
    
    @Inject 
    private RepositoryService repositoryService;

    @PathParam("repository")
    private String repositoryCode;

    @POST
    @Path("/{cetCode}/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> list(@PathParam("cetCode") String cetCode, PaginationConfiguration paginationConfiguration){
        final CustomEntityTemplate customEntityTemplate = cache.getCustomEntityTemplate(cetCode);
        if(customEntityTemplate == null){
            throw new NotFoundException("Custom entity template with code " + cetCode + " does not exists");
        }

        if(paginationConfiguration == null){
            paginationConfiguration = new PaginationConfiguration();
        }

        Repository repository = repositoryService.findByCode(repositoryCode);
        List<Map<String, Object>> data = crossStorageService.find(repository, customEntityTemplate, paginationConfiguration);

        for(Map<String, Object> values : data) {
        	replaceFilePathsByUrls(customEntityTemplate, values);
        }

		return data;
    }

    @DELETE
    @Path("/{cetCode}/{uuid}")
    public Response delete(@PathParam("cetCode") String cetCode, @PathParam("uuid") String uuid) throws BusinessException {
        final CustomEntityTemplate customEntityTemplate = cache.getCustomEntityTemplate(cetCode);
        if(customEntityTemplate == null){
            throw new NotFoundException("Custom entity template with code " + cetCode + " does not exists");
        }

        final Repository repository = repositoryService.findByCode(repositoryCode);
        if(repository == null){
            throw new NotFoundException("Repository with code " + repositoryCode + " does not exists");
        }

        crossStorageService.remove(repository, customEntityTemplate, uuid);

        return Response.noContent().build();
    }

    @GET
    @Path("/{cetCode}/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> get(@PathParam("cetCode") String cetCode, @PathParam("uuid") String uuid) {
        final CustomEntityTemplate customEntityTemplate = cache.getCustomEntityTemplate(cetCode);
        if(customEntityTemplate == null){
            throw new NotFoundException();
        }

        final Repository repository = repositoryService.findByCode(repositoryCode);
        Map<String, Object> values = crossStorageService.find(repository, customEntityTemplate, uuid);

        replaceFilePathsByUrls(customEntityTemplate, values);

		return values;
    }

    @PUT
    @Path("/{cetCode}/{uuid}")
    public void update(@PathParam("cetCode") String cetCode, @PathParam("uuid") String uuid, Map<String, Object> body) throws BusinessException, BusinessApiException, IOException {
        final CustomEntityTemplate customEntityTemplate = cache.getCustomEntityTemplate(cetCode);
        if(customEntityTemplate == null){
            throw new NotFoundException();
        }

        final Repository repository = repositoryService.findByCode(repositoryCode);
        crossStorageService.update(repository, customEntityTemplate, body, uuid);
    }
    
    @SuppressWarnings("unchecked")
	@POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistedItem> persist(MultipartFormDataInput input) throws IOException, CyclicDependencyException {
    	
    	java.nio.file.Path tempDir = Files.createTempDirectory("dataUpload");
    	
    	try {

            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            InputPart dtosPart = uploadForm.remove("data").get(0);
            GenericType<Collection<PersistenceDto>> dtosType = new GenericType<Collection<PersistenceDto>>() {
            };
            Collection<PersistenceDto> dtos = dtosPart.getBody(dtosType);

            for (Map.Entry<String, List<InputPart>> formPart : uploadForm.entrySet()) {
                String[] splittedKey = formPart.getKey().split("\\.");
                String entityName = splittedKey[0];
                String propertyName = splittedKey[1];

                if (formPart.getValue().size() == 1) {
                    InputPart inputPart = formPart.getValue().get(0);
                    InputStream inputStream = inputPart.getBody(InputStream.class, null);

                    File file = new File(tempDir.toString(), RestUtils.getFileName(inputPart));
                    FileUtils.copyInputStreamToFile(inputStream, file);

                    dtos.stream()
                            .filter(dto -> dto.getName().equals(entityName))
                            .findFirst()
                            .ifPresent(dto -> dto.getProperties().put(propertyName, file));

                } else {

                    for (InputPart inputPart : formPart.getValue()) {
                        InputStream inputStream = inputPart.getBody(InputStream.class, null);

                        File file = new File(tempDir.toString(), RestUtils.getFileName(inputPart));
                        FileUtils.copyInputStreamToFile(inputStream, file);

                        dtos.stream()
                                .filter(dto -> dto.getName().equals(entityName))
                                .findFirst()
                                .ifPresent(dto -> {
                                    List<File> property = (List<File>) dto.getProperties().computeIfAbsent(propertyName, s -> new ArrayList<File>());
                                    property.add(file);
                                });

                    }

                }
            }

            return persist(dtos);

    	} finally {
    		
    		Files.list(tempDir).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					LOGGER.warn("{} cannot be deleted", path.toString(), e);
				}
			});
    		
    		if(Files.list(tempDir).count() == 0) {
    			Files.delete(tempDir);
    		}
    	}
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public List<PersistedItem> persist(Collection<PersistenceDto> dtos) throws CyclicDependencyException {

        /* Extract the entities */
        final List<Entity> entities = dtos.stream()
                .filter(persistenceDto -> persistenceDto.getDiscriminator().equals(EntityOrRelation.ENTITY))
                .map(persistenceDto -> new Entity.Builder()
                        .type(persistenceDto.getType())
                        .name(persistenceDto.getName())
                        .properties(persistenceDto.getProperties())
                        .build())
                        .collect(Collectors.toList());

        /* Extract the relationships */
        final List<EntityRelation> relations = dtos.stream()
                .filter(persistenceDto -> persistenceDto.getDiscriminator().equals(EntityOrRelation.RELATION))
                .map(persistenceDto -> {
                    final Optional<Entity> source = entities.stream()
                            .filter(entity -> entity.getName().equals(persistenceDto.getSource()))
                            .findAny();
                    final Optional<Entity> target = entities.stream().filter(entity -> entity.getName().equals(persistenceDto.getTarget()))
                            .findAny();
                    if (source.isPresent() && target.isPresent()) {
                        return new EntityRelation.Builder()
                                .type(persistenceDto.getType())
                                .source(source.get())
                                .target(target.get())
                                .properties(persistenceDto.getProperties())
                                .build();
                    }
                    LOGGER.warn("Relationship of type {} between {} and {} will not be persisted because of missing source or target", persistenceDto.getType(), persistenceDto.getSource(), persistenceDto.getTarget());
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        /* Create the persistence schedule */
        List<EntityOrRelation> entityOrRelations = new ArrayList<>(entities);
        entityOrRelations.addAll(relations);
        AtomicPersistencePlan atomicPersistencePlan = schedulingService.schedule(entityOrRelations);

        try {

            /* Persist the entities and return 201 created response */
            return scheduledPersistenceService.persist(repositoryCode, atomicPersistencePlan);

        } catch (BusinessException | ELException | IOException | BusinessApiException e) {

            /* An error happened */
            throw new ServerErrorException(Response.serverError().entity(e).build());
        }

    }

	/**
	 * Replace the hard drive file paths by URL that permit to download them
	 *
	 * @param customEntityTemplate Template of the values
	 * @param values               Actual values containing the file paths
	 */
    private void replaceFilePathsByUrls(CustomEntityTemplate customEntityTemplate, Map<String, Object> values) {
    	cache.getCustomFieldTemplates(customEntityTemplate.getAppliesTo())
    		.values()
    		.stream()
			.filter(f -> f.getFieldType().equals(CustomFieldTypeEnum.BINARY))	// Only get binary fields
			.filter(f -> values.get(f.getCode()) != null)						// Filter on present values
			.forEach(binaryField -> {
				Object binaryFieldValue = values.get(binaryField.getCode());
				if(binaryFieldValue instanceof String) {
					String url = buildFileUrl(customEntityTemplate, values, binaryField);
					values.put(binaryField.getCode(), url);

				} else if(binaryFieldValue instanceof Collection) {
					List<String> urls = new ArrayList<>();
					for(int index = 0; index < ((Collection<?>) binaryFieldValue).size(); index++) {
						String url = buildFileUrl(customEntityTemplate, values, binaryField);
						url += "?index=" + index;
						urls.add(url);
					}
					values.put(binaryField.getCode(), urls);
				}
			});
    }

	/**
	 * Build an URL allowing to download a given file for a given entity in the FileSysytem
	 *
	 * @param customEntityTemplate Template of the entity
	 * @param values               Actual values of the entity
	 * @param binaryField          Field holding the file reference
	 */
	private String buildFileUrl(CustomEntityTemplate customEntityTemplate, Map<String, Object> values, CustomFieldTemplate binaryField) {
		String uuid = values.get("uuid") != null ? (String) values.get("uuid") : (String) values.get("meveo_uuid");

		return new StringBuilder("/api/rest/fileSystem/binaries/")
				.append(repositoryCode).append("/")
				.append(customEntityTemplate.getCode()).append("/")
				.append(uuid).append("/")
				.append(binaryField.getCode())
				.toString();
	}

}
