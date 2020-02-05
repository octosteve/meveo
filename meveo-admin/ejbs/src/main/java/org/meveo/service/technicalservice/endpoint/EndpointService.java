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
package org.meveo.service.technicalservice.endpoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.event.qualifier.Created;
import org.meveo.event.qualifier.Removed;
import org.meveo.event.qualifier.Updated;
import org.meveo.keycloak.client.KeycloakAdminClientConfig;
import org.meveo.keycloak.client.KeycloakAdminClientService;
import org.meveo.keycloak.client.KeycloakUtils;
import org.meveo.model.git.GitRepository;
import org.meveo.model.persistence.JacksonUtil;
import org.meveo.model.scripts.Function;
import org.meveo.model.scripts.Sample;
import org.meveo.model.technicalservice.endpoint.Endpoint;
import org.meveo.model.technicalservice.endpoint.EndpointHttpMethod;
import org.meveo.model.technicalservice.endpoint.EndpointPathParameter;
import org.meveo.model.technicalservice.endpoint.TSParameterMapping;
import org.meveo.service.base.BusinessService;
import org.meveo.service.git.GitClient;
import org.meveo.service.git.GitHelper;
import org.meveo.service.git.MeveoRepository;
import org.meveo.util.Version;

import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;

/**
 * EJB for managing technical services endpoints
 *
 * @author clement.bareth
 * @since 01.02.2019
 */
@Stateless
public class EndpointService extends BusinessService<Endpoint> {

    public static final String EXECUTE_ALL_ENDPOINTS = "Execute_All_Endpoints";
    public static final String ENDPOINTS_CLIENT = "endpoints";
    public static final String EXECUTE_ENDPOINT_TEMPLATE = "Execute_Endpoint_%s";
    public static final String ENDPOINT_MANAGEMENT = "endpointManagement";

    @Context
    private HttpServletRequest request;

    @Inject
    @MeveoRepository
    private GitRepository meveoRepository;

    @Inject
    private GitClient gitClient;

    public static String getEndpointPermission(Endpoint endpoint) {
        return String.format(EXECUTE_ENDPOINT_TEMPLATE, endpoint.getCode());
    }

    @EJB
    private KeycloakAdminClientService keycloakAdminClientService;

    /**
     * Retrieve all endpoints associated to the given service
     *
     * @param code Code of the service
     * @return All endpoints associated to the service with the provided code
     */
    public List<Endpoint> findByServiceCode(String code){
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Endpoint> query = cb.createQuery(Endpoint.class);
        Root<Endpoint> root = query.from(Endpoint.class);
        final Join<Endpoint, Function> service = root.join("service");
        query.where(cb.equal(service.get("code"), code));
        return getEntityManager().createQuery(query).getResultList();
    }

    /**
     * @param code  Code of the service associated to the endpoint
     * @param parameterName Filter on endpoint's parameters names
     * @return the filtered list of endpoints
     */
    public List<Endpoint> findByParameterName(String code, String parameterName){
        return getEntityManager()
                .createNamedQuery("findByParameterName", Endpoint.class)
                .setParameter("serviceCode", code)
                .setParameter("propertyName", parameterName)
                .getResultList();
    }

    /**
     * Create a new endpoint in database. Also create associated client and roles in keycloak.
     *
     * @param entity Endpoint to create
     */
    @Override
    public void create(Endpoint entity) throws BusinessException {

        // Create client if not exitsts
        keycloakAdminClientService.createClient(ENDPOINTS_CLIENT);

        String endpointPermission = getEndpointPermission(entity);

        // Create endpoint permission and add it to Execute_All_Endpoints composite
        keycloakAdminClientService.addToComposite(ENDPOINTS_CLIENT, endpointPermission, EXECUTE_ALL_ENDPOINTS);

        // Create endpointManagement role in default client if not exists
        KeycloakAdminClientConfig keycloakConfig = KeycloakUtils.loadConfig();
        keycloakAdminClientService.createRole(keycloakConfig.getClientId(), ENDPOINT_MANAGEMENT);

        // Add endpoint role and selected composite roles
        if (CollectionUtils.isNotEmpty(entity.getRoles())) {
            for (String compositeRole : entity.getRoles()) {
                keycloakAdminClientService.addToCompositeCrossClient(ENDPOINTS_CLIENT, keycloakConfig.getClientId(), endpointPermission, compositeRole);
            }
        }

        // Add Execute_All_Endpoints to endpointManagement composite if not already in
        keycloakAdminClientService.addToCompositeCrossClient(ENDPOINTS_CLIENT, keycloakConfig.getClientId(), EXECUTE_ALL_ENDPOINTS, ENDPOINT_MANAGEMENT);
        
        super.create(entity);
    }

