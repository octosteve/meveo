/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.technicalservice.endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ModuleItem;
import org.meveo.model.ModuleItemOrder;
import org.meveo.model.ObservableEntity;
import org.meveo.model.annotation.ImportOrder;
import org.meveo.model.scripts.Function;
import org.meveo.validation.constraint.nointersection.NoIntersectionBetween;

/**
 * Configuration of an endpoint allowing to use a technical service.
 *
 * @author clement.bareth
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.10
 */
@Entity
@Table(name = "service_endpoint")
@GenericGenerator(name = "ID_GENERATOR", strategy = "increment")
@NoIntersectionBetween(firstCollection = "pathParameters.endpointParameter.parameter", secondCollection = "parametersMapping.endpointParameter.parameter")
@NamedQueries({ @NamedQuery(name = "findByParameterName", query = "SELECT e FROM Endpoint e "
		+ "INNER JOIN e.service as service " + "LEFT JOIN e.pathParameters as pathParameter "
		+ "LEFT JOIN e.parametersMapping as parameterMapping " + "WHERE service.code = :serviceCode "
		+ "AND (pathParameter.endpointParameter.parameter = :propertyName OR parameterMapping.endpointParameter.parameter = :propertyName)"),
		@NamedQuery(name = "Endpoint.deleteByService", query = "DELETE from Endpoint e WHERE e.service.id=:serviceId") })
@ImportOrder(5)
@ExportIdentifier({ "code" })
@ModuleItem(value = "Endpoint", path = "endpoints")
@ModuleItemOrder(80)
@ObservableEntity
public class Endpoint extends BusinessEntity {

	private static final long serialVersionUID = 6561905332917884613L;
	
	public static final String ENDPOINT_INTERFACE_JS = "EndpointInterface";

	public static final Pattern basePathPattern = Pattern.compile("[a-zA-Z0-9_\\-.]+");

	public static final Pattern pathPattern = Pattern.compile("[a-zA-Z0-9_\\-./\\{\\}]+");

	public static final Pattern pathParamPattern = Pattern.compile("\\{[a-zA-Z0-9_\\-./]+\\}");
	
	/** Whether endpoint is accessible without logging */
	@Column(name = "secured", nullable = false)
	@Type(type = "numeric_boolean")
	private boolean isSecured = true;
	
	/** Whether to check exact match of path parameters number */
	@Column(name = "check_path_params", nullable = false)
	@Type(type = "numeric_boolean")	
	private boolean checkPathParams = true;

	/**
	 * Technical service associated to the endpoint
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "service_id", updatable = false, nullable = false)
	private Function service;

	/**
	 * Whether the execution of the service will be syncrhonous. If asynchronous,
	 * and id of execution will be returned to the user.
	 */
	@Type(type = "numeric_boolean")
	@Column(name = "synchronous", nullable = false)
	private boolean synchronous;

