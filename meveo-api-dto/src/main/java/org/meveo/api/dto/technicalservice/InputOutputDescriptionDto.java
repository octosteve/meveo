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
package org.meveo.api.dto.technicalservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.meveo.api.dto.EntityDescriptionDto;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "descriptionType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EntityDescriptionDto.class, name = "EntityDescription"),
        @JsonSubTypes.Type(value = RelationDescriptionDto.class, name = "RelationDescription")
})
public abstract class InputOutputDescriptionDto {

    @JsonProperty
    private List<InputPropertyDto> inputProperties = new ArrayList<>();

    @JsonProperty
    private List<OutputPropertyDto> outputProperties = new ArrayList<>();

    @JsonProperty(required = true)
    @NotNull
    private String type;

    @JsonProperty
    private boolean input;

    @JsonProperty
    private boolean output;

    /**
     * List of properties that are defined as inputs. Non empty list implies input = true.
     *
     * @return The list of the properties that are defined as inputs.
     */
    public List<InputPropertyDto> getInputProperties() {
        return inputProperties;
    }

    /**
     * List of properties that are defined as inputs. Non empty list implies input = true.
     *
     * @param inputProperties The List of properties that are defined as inputs.
     */
    public void setInputProperties(List<InputPropertyDto> inputProperties) {
        this.inputProperties = inputProperties;
    }

    /**
     * List of properties that are defined as outputs. Non empty list implies output = true.
     *
     * @return The list of the properties that are defined as inputs.
     */
    public List<OutputPropertyDto> getOutputProperties() {
        return outputProperties;
    }

    /**
     * List of properties that are defined as outputs. Non empty list implies output = true.
     *
     * @param outputProperties List of properties that are defined as outputs.
     */
    public void setOutputProperties(List<OutputPropertyDto> outputProperties) {
        this.outputProperties = outputProperties;
    }

    /**
     * Custom entity template code that the object describe.
     *
     * @return The code of the CET described
     */
    public String getType() {
        return type;
    }

    /**
     * Custom entity template code that the object describe.
     *
     * @param type The code of the CET described
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Whether the variable is defined as output of the technical service.
     *
     * @return "false" if the variable is not an output.
     */
    public boolean isOutput() {
        return output;
    }

    /**
     * Whether the variable is defined as output of the technical service.
     *
     * @param output "false" if the variable is not an output.
     */
    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * Whether the variable is defined as input of the technical service.
     *
     * @return "false" if the variable is not an input.
     */
    public boolean isInput() {
        return input;
    }

    /**
     * Whether the variable is defined as input of the technical service.
     *
     * @param input "false" if the variable is not an input.
     */
    public void setInput(boolean input) {
        this.input = input;
    }

    /**
     * Name of the variable described
     *
     * @return The instance name of the variable described
     */
    public abstract String getName();

    /**
     * Name of the variable described
     *
     * @param name The instance name of the variable described
     */
    public abstract void setName(String name);

}