    /**
     * Remove an endpoint from database. Also remove associated role in keycloak.
     *
     * @param entity Endpoint to remove
     */
    @Override
    public void remove(Endpoint entity) throws BusinessException {
        super.remove(entity);
        String role = getEndpointPermission(entity);
        keycloakAdminClientService.removeRole(ENDPOINTS_CLIENT, role);
        if (CollectionUtils.isNotEmpty(entity.getRoles())) {
            for (String compositeRole : entity.getRoles()) {
                keycloakAdminClientService.removeRoleInCompositeRole(role, compositeRole);
            }
        }
        keycloakAdminClientService.removeRole(role);
    }

    /**
     * Update an endpoint.
     *
     * @param entity Endpoint to update
     * @return the updated endpoint
     */
    @Override
    public Endpoint update(Endpoint entity) throws BusinessException {
        Endpoint endpoint = findById(entity.getId());
        String oldEndpointPermission = getEndpointPermission(endpoint);
        endpoint.getPathParameters().clear();
        endpoint.getParametersMapping().clear();

        flush();

        endpoint.getPathParameters().addAll(entity.getPathParameters());
        endpoint.getParametersMapping().addAll(entity.getParametersMapping());
        endpoint.setJsonataTransformer(entity.getJsonataTransformer());
        endpoint.setMethod(entity.getMethod());
        endpoint.setService(entity.getService());
        endpoint.setSynchronous(entity.isSynchronous());
        endpoint.setCode(entity.getCode());
        endpoint.setDescription(entity.getDescription());
        endpoint.setReturnedVariableName(entity.getReturnedVariableName());
        endpoint.setSerializeResult(entity.isSerializeResult());
        endpoint.setContentType(entity.getContentType());
        endpoint.setRoles(entity.getRoles());

        super.update(endpoint);

        keycloakAdminClientService.removeRole(ENDPOINTS_CLIENT, oldEndpointPermission);

        // Create client if not exitsts
        keycloakAdminClientService.createClient(ENDPOINTS_CLIENT);

        String endpointPermission = getEndpointPermission(entity);

        // Create endpoint permission and add it to Execute_All_Endpoints composite
        keycloakAdminClientService.addToComposite(ENDPOINTS_CLIENT, endpointPermission, EXECUTE_ALL_ENDPOINTS);

        KeycloakAdminClientConfig keycloakConfig = KeycloakUtils.loadConfig();
        List<String> roles = keycloakAdminClientService.getCompositeRolesByRealmClientId(keycloakConfig.getClientId(), keycloakConfig.getRealm());
        for (String compositeRole: roles) {
            if (!compositeRole.equals(EXECUTE_ALL_ENDPOINTS)) {
                keycloakAdminClientService.removeRoleInCompositeRole(oldEndpointPermission, compositeRole);
            }
        }

        for (String compositeRole: entity.getRoles()) {
            keycloakAdminClientService.addToCompositeCrossClient(ENDPOINTS_CLIENT, keycloakConfig.getClientId(), endpointPermission, compositeRole);
        }
        
        return entity;
    }

    /**
     * Create and commit the generated JS file to call the endpoint.
     * Automatically called at endpoint's creation.
     *
     * @param endpoint Created endpoint
     * @return the generated file
     * @throws IOException if file cannot be created
     * @throws BusinessException if the changes can't be commited
     */
    public File createESFile(@Observes @Created Endpoint endpoint) throws IOException, BusinessException {
        final File scriptFile = getScriptFile(endpoint);
        FileUtils.write(scriptFile, ESGenerator.generate(endpoint));
        gitClient.commitFiles(meveoRepository, Collections.singletonList(scriptFile), "Create JS script for endpoint " + endpoint.getCode());
        return scriptFile;
    }

    /**
     * Update (or create if not exists yet) the generated JS file to call the endpoint and commit the changes if there are any.
     * Automatically called at endpoint's update.
     *
     * @param endpoint Updated endpoint
     * @return the updated generated file
     * @throws IOException if file cannot be created or overwritten
     * @throws BusinessException if the changes can't be commited
     */
    public File updateESFile(@Observes @Updated Endpoint endpoint) throws IOException, BusinessException {
        final File scriptFile = getScriptFile(endpoint);
        String updatedScript = ESGenerator.generate(endpoint);

        if(!scriptFile.exists() || !FileUtils.readFileToString(scriptFile).equals(updatedScript)) {
            FileUtils.write(scriptFile, updatedScript);
            gitClient.commitFiles(meveoRepository, Collections.singletonList(scriptFile), "Update JS script for endpoint " + endpoint.getCode());
        }

        return scriptFile;
    }

