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

package org.meveo.api.technicalservice.endpoint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.rest.technicalservice.EndpointExecution;
import org.meveo.api.rest.technicalservice.EndpointExecutionBuilder;
import org.meveo.model.scripts.Accessor;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.model.technicalservice.endpoint.Endpoint;
import org.meveo.model.technicalservice.endpoint.EndpointParameter;
import org.meveo.model.technicalservice.endpoint.EndpointPathParameter;
import org.meveo.model.technicalservice.endpoint.TSParameterMapping;
import org.meveo.service.script.ConcreteFunctionService;
import org.meveo.service.script.CustomScriptService;
import org.meveo.service.script.ScriptInstanceService;
import org.mockito.Answers;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EndpointsTest {

    private ConcreteFunctionService concreteFunctionService;
    private ScriptInstanceService customScriptService;
    private EndpointApi endpointApi;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws BusinessException {
        concreteFunctionService = mock(ConcreteFunctionService.class);
        customScriptService = mock(ScriptInstanceService.class, Answers.CALLS_REAL_METHODS.get());

        when(concreteFunctionService.getFunctionService(anyString())).thenReturn((CustomScriptService) customScriptService);

        ScriptInstance scriptInstance = getScriptInstance();

        doReturn(scriptInstance).when(customScriptService).findByCode(anyString());
        doReturn(new ExposedScriptTest()).when(customScriptService).getScriptInstance(anyString());

        when(customScriptService.getExecutionEngine(anyString(), anyMap())).thenCallRealMethod();
        when(customScriptService.execute(anyObject(), anyMap())).thenCallRealMethod();

        endpointApi = new EndpointApi(null, concreteFunctionService);
    }

    /**
     * The goal is to test the path parameters mapping
     */
    @Test
    public void testPathParameter() throws BusinessException {
        Endpoint endpoint = getEndpoint();

        EndpointExecution execution = getEndpointExecution(endpoint);

        final Map<String, Object> execute = endpointApi.execute(endpoint, execution);

        Assert.assertEquals("The country is : France", execute.get("resultCountry"));
    }


    /**
     * The goal is to test the budget information mapping
     */
    @Test
    public void testBudget() throws BusinessException {
        Endpoint endpoint = getEndpoint();

        EndpointExecution execution = getEndpointExecution(endpoint);

        final Map<String, Object> execute = endpointApi.execute(endpoint, execution);

        Assert.assertEquals("The budget is 32.0 EUR", execute.get("budget"));
    }

    /**
     * The goal is to test the body / query parameters mapping
     */
    @Test
    public void testParameterMapping() throws BusinessException {
        Endpoint endpoint = getEndpoint();

        EndpointExecution execution = getEndpointExecution(endpoint);

        final Map<String, Object> execute = endpointApi.execute(endpoint, execution);

        Assert.assertEquals("The city is : New-York", execute.get("resultCity"));
    }

    private EndpointExecution getEndpointExecution(Endpoint endpoint) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("CityValue", "New-York");

        return new EndpointExecutionBuilder()
                .setParameters(parameters)
                .setBugetMax(32.0)
                .setBudgetUnit("EUR")
                .setPathInfo(new String[]{"rest", endpoint.getCode(), "France"})
                .createEndpointExecution();
    }

    private Endpoint getEndpoint() {
        ScriptInstance function = getScriptInstance();

        // Path parameters
        EndpointPathParameter countryPathParameter = mock(EndpointPathParameter.class);
        EndpointParameter countryParameter = new EndpointParameter();
        countryParameter.setParameter("country");
        countryPathParameter.setEndpointParameter(countryParameter);
        when(countryPathParameter.getPosition()).thenReturn(0);
        when(countryPathParameter.toString()).thenReturn("country");

        // Parameters mapping
        TSParameterMapping cityParameterMapping = new TSParameterMapping();
        EndpointParameter cityParameter = new EndpointParameter();
        cityParameter.setParameter("city");
        cityParameterMapping.setEndpointParameter(cityParameter);
        cityParameterMapping.setMultivalued(false);
        cityParameterMapping.setParameterName("CityValue");
        cityParameterMapping.setDefaultValue("Paris");

        // Endpoint
        Endpoint endpoint = new Endpoint();
        endpoint.setCode("test-endpoint");
        endpoint.setService(function);
        endpoint.setPathParameters(Collections.singletonList(countryPathParameter));
        endpoint.setParametersMapping(Collections.singletonList(cityParameterMapping));
        return endpoint;
    }

    private static ScriptInstance getScriptInstance() {
        // Inputs of the function
        Accessor city = new Accessor();
        city.setName("city");
        city.setMethodName("setCity");

        Accessor country = new Accessor();
        country.setName("country");
        country.setMethodName("setCountry");

        List<Accessor> inputs = new ArrayList<>();
        inputs.add(city);
        inputs.add(country);

        // Function
        ScriptInstance function = new ScriptInstance();
        function.setCode("test");
        function.setSetters(inputs);

        return function;
    }
}