	/**
	 * Method used to access the endpoint. Conditionates the input format of the
	 * endpoint.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false)
	private EndpointHttpMethod method;

	/**
	 * Parameters that will be exposed in the endpoint path
	 */
	@OneToMany(mappedBy = "endpointParameter.endpoint", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderColumn(name = "position")
	private List<EndpointPathParameter> pathParameters;

	/**
	 * Mapping of the parameters that are not defined as path parameters
	 */
	@OneToMany(mappedBy = "endpointParameter.endpoint", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<TSParameterMapping> parametersMapping;

	/**
	 * JSONata query used to transform the result
	 */
	@Column(name = "jsonata_transformer")
	private String jsonataTransformer;

	/**
	 * Context variable to be returned by the endpoint
	 */
	@Column(name = "returned_variable_name")
	private String returnedVariableName;

	/**
	 * Context variable to be returned by the endpoint
	 */
	@Type(type = "numeric_boolean")
	@Column(name = "serialize_result", nullable = false)
	private boolean serializeResult;

	/**
	 * Content type of the response
	 */
	@Column(name = "content_type")
	private String contentType;


	@Column(name = "base_path")
	private String basePath;

	/**
	 * The path in swagger form like /organizations/{orgId}/members/{memberId}
	 * to be added to the base path to form the relative URL of the endpoint
	 */
	@Column(name = "path")
	private String path;

	@Transient
	Pattern pathRegex;

	public void setCode(String code){
		Matcher matcher = basePathPattern.matcher(code);
		if(matcher.matches()) {
			this.code = code;
		} else {
			throw new RuntimeException("invalid code");
		}
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setSerializeResult(boolean serializeResult) {
		this.serializeResult = serializeResult;
	}

	public boolean isSerializeResult() {
		return serializeResult;
	}

	public String getReturnedVariableName() {
		return returnedVariableName;
	}

	public void setReturnedVariableName(String returnedVariableName) {
		this.returnedVariableName = returnedVariableName;
	}

	public String getJsonataTransformer() {
		return jsonataTransformer;
	}

	public void setJsonataTransformer(String jsonataTransformer) {
		this.jsonataTransformer = jsonataTransformer;
	}

	public Function getService() {
		return service;
	}

	public void setService(Function service) {
		this.service = service;
	}

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public EndpointHttpMethod getMethod() {
		return method;
	}

	public void setMethod(EndpointHttpMethod method) {
		this.method = method;
	}

	public List<EndpointPathParameter> getPathParametersNullSafe() {
		if (pathParameters == null) {
			pathParameters = new ArrayList<>();
		}
		return getPathParameters();
	}

	public List<EndpointPathParameter> getPathParameters() {
		return pathParameters;
	}

	public void setPathParameters(List<EndpointPathParameter> pathParameters) {
		this.pathParameters = pathParameters;
	}

	public List<TSParameterMapping> getParametersMappingNullSafe() {
		if (parametersMapping == null) {
			parametersMapping = new ArrayList<TSParameterMapping>();
		}
		return getParametersMapping();
	}

	public List<TSParameterMapping> getParametersMapping() {
		return parametersMapping;
	}

	public void setParametersMapping(List<TSParameterMapping> parametersMapping) {
		this.parametersMapping = parametersMapping;
	}

	public boolean isSecured() {
		return isSecured;
	}

	public void setSecured(boolean isSecured) {
		this.isSecured = isSecured;
	}

	public String getBasePath() {
		if(basePath==null){
			basePath=code;
			pathRegex=null;
		}
		return basePath;
	}

	public void setBasePath(String basePath) {
		if(basePath == null){
			this.basePath=code;
			pathRegex=null;
		} else {
			/* check that the basepath is valid */
			Matcher matcher = basePathPattern.matcher(basePath);
			if(matcher.matches()) {
				this.basePath = basePath;
				pathRegex=null;
			} else {
				throw new RuntimeException("invalid basePath");
			}
		}
	}

	public String getPath() {
		if(path==null){
			String sep="";
			final StringBuilder endpointPath = new StringBuilder("/");
			if(pathParameters!=null) {
				for(EndpointPathParameter endpointPathParameter:pathParameters){
					endpointPath.append(sep).append("{").append(endpointPathParameter).append("}");
					sep="/";
				}
			}
			path = endpointPath.toString();
			pathRegex=null;
		}
		return path;
	}

	public void setPath(String path) {
		/* check that the path is valid */
		if(path !=null) {
			Matcher matcher=pathParamPattern.matcher(path);
			int i=0;
			while (matcher.find()) {
				String param = matcher.group();
				String paramName=param.substring(1,param.length()-1);
				if(pathParameters.size()>i){
					String actualParam=pathParameters.get(i).toString();
					if(!paramName.equals(actualParam)){
						throw new RuntimeException((i+1)+"th path param is expected to be " + pathParameters.get(i)+" while actual value is "+paramName);
					}
					i++;
				} else {
					throw new RuntimeException("unexpected param "+paramName);
				}
			}
			if(pathParameters.size()>i){
				throw new RuntimeException("missing param "+pathParameters.get(i));
			}
		}
		this.path = path;
		pathRegex=null;
		getPath();
	}

	@Transient
	/*
	* returns the endpoint url relative to the meveo base url
	 */
	public String getEndpointUrl() {
		return getBasePath()+getPath();
	}

	@Transient
	public Pattern getPathRegex(){
		if(pathRegex==null){
			String pattern = "/"+getBasePath()+getPath().replaceAll("\\{","(?<").replaceAll("\\}", ">[^/]+)");
			if(pattern.endsWith("/")){
				pattern+="?";
			}
			pathRegex=Pattern.compile(pattern);
		}
		return pathRegex;
	}

	public void addPathParameter(EndpointPathParameter endpointPathParameter) {
		if (pathParameters == null) {
			pathParameters = new ArrayList<EndpointPathParameter>();
		}

		pathParameters.add(endpointPathParameter);
	}

	public void addParametersMapping(TSParameterMapping e) {
		if (parametersMapping == null) {
			parametersMapping = new ArrayList<TSParameterMapping>();
		}

		parametersMapping.add(e);
	}

	public boolean isCheckPathParams() {
		return checkPathParams;
	}

	public void setCheckPathParams(boolean checkPathParams) {
		this.checkPathParams = checkPathParams;
	}
	
}