    /**
     * Remove the generated JS file from Meveo git repository.
     * Called at endpoint's deletion.
     *
     * @param endpoint Removed endpoint
     * @return the result of {@link File#delete()} called on the script file
     * @throws BusinessException if the changes can't be commited
     */
    public boolean removeESFile(@Observes @Removed Endpoint endpoint) throws BusinessException {
        File scriptFile = getScriptFile(endpoint);
        gitClient.commitFiles(meveoRepository, Collections.singletonList(scriptFile), "Update JS script for endpoint " + endpoint.getCode());
        return scriptFile.delete();
    }

    private File getScriptFile(Endpoint endpoint) {
        final File repositoryDir = GitHelper.getRepositoryDir(currentUser, meveoRepository.getCode());
        final File endpointDir = new File(repositoryDir, "/endpoints/" + endpoint.getCode());
        endpointDir.mkdirs();
        return new File(endpointDir, endpoint.getCode() + ".js");
    }
    
    public Swagger generateOpenApiJson(String baseUrl, Endpoint endpoint) {

		Info info = new Info();
		info.setTitle(endpoint.getCode());
		info.setDescription(endpoint.getDescription());
		info.setVersion(Version.appVersion);

		Map<String, Path> paths = new HashMap<>();
		Path path = new Path();

		Operation operation = new Operation();

		if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
			path.setGet(operation);

		} else if (endpoint.getMethod().equals(EndpointHttpMethod.POST)) {
			path.setPost(operation);
		}

		if (!Objects.isNull(endpoint.getPathParameters())) {
			for (EndpointPathParameter endpointPathParameter : endpoint.getPathParameters()) {
				Parameter parameter = new PathParameter();
				parameter.setName(endpointPathParameter.getEndpointParameter().getParameter());
				path.addParameter(parameter);
			}
		}

		paths.put(endpoint.getEndpointUrl(), path);

		List<Sample> samples = endpoint.getService().getSamples();

		if (!Objects.isNull(endpoint.getParametersMapping())) {
			List<Parameter> operationParameter = new ArrayList<>();

			for (TSParameterMapping tsParameterMapping : endpoint.getParametersMapping()) {

				if (endpoint.getMethod().equals(EndpointHttpMethod.GET)) {
					QueryParameter queryParameter = new QueryParameter();
					queryParameter.setName(tsParameterMapping.getParameterName());
					operationParameter.add(queryParameter);

					if(samples != null && !samples.isEmpty()) {
						Object inputExample = samples.get(0).getInputs().get(tsParameterMapping.getParameterName());
						queryParameter.setExample(String.valueOf(inputExample));
					}

				} else if (endpoint.getMethod().equals(EndpointHttpMethod.POST)) {
					BodyParameter bodyParameter = new BodyParameter();
					bodyParameter.setName(tsParameterMapping.getParameterName());
					operationParameter.add(bodyParameter);

					if(samples != null && !samples.isEmpty()) {
						Object inputExample = samples.get(0).getInputs().get(tsParameterMapping.getParameterName());
						String mediaType = endpoint.getContentType() != null ? endpoint.getContentType() : "application/json";
						String inputExampleSerialized = inputExample.getClass().isPrimitive() ? String.valueOf(inputExample) : JacksonUtil.toString(inputExample);
						bodyParameter.addExample(mediaType, inputExampleSerialized);
					}

				}
			}

			operation.setParameters(operationParameter);
		}

		Map<String, io.swagger.models.Response> responses = new HashMap<>();
		io.swagger.models.Response response = new io.swagger.models.Response();

		if(samples != null && !samples.isEmpty()) {
			Object outputExample = samples.get(0).getOutputs();
			String mediaType = endpoint.getContentType() != null ? endpoint.getContentType() : "application/json";
			response.example(mediaType, outputExample);
		}

		responses.put("200", response);

		Swagger swagger = new Swagger();
		swagger.setInfo(info);
		swagger.setBasePath(baseUrl);
		swagger.setSchemes(Arrays.asList(Scheme.HTTP, Scheme.HTTPS));
		swagger.setProduces(Collections.singletonList(endpoint.getContentType()));
		if(endpoint.getMethod() == EndpointHttpMethod.POST) {
			swagger.setConsumes(Arrays.asList("application/json", "application/xml"));
		}
		swagger.setPaths(paths);
		swagger.setResponses(responses);

		return swagger;
	}
}
